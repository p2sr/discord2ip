package sr.portal2.discord2ip.bot;

import it.unimi.dsi.fastutil.ints.*;
import me.walkerknapp.rapidopus.OpusDecoder;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import sr.portal2.discord2ip.buffer.AudioBuffer;
import sr.portal2.discord2ip.buffer.AudioFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VoiceAudioReceiveHandler implements AudioReceiveHandler {
    private final AudioBuffer audioBuffer;

    private final Int2ObjectMap<OpusDecoder> opusDecoders;
    private final Int2IntMap userTimestampOffsets;

    private static final int SAMPLES_PER_MS = OpusPacket.OPUS_FRAME_SIZE / OpusPacket.OPUS_FRAME_TIME_AMOUNT;

    private final ByteBuffer temporaryReadBuffer;

    public VoiceAudioReceiveHandler(AudioBuffer audioBuffer) {
        this.audioBuffer = audioBuffer;

        this.opusDecoders = new Int2ObjectOpenHashMap<>(100);
        this.userTimestampOffsets = new Int2IntOpenHashMap(100);

        this.temporaryReadBuffer = ByteBuffer.allocateDirect(OpusPacket.OPUS_FRAME_SIZE * OpusPacket.OPUS_CHANNEL_COUNT * Float.BYTES);
        this.temporaryReadBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void handleEncodedAudio(@NotNull OpusPacket packet) {
        // Directly deal with opus packets because we use a faster decoder than the default JDA implementation.

        byte[] opusData = packet.getOpusAudio();

        // Decode a frame of the user's audio into a temporary buffer
        this.opusDecoders
                .computeIfAbsent(packet.getSSRC(), ssrc -> new OpusDecoder(OpusPacket.OPUS_SAMPLE_RATE, OpusPacket.OPUS_CHANNEL_COUNT))
                .decodeFloat(opusData, this.temporaryReadBuffer, OpusPacket.OPUS_FRAME_SIZE, 0);

        // Synchronize any calls that could be influenced by the output thread popping/adding frames from the buffer
        synchronized (audioBuffer) {
            // Try to convert the local packet timestamp (in samples) into the global buffer timestamp (in milliseconds)
            int userTimestampOffset = this.userTimestampOffsets
                    .computeIfAbsent(packet.getSSRC(), ssrc -> packet.getTimestamp() - (SAMPLES_PER_MS * audioBuffer.getNewPlacementTimestamp()));
            int packetTimestampRelativeToBuffer = (packet.getTimestamp() - userTimestampOffset) / 48;

            AudioFrame targetAudioFrame = this.audioBuffer.getFrameAtTimestamp(packetTimestampRelativeToBuffer);
            if (targetAudioFrame == null) {
                // We have fallen out of the range of the buffer
                System.err.println("Frame from user " + packet.getUserId() + " fell out of range of the audio buffer.");

                // Recalculate this user's timestamp offset so that at least this frame is guaranteed to be buffered
                userTimestampOffset = packet.getTimestamp() - (SAMPLES_PER_MS * audioBuffer.getNewPlacementTimestamp());
                this.userTimestampOffsets.put(packet.getSSRC(), userTimestampOffset);
                packetTimestampRelativeToBuffer = (packet.getTimestamp() - userTimestampOffset) / 48;

                targetAudioFrame = this.audioBuffer.getFrameAtTimestamp(packetTimestampRelativeToBuffer);
                if (targetAudioFrame == null) {
                    // This shouldn't be reachable, and would indicate a problem in the audioBuffer implementation.
                    System.err.println("Could not place user " + packet.getUserId() + " back into range of the audio buffer. Skipping a frame...");
                    return;
                }
            }

            // Add this user's audio data to the combined buffer in the target frame
            final ByteBuffer combinationBuffer = targetAudioFrame.combinationBuffer;
            for (int i = 0; i < OpusPacket.OPUS_FRAME_SIZE * OpusPacket.OPUS_CHANNEL_COUNT * Float.BYTES; i += Float.BYTES) {
                float sum = this.temporaryReadBuffer.getFloat(i) + combinationBuffer.getFloat(i);
                combinationBuffer.putFloat(i, sum);
            }

            // Indicate that this user is talking on this frame
            if (targetAudioFrame.usersSpeaking.contains(packet.getUserId())) {
                System.err.println("Got 2 packets for user " + packet.getUserId() + " and timestamp " + packet.getTimestamp() +
                        " (this is expected after a \"fell out of range\" message).");
            }

            targetAudioFrame.usersSpeaking.add(packet.getUserId());
        }
    }

    @Override
    public boolean canReceiveCombined() {
        // Tell JDA that we want to opt for encoded audio instead of combined audio
        return false;
    }

    @Override
    public boolean canReceiveUser() {
        // Tell JDA that we want to opt for encoded audio instead of individual user audio
        return false;
    }

    @Override
    public boolean canReceiveEncoded() {
        // Tell JDA that we want to opt for encoded audio
        return true;
    }
}

package sr.portal2.discord2ip.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.walkerknapp.devolay.DevolayAudioFrameInterleaved32f;
import net.dv8tion.jda.api.audio.OpusPacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioFrame {
    private static final ByteBuffer zeroingBuffer = ByteBuffer.allocateDirect(OpusPacket.OPUS_FRAME_SIZE * OpusPacket.OPUS_CHANNEL_COUNT * Float.BYTES);

    public final ByteBuffer combinationBuffer;
    public final LongSet usersSpeaking;

    public final DevolayAudioFrameInterleaved32f devolayFrame;

    public AudioFrame() {
        this.combinationBuffer = ByteBuffer.allocateDirect(OpusPacket.OPUS_FRAME_SIZE * OpusPacket.OPUS_CHANNEL_COUNT * Float.BYTES);
        this.combinationBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.usersSpeaking = new LongOpenHashSet();

        this.devolayFrame = new DevolayAudioFrameInterleaved32f();
        this.devolayFrame.setSamples(OpusPacket.OPUS_FRAME_SIZE);
        this.devolayFrame.setData(this.combinationBuffer);
        this.devolayFrame.setChannels(OpusPacket.OPUS_CHANNEL_COUNT);
        this.devolayFrame.setSampleRate(OpusPacket.OPUS_SAMPLE_RATE);
    }

    public void reset() {
        // Zero the combination buffer
        this.combinationBuffer.position(0);
        zeroingBuffer.position(0);
        this.combinationBuffer.put(zeroingBuffer);

        // Reset the users speaking
        this.usersSpeaking.clear();
    }
}

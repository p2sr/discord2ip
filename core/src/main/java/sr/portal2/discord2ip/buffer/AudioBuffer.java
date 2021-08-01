package sr.portal2.discord2ip.buffer;

import net.dv8tion.jda.api.audio.OpusPacket;
import sr.portal2.discord2ip.output.NDIAudioSink;
import sr.portal2.discord2ip.bot.DiscordBot;

import java.util.LinkedList;

/**
 * A circular buffer that holds a fixed number of opus audio frames.
 * Incoming frames from the {@link DiscordBot} accumulate in these frames,
 * and frames are read, cleared, and reallocated by the {@link NDIAudioSink} whether.
 *
 * All timestamps in this file are in MS.
 */
public class AudioBuffer {
    private final int frameCount;
    private final LinkedList<AudioFrame> queuedFrames;

    private int currentTailTimestamp;
    private int currentHeadTimestamp;

    public AudioBuffer(int frameCount) {
        this.frameCount = frameCount;

        this.queuedFrames = new LinkedList<>();
        for (int i = 0; i < frameCount; i++) {
            this.queuedFrames.add(new AudioFrame());
        }

        this.currentTailTimestamp = 0;
        this.currentHeadTimestamp = (frameCount - 1) * OpusPacket.OPUS_FRAME_TIME_AMOUNT;
    }

    public AudioFrame getFrameAtTimestamp(int timestamp) {
        if (timestamp < this.currentTailTimestamp) {
            System.err.println("Couldn't get audio frame at " + timestamp + " because the buffer has already passed this timestamp (tail=" + this.currentTailTimestamp + ")");
            return null;
        }

        if (timestamp > this.currentHeadTimestamp) {
            System.err.println("Couldn't get audio frame at " + timestamp + " because the buffer hasn't reached this timestamp (head=" + this.currentHeadTimestamp + ")");
            return null;
        }

        int listIndex = (timestamp - this.currentTailTimestamp) / OpusPacket.OPUS_FRAME_TIME_AMOUNT;
        return this.queuedFrames.get(listIndex);
    }

    public AudioFrame getOldestFrame() {
        return this.queuedFrames.getFirst();
    }

    public void recycleOldestFrame() {
        AudioFrame lastFrame = this.queuedFrames.pop();
        lastFrame.reset();

        this.currentTailTimestamp += OpusPacket.OPUS_FRAME_TIME_AMOUNT;
        this.currentHeadTimestamp += OpusPacket.OPUS_FRAME_TIME_AMOUNT;
        this.queuedFrames.addLast(lastFrame);
    }

    public int getNewPlacementTimestamp() {
        return this.currentTailTimestamp + ((this.frameCount/2) * OpusPacket.OPUS_FRAME_TIME_AMOUNT);
    }

    public int getOldestTimestamp() {
        return this.currentTailTimestamp;
    }
}

package sr.portal2.discord2ip.buffer;

import net.dv8tion.jda.api.audio.OpusPacket;
import sr.portal2.discord2ip.NDIAudioSink;
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

    public AudioBuffer(int frameCount) {
        this.frameCount = frameCount;

        this.queuedFrames = new LinkedList<>();
        for (int i = 0; i < frameCount; i++) {
            this.queuedFrames.add(new AudioFrame());
        }
    }

    public AudioFrame getFrameAtTimestamp(int timestamp) {
        if (timestamp < this.currentTailTimestamp) {
            System.err.println("Couldn't get audio frame at " + timestamp + " because the buffer has already passed this timestamp.");
            return null;
        }

        if (timestamp >= this.currentTailTimestamp + (this.frameCount * OpusPacket.OPUS_FRAME_TIME_AMOUNT)) {
            System.err.println("Couldn't get audio frame at " + timestamp + " because the buffer hasn't reached this timestamp");
            return null;
        }

        int listIndex = (timestamp - this.currentTailTimestamp) / OpusPacket.OPUS_FRAME_TIME_AMOUNT;
        return this.queuedFrames.get(listIndex);
    }

    public AudioFrame getOldestFrame() {
        return this.queuedFrames.getLast();
    }

    public void recycleOldestFrame() {
        this.currentTailTimestamp += OpusPacket.OPUS_FRAME_TIME_AMOUNT;
        this.queuedFrames.addFirst(this.queuedFrames.pollLast());
    }

    public int getYoungestTimestamp() {
        return this.currentTailTimestamp;
    }
}

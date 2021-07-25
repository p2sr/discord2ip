package sr.portal2.discord2ip;

import me.walkerknapp.devolay.DevolaySender;
import net.dv8tion.jda.api.audio.OpusPacket;
import sr.portal2.discord2ip.buffer.AudioBuffer;
import sr.portal2.discord2ip.buffer.AudioFrame;

import java.util.concurrent.atomic.AtomicBoolean;

public class NDIAudioSink implements Runnable {

    private final AudioBuffer audioBuffer;

    private final DevolaySender devolaySender;
    private final AtomicBoolean stopped;

    public NDIAudioSink(AudioBuffer audioBuffer) {
        this.audioBuffer = audioBuffer;

        this.devolaySender = new DevolaySender("Discord Audio Source", null, false, false);
        this.stopped = new AtomicBoolean(false);
    }

    public void stop() {
        this.stopped.set(true);
    }

    @Override
    public void run() {
        long nextFrameNanos = System.nanoTime();
        do {
            synchronized (this.audioBuffer) {
                AudioFrame frame = this.audioBuffer.getOldestFrame();
                frame.devolayFrame.setTimecode(this.audioBuffer.getYoungestTimestamp() * 10L);

                this.devolaySender.sendAudioFrameInterleaved32f(frame.devolayFrame);

                this.audioBuffer.recycleOldestFrame();
            }

            nextFrameNanos += OpusPacket.OPUS_FRAME_TIME_AMOUNT * 1000L;

            while (nextFrameNanos > System.nanoTime()) {
                Thread.onSpinWait();
            }
        } while(!stopped.get());
    }
}

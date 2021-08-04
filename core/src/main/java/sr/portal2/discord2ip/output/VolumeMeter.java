package sr.portal2.discord2ip.output;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep a circular buffer to sound data of a certain # of samples that can be RMSed together to find volume in that range.
 */
public class VolumeMeter {
    private final int samples;
    private final double invSamples;

    private final float[] buffer;
    private int bufferPointer;

    private boolean zeroed = true;
    private AtomicInteger readsSinceLastSample = new AtomicInteger(0);

    public VolumeMeter(int samples) {
        this.samples = samples;
        this.invSamples = 1d/samples;

        this.buffer = new float[samples];
        this.bufferPointer = 0;
    }

    public double getVolume() {
        if (readsSinceLastSample.getAndIncrement() >= 2) {
            this.zero();
        }

        float[] bufferCopy;
        synchronized (buffer) {
            bufferCopy = Arrays.copyOf(buffer, samples);
        }

        if (this.zeroed) {
            return 0d;
        }

        // Take the RMS of the data, will be on range 0-1
        double volume = 0;

        double sample;
        for (int i = 0; i < samples; i++) {
            sample = bufferCopy[i];
            volume += (sample * sample);
        }

        volume *= invSamples;
        return Math.sqrt(volume);
    }

    public void consumeSamples(ByteBuffer audioBuffer, int offset, int strideBytes, int sizeBytes, float multiplier) {
        readsSinceLastSample.set(0);
        synchronized (buffer) {
            this.zeroed = false;

            int inputIndex = offset;
            while (inputIndex < sizeBytes + offset) {
                if (samples == bufferPointer) {
                    bufferPointer = 0;
                }
                buffer[bufferPointer++] = audioBuffer.getFloat(inputIndex) * multiplier;
                inputIndex += strideBytes;
            }
        }
    }

    public void zero() {
        if (!this.zeroed) {
            this.zeroed = true;

            synchronized (buffer) {
                for (int i = 0; i < samples; i++) {
                    buffer[i] = 0;
                }
            }
        }
    }
}

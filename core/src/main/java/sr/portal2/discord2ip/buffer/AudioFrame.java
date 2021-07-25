package sr.portal2.discord2ip.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.dv8tion.jda.api.audio.OpusPacket;

import java.nio.ByteBuffer;

public class AudioFrame {
    public final ByteBuffer combinationBuffer;
    public final LongSet usersSpeaking;

    public AudioFrame() {
        this.combinationBuffer = ByteBuffer.allocateDirect(OpusPacket.OPUS_FRAME_SIZE * OpusPacket.OPUS_CHANNEL_COUNT * Float.BYTES);
        this.usersSpeaking = new LongOpenHashSet();
    }
}

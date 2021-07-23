package sr.portal2.discord2ip.bot;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Provides an opus packet of silence whenever prompted by Discord.
 * Required because discord will not send bots voice information if they are not transmitting anything.
 */
public class SilenceSendHandler implements AudioSendHandler {
    private final ByteBuffer silenceOpusPacket;

    public SilenceSendHandler() {
        // An opus packet containing only 0xF8FFFE corresponds to 20ms of silence.
        this.silenceOpusPacket = ByteBuffer.allocate(3);
        this.silenceOpusPacket.put((byte) 0xF8).put((byte) 0xFF).put((byte) 0xFE);
    }

    @Override
    public boolean canProvide() {
        return true;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        this.silenceOpusPacket.position(0);
        return this.silenceOpusPacket;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}

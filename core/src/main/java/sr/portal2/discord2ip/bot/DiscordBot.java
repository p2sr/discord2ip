package sr.portal2.discord2ip.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.Arrays;

public class DiscordBot {

    private final JDA discordClient;
    private final BotEventListener eventListener;

    private final SilenceSendHandler silenceSendHandler;
    private final VoiceAudioReceiveHandler audioReceiveHandler;

    private AudioManager activeAudioManager;

    public DiscordBot(String token, BotEventListener eventListener) throws LoginException, InterruptedException {
        this.discordClient = JDABuilder.create(token, Arrays.asList(GatewayIntent.values())).build().awaitReady();
        this.eventListener = eventListener;

        this.silenceSendHandler = new SilenceSendHandler();
        this.audioReceiveHandler = new VoiceAudioReceiveHandler();
    }

    public DiscordBot(String token) throws LoginException, InterruptedException {
        this(token, new AbstractBotEventListener() { /* no-op */ });
    }

    public boolean joinVoiceChannel(long channelId) {
        // Try to find the requested channel to join
        VoiceChannel channel = this.discordClient.getVoiceChannelById(channelId);
        if (channel == null) {
            return false;
        }

        // If we're already connected to a channel, disconnect from it
        this.leaveVoiceChannel();

        // Join the new voice channel
        this.activeAudioManager = channel.getGuild().getAudioManager();
        this.activeAudioManager.setReceivingHandler(this.audioReceiveHandler);
        this.activeAudioManager.setSendingHandler(this.silenceSendHandler);
        this.activeAudioManager.openAudioConnection(channel);

        return true;
    }

    public void leaveVoiceChannel() {
        if (this.activeAudioManager != null) {
            if (this.activeAudioManager.getConnectionStatus() != ConnectionStatus.NOT_CONNECTED) {
                this.activeAudioManager.closeAudioConnection();
            }
        }
    }
}

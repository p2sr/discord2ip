package sr.portal2.discord2ip.bot;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import sr.portal2.discord2ip.buffer.AudioBuffer;

import javax.security.auth.login.LoginException;
import java.util.Arrays;

public class DiscordBot extends ListenerAdapter {

    private final JDA discordClient;
    private BotEventListener eventListener;

    private final SilenceSendHandler silenceSendHandler;
    private final VoiceAudioReceiveHandler audioReceiveHandler;

    private AudioManager activeAudioManager;

    public DiscordBot(String token, AudioBuffer audioBuffer, BotEventListener eventListener) throws LoginException, InterruptedException {
        this.discordClient = JDABuilder.create(token, Arrays.asList(GatewayIntent.values()))
                .addEventListeners(this).build().awaitReady();
        this.eventListener = eventListener;

        this.silenceSendHandler = new SilenceSendHandler();
        this.audioReceiveHandler = new VoiceAudioReceiveHandler(audioBuffer);
    }

    public DiscordBot(String token, AudioBuffer audioBuffer) throws LoginException, InterruptedException {
        this(token, audioBuffer, new AbstractBotEventListener() { /* no-op */ });
    }

    public void setEventListener(BotEventListener eventListener) {
        this.eventListener = eventListener;
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

    public LongSet getVisibleUsers() {
        return this.discordClient.getGuilds().stream()
                .flatMap(guild -> guild.getVoiceStates().stream())
                .filter(GuildVoiceState::inVoiceChannel)
                .filter(state -> !state.getMember().getUser().isBot())
                .mapToLong(state -> state.getMember().getIdLong())
                .collect(LongOpenHashSet::new, LongOpenHashSet::add, LongOpenHashSet::addAll);
    }

    public LongSet getAvailableChannels() {
        return this.discordClient.getGuilds().stream()
                .flatMap(guild -> guild.getVoiceChannels().stream())
                .mapToLong(VoiceChannel::getIdLong)
                .collect(LongOpenHashSet::new, LongOpenHashSet::add, LongOpenHashSet::addAll);
    }

    public String getName() {
        return this.discordClient.getSelfUser().getName();
    }

    public String getAvatarUrl() {
        return this.discordClient.getSelfUser().getAvatarUrl();
    }

    public String getState() {
        return this.discordClient.getStatus() == JDA.Status.CONNECTED ? "online" : "disconnected";
    }

    public String getUserName(String userId) {
        User user = this.discordClient.getUserById(userId);
        return user == null ? "<@" + userId + ">" : user.getName();
    }

    public String getUserAvatarUrl(String userId) {
        User user = this.discordClient.getUserById(userId);
        return user == null ? "" : user.getAvatarUrl();
    }

    public String getUserChannel(String userId) {
        return this.discordClient.getGuilds().stream()
                .flatMap(guild -> guild.getVoiceStates().stream())
                .filter(voiceState -> voiceState.getMember().getId().equals(userId))
                .filter(GuildVoiceState::inVoiceChannel)
                .map(voiceState -> voiceState.getChannel().getId())
                .findAny().orElse(null);
    }

    public String getChannelName(long channelId) {
        VoiceChannel channel = this.discordClient.getVoiceChannelById(channelId);
        return channel == null ? "<#" + channelId + ">" : channel.getName();
    }

    public String getChannelServer(long channelID) {
        VoiceChannel channel = this.discordClient.getVoiceChannelById(channelID);
        return channel == null ? "" : channel.getGuild().getName();
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        // When the bot reconnects from a disconnect, update with all visible users
        this.getVisibleUsers().forEach(this.eventListener::onNewVisibleUser);
        this.eventListener.onUserInfoUpdate();
    }

    @Override
    public void onVoiceChannelCreate(@NotNull VoiceChannelCreateEvent event) {
        // Refresh the channels list when a new VC is created
        this.eventListener.onAvailableChannelsUpdate(this.getAvailableChannels());
    }

    @Override
    public void onVoiceChannelDelete(@NotNull VoiceChannelDeleteEvent event) {
        // Refresh the channels list when a VC is deleted
        this.eventListener.onAvailableChannelsUpdate(this.getAvailableChannels());
    }

    @Override
    public void onVoiceChannelUpdateName(@NotNull VoiceChannelUpdateNameEvent event) {
        // Refresh the channels list when a VC is renamed
        this.eventListener.onAvailableChannelsUpdate(this.getAvailableChannels());
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        // Trigger a user information refresh when a user moves
        this.eventListener.onNewVisibleUser(event.getMember().getIdLong());

        // TODO: If this ends up being a heavy operation, we could only refresh relevant users
        this.eventListener.onUserInfoUpdate();
    }
}

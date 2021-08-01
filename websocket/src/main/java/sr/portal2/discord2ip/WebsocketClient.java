package sr.portal2.discord2ip;

import com.dslplatform.json.DslJson;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.portal2.discord2ip.bot.BotEventListener;
import sr.portal2.discord2ip.bot.DiscordBot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WebsocketClient extends WebSocketListener implements BotEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClient.class);

    private static final Path usersTmpPath = Path.of("users.json.tmp");
    private static final Path usersPath = Path.of("users.json");

    private final DiscordBot discordBot;
    private final DslJson<Object> dslJson;

    private final ObjectSet<WebsocketMessage.User> exposedUsers;

    public final CompletableFuture<Void> openFuture = new CompletableFuture<>();
    public final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public WebSocket currentWsConnection;

    public WebsocketClient(DiscordBot bot) {
        this.discordBot = bot;

        this.dslJson = new DslJson<>(new DslJson.Settings<>().includeServiceLoader());

        this.exposedUsers = new ObjectOpenHashSet<>();
    }

    private void loadUsers() throws IOException {
        if (Files.exists(usersPath)) {
            synchronized (this.exposedUsers) {
                exposedUsers.clear();
                try (InputStream fis = Files.newInputStream(usersPath)) {
                    List<WebsocketMessage.User> newUsers = dslJson.deserializeList(WebsocketMessage.User.class, fis);
                    if (newUsers != null) {
                        exposedUsers.addAll(newUsers);
                    }
                }

                // TODO: Indicate to the discordBot any volumes that are changed here.
            }
        }
    }

    private void saveUsers() throws IOException {
        Files.deleteIfExists(usersTmpPath);

        try (OutputStream fos = Files.newOutputStream(usersTmpPath)) {
            dslJson.serialize(this.exposedUsers, fos);
        }

        Files.copy(usersTmpPath, usersPath, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(usersTmpPath);
    }

    public void sendIdentity() {
        WebsocketMessage message = new WebsocketMessage();
        message.type = WebsocketMessage.UPDATE_IDENTITY_TYPE;
        message.identity = new WebsocketMessage.Identity(this.discordBot.getName(), this.discordBot.getAvatarUrl(), this.discordBot.getState());
        try {
            this.currentWsConnection.send(message.serialize(dslJson));
        } catch (IOException e) {
            logger.error("Failed to serialize message", e);
        }
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        openFuture.complete(null);
        this.currentWsConnection = webSocket;

        try {
            // Load any users we should be aware of from a file
            this.loadUsers();

            // Send a full update of our visible information
            this.sendIdentity();
            this.discordBot.getVisibleUsers().forEach(this::onNewVisibleUser);
            this.onUserInfoUpdate();
            this.onAvailableChannelsUpdate(this.discordBot.getAvailableChannels());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        try {
            byte[] data = text.getBytes();
            WebsocketMessage message = dslJson.deserialize(WebsocketMessage.class, data, data.length);

            if (message == null) {
                logger.error("Failed to deserialize message, DSLJson returned null.");
                return;
            }

            switch (message.type) {
                case WebsocketMessage.UPDATE_CURRENT_CHANNEL_TYPE:
                    if (message.currentChannel == null) {
                        logger.info("Leaving vc...");
                        this.discordBot.leaveVoiceChannel();
                    } else {
                        logger.info("Joining vc {}", message.currentChannel);
                        this.discordBot.joinVoiceChannel(Long.parseLong(message.currentChannel));
                    }
                    break;
                default:
                    logger.error("Unknown message type: {}", message.type);
            }
        } catch (IOException e) {
            logger.error("Failed to deserialize message", e);
        }
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        closeFuture.complete(null);
    }

    @Override
    public void onNewVisibleUser(long userId) {
        this.exposedUsers.add(new WebsocketMessage.User(String.valueOf(userId)));
    }

    @Override
    public void onUserInfoUpdate() {
        for (WebsocketMessage.User user : this.exposedUsers) {
            user.name = this.discordBot.getUserName(user.id);
            user.avatar = this.discordBot.getUserAvatarUrl(user.id);
            user.channel = this.discordBot.getUserChannel(user.id);
        }

        try {
            this.saveUsers();
        } catch (IOException e) {
            logger.error("Failed to save users", e);
        }

        WebsocketMessage message = new WebsocketMessage();
        message.type = WebsocketMessage.UPDATE_USERS_TYPE;
        message.users = this.exposedUsers;
        try {
            this.currentWsConnection.send(message.serialize(dslJson));
        } catch (IOException e) {
            logger.error("Failed to serialize message", e);
        }
    }

    @Override
    public void onUserVolumeUpdate(LongList userIds, DoubleList leftVolumes, DoubleList rightVolumes) {
        ObjectArrayList<WebsocketMessage.VolumeUpdate> volumeUpdates = new ObjectArrayList<>(userIds.size());

        for (int i = 0; i < userIds.size(); i++) {
            volumeUpdates.add(i, new WebsocketMessage.VolumeUpdate(String.valueOf(userIds.getLong(i)), leftVolumes.getDouble(i), rightVolumes.getDouble(i)));
        }

        WebsocketMessage message = new WebsocketMessage();
        message.type = WebsocketMessage.UPDATE_VOLUMES;
        message.volumeUpdates = volumeUpdates;
        try {
            this.currentWsConnection.send(message.serialize(dslJson));
        } catch (IOException e) {
            logger.error("Failed to serialize message", e);
        }
    }

    @Override
    public void onAvailableChannelsUpdate(LongSet channelIds) {
        WebsocketMessage message = new WebsocketMessage();
        message.type = WebsocketMessage.UPDATE_CHANNELS_TYPE;
        message.channels = channelIds.longStream()
                .mapToObj(id -> new WebsocketMessage.Channel(String.valueOf(id), this.discordBot.getChannelName(id), this.discordBot.getChannelServer(id)))
                .collect(Collectors.toList());
        try {
            this.currentWsConnection.send(message.serialize(dslJson));
        } catch (IOException e) {
            logger.error("Failed to serialize message", e);
        }
    }
}

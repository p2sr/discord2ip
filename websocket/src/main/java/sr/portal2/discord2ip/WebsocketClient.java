package sr.portal2.discord2ip;

import it.unimi.dsi.fastutil.longs.LongSet;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import sr.portal2.discord2ip.bot.BotEventListener;
import sr.portal2.discord2ip.bot.DiscordBot;

import java.util.concurrent.CompletableFuture;

public class WebsocketClient extends WebSocketListener implements BotEventListener {

    public final DiscordBot discordBot;

    public final CompletableFuture<Void> openFuture = new CompletableFuture<>();
    public final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public WebSocket currentWsConnection;
    public WebsocketClient(DiscordBot bot) {
        this.discordBot = bot;
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        openFuture.complete(null);
        this.currentWsConnection = webSocket;
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        closeFuture.complete(null);
    }

    @Override
    public void onNewVisibleUser(long userId) {
    }

    @Override
    public void onUserInfoUpdate() {
    }

    @Override
    public void onAvailableChannelsUpdate(LongSet channelIds) {
    }
}

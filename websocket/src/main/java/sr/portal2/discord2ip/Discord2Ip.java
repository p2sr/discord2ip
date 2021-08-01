package sr.portal2.discord2ip;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import sr.portal2.discord2ip.bot.DiscordBot;
import sr.portal2.discord2ip.buffer.AudioBuffer;
import sr.portal2.discord2ip.output.NDIAudioSink;

import javax.security.auth.login.LoginException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Discord2Ip {
    public static void main(String[] args) throws InterruptedException, LoginException {
        if (args.length < 2) {
            System.err.println("Syntax: [serverUrl] [botToken]");
            return;
        }
        String serverUrl = args[0];
        String botToken = args[1];

        // Set up bot and NDI sending
        AudioBuffer audioBuffer = new AudioBuffer(10);
        DiscordBot bot = new DiscordBot(botToken, audioBuffer);
        NDIAudioSink sink = new NDIAudioSink(audioBuffer);

        Executors.newSingleThreadExecutor().submit(sink);

        // Try to connect the websocket client so we can be controlled from NodeCG
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        while (true) {
            try {
                Request wsOpenRequest = new Request.Builder()
                        .url(serverUrl)
                        .build();

                WebsocketClient client = new WebsocketClient(bot);
                bot.setEventListener(client);

                WebSocket webSocket = okHttpClient.newWebSocket(wsOpenRequest, client);

                client.openFuture.get(5, TimeUnit.SECONDS);
                client.closeFuture.join();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            System.err.println("Disconnected, retrying connection in 1 second...");
            Thread.sleep(1000L);
        }
    }
}

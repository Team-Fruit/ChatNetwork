package net.teamfruit.chatnetwork.network;

import com.google.common.base.Charsets;
import net.minecraftforge.common.MinecraftForge;
import net.teamfruit.chatnetwork.ChatData;
import net.teamfruit.chatnetwork.Log;
import net.teamfruit.chatnetwork.ModConfig;
import net.teamfruit.chatnetwork.event.NetworkClientChatEvent;
import net.teamfruit.chatnetwork.event.PlayerListUpdateEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ChatSender {
    public void send(ChatData data) {
        if (!onNetworkClientChatEvent(data))
            return;

        String body = ChatData.Serializer.chatDataToJson(data);

        for (String target : ModConfig.api.address) {
            CompletableFuture.supplyAsync(() -> {
                HttpURLConnection urlConn = null;

                try {
                    URL url = new URL(target);

                    urlConn = (HttpURLConnection) url.openConnection();
                    urlConn.setRequestMethod("POST");
                    urlConn.setRequestProperty("User-Agent", ModConfig.api.useragent);
                    urlConn.setRequestProperty("Accept-Language", Locale.getDefault().toString());
                    urlConn.addRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    urlConn.setDoInput(true);
                    urlConn.setDoOutput(true);

                    try (OutputStream out = urlConn.getOutputStream()) {
                        IOUtils.write(body, out, Charsets.UTF_8);
                    }
                    urlConn.connect();

                    int status = urlConn.getResponseCode();
                    if (!(status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_NO_CONTENT)) {
                        Log.log.debug("Cloud not send message: " + target + " returned invalid status code " + status);
                        return false;
                    }

                    try {
                        String response = IOUtils.toString(urlConn.getInputStream(), Charsets.UTF_8);
                        if (!StringUtils.isEmpty(response)) {
                            ChatData res = ChatData.Serializer.jsonToChatData(response);
                            if (res.players != null)
                                MinecraftForge.EVENT_BUS.post(new PlayerListUpdateEvent(res));
                        }
                    } catch (Exception e) {
                        Log.log.debug("Cloud not parse response message from " + target);
                    }

                    return true;

                } catch (IOException e) {
                    Log.log.debug("Cloud not send message to " + target);
                    return false;

                } finally {
                    if (urlConn != null)
                        urlConn.disconnect();
                }
            });
        }
    }

    @Nullable
    private static boolean onNetworkClientChatEvent(ChatData data) {
        NetworkClientChatEvent event = new NetworkClientChatEvent(data);
        event.component = event.data.component;
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        event.data.component = event.component;
        return true;
    }
}

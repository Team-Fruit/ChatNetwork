package net.teamfruit.chatnetwork.network;

import com.google.common.base.Charsets;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.teamfruit.chatnetwork.event.NetworkServerChatEvent;
import net.teamfruit.chatnetwork.util.ServerThreadExecutor;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatReceiver {
    private HttpServer httpServer;
    private ExecutorService httpThreadPool;

    public void start(MinecraftServer server, int port) throws IOException {
        if (httpServer != null)
            throw new IllegalStateException("ChatNetwork is already listening");

        httpServer = HttpServer.create(new InetSocketAddress(port), port);
        httpThreadPool = Executors.newFixedThreadPool(1);
        httpServer.setExecutor(httpThreadPool);
        httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exc) throws IOException {
                boolean success = true;
                try {
                    ChatData body = ChatData.Serializer.jsonToChatData(IOUtils.toString(exc.getRequestBody(), Charsets.UTF_8));
                    CompletableFuture.runAsync(() -> {
                        ITextComponent itextcomponent = body.component;
                        itextcomponent = onNetworkServerChatEvent(body.getProfile(), body.content, itextcomponent);
                        if (itextcomponent == null) return;
                        server.getPlayerList().sendMessage(itextcomponent, false);
                    }, ServerThreadExecutor.INSTANCE);
                } catch (JsonParseException e) {
                    success = false;
                }
                if (success)
                    exc.sendResponseHeaders(204, 0);
                else
                    exc.sendResponseHeaders(400, 0);
            }
        });
        httpServer.start();
    }

    @Nullable
    public static ITextComponent onNetworkServerChatEvent(GameProfile player, String raw, ITextComponent comp) {
        NetworkServerChatEvent event = new NetworkServerChatEvent(player, raw, comp);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return null;
        }
        return event.getComponent();
    }

    public void stop() {
        if (httpServer != null)
            httpServer.stop(1);
        if (httpThreadPool != null)
            httpThreadPool.shutdownNow();
    }
}

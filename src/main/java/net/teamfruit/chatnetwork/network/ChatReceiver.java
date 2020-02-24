package net.teamfruit.chatnetwork.network;

import com.google.common.base.Charsets;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.teamfruit.chatnetwork.ChatData;
import net.teamfruit.chatnetwork.Log;
import net.teamfruit.chatnetwork.ModConfig;
import net.teamfruit.chatnetwork.event.NetworkServerChatEvent;
import net.teamfruit.chatnetwork.util.ServerThreadExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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
                    ChatData data = ChatData.Serializer.jsonToChatData(IOUtils.toString(exc.getRequestBody(), Charsets.UTF_8));
                    CompletableFuture.runAsync(() -> {
                        String name = StringUtils.isEmpty(data.servername)
                                ? String.format(ModConfig.messages.displayNameMessage, data.username)
                                : String.format(ModConfig.messages.displayNameWithServerMessage, data.username, data.servername);
                        ITextComponent itextcomponent = data.component;
                        if (itextcomponent == null)
                            itextcomponent = new TextComponentTranslation("chat.type.text", name, ForgeHooks.newChatWithLinks(data.content));
                        itextcomponent = onNetworkServerChatEvent(data, itextcomponent);
                        if (itextcomponent == null) return;
                        server.getPlayerList().sendMessage(itextcomponent, false);
                    }, ServerThreadExecutor.INSTANCE);

                } catch (JsonParseException e) {
                    Log.log.warn("Failed to parse network chat packet", e);
                    success = false;
                }
                if (success)
                    exc.sendResponseHeaders(204, -1);
                else
                    exc.sendResponseHeaders(400, -1);
            }
        });
        httpServer.start();

        Log.log.info("ChatNetwork starts listening at port " + port);
    }

    @Nullable
    public static ITextComponent onNetworkServerChatEvent(ChatData data, ITextComponent itextcomponent) {
        NetworkServerChatEvent event = new NetworkServerChatEvent(data);
        event.component = itextcomponent;
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return null;
        }
        return event.component;
    }

    public void stop() {
        if (httpServer != null)
            httpServer.stop(1);
        if (httpThreadPool != null)
            httpThreadPool.shutdownNow();
    }
}

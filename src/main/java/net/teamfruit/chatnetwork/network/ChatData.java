package net.teamfruit.chatnetwork.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;

import javax.annotation.Nullable;
import java.util.UUID;

public class ChatData {
    public String username;
    public String userid;
    public String content;
    public ITextComponent component;

    public GameProfile getProfile() {
        try {
            UUID uuid = UUIDTypeAdapter.fromString(userid);
            GameProfile ret = new GameProfile(uuid, username);
            return ret;
        } catch (IllegalArgumentException e) {
            return new GameProfile(net.minecraft.entity.player.EntityPlayer.getUUID(new GameProfile((UUID) null, username)), username);
        }
    }

    public static class Serializer {
        private static final Gson GSON;

        public static String chatDataToJson(ChatData component) {
            return GSON.toJson(component);
        }

        @Nullable
        public static ChatData jsonToChatData(String json) {
            return (ChatData) JsonUtils.gsonDeserialize(GSON, json, ChatData.class, false);
        }

        static
        {
            GsonBuilder gsonbuilder = new GsonBuilder();
            gsonbuilder.registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer());
            gsonbuilder.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
            gsonbuilder.registerTypeAdapterFactory(new EnumTypeAdapterFactory());
            GSON = gsonbuilder.create();
        }
    }
}

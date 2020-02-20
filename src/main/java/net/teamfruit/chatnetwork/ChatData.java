package net.teamfruit.chatnetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;

import javax.annotation.Nullable;

public class ChatData {
    public String username;
    public String content;
    public String servername;
    public ITextComponent component;

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

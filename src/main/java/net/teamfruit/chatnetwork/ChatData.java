package net.teamfruit.chatnetwork;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.world.GameType;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

public class ChatData {
    public String username;
    public String content;
    public String servername;
    public ITextComponent component;
    public List<PlayerData> players;

    public static class PlayerData {
        public int ping;
        public GameType gamemode;
        public GameProfile profile;
        public ITextComponent displayName;

        public PlayerData() {
        }

        public PlayerData(GameProfile profileIn, int latencyIn, GameType gameModeIn, @Nullable ITextComponent displayNameIn) {
            this.profile = profileIn;
            this.ping = latencyIn;
            this.gamemode = gameModeIn;
            this.displayName = displayNameIn;
        }

        public static PlayerData createFromEntityPlayer(EntityPlayerMP p) {
            return new PlayerData(p.getGameProfile(), p.ping, p.interactionManager.getGameType(), p.getTabListDisplayName());
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

        public static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
            @Override
            public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json instanceof JsonObject) {
                    final JsonObject object = (JsonObject) json;

                    UUID id = null;
                    if (object.has("id"))
                        id = context.deserialize(object.get("id"), UUID.class);
                    String name = null;
                    if (object.has("name"))
                        name = object.get("name").getAsString();

                    GameProfile profile = new GameProfile(id, name);

                    if (object.has("properties"))
                        profile.getProperties().putAll(context.deserialize(object.get("properties"), PropertyMap.class));

                    return profile;
                }

                return null;
            }

            @Override
            public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonobject = new JsonObject();

                jsonobject.add("id", context.serialize(src.getId(), UUID.class));
                jsonobject.addProperty("name", src.getName());
                jsonobject.add("properties", context.serialize(src.getProperties(), PropertyMap.class));

                return jsonobject;
            }
        }

        static {
            GsonBuilder gsonbuilder = new GsonBuilder();
            gsonbuilder.registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer());
            gsonbuilder.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
            gsonbuilder.registerTypeHierarchyAdapter(PropertyMap.class, new PropertyMap.Serializer());
            gsonbuilder.registerTypeHierarchyAdapter(GameProfile.class, new GameProfileSerializer());
            gsonbuilder.registerTypeAdapterFactory(new EnumTypeAdapterFactory());
            GSON = gsonbuilder.create();
        }
    }
}

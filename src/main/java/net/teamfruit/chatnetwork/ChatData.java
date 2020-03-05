package net.teamfruit.chatnetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.world.GameType;

import javax.annotation.Nullable;
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
        public Profile profile;
        public ITextComponent displayName;

        public PlayerData() {
        }

        public PlayerData(Profile profileIn, int latencyIn, GameType gameModeIn, @Nullable ITextComponent displayNameIn) {
            this.profile = profileIn;
            this.ping = latencyIn;
            this.gamemode = gameModeIn;
            this.displayName = displayNameIn;
        }

        public static PlayerData createFromEntityPlayer(EntityPlayerMP p) {
            return new PlayerData(new ChatData.PlayerData.Profile(p.getGameProfile()), p.ping, p.interactionManager.getGameType(), p.getTabListDisplayName());
        }

        public static class Profile {
            public UUID id;
            public String name;

            public Profile() {
            }

            public Profile(GameProfile profile) {
                this.id = profile.getId();
                this.name = profile.getName();
            }

            public GameProfile toGameProfile() {
                return new GameProfile(this.id, this.name);
            }
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

        static {
            GsonBuilder gsonbuilder = new GsonBuilder();
            gsonbuilder.registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer());
            gsonbuilder.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
            gsonbuilder.registerTypeAdapterFactory(new EnumTypeAdapterFactory());
            GSON = gsonbuilder.create();
        }
    }
}

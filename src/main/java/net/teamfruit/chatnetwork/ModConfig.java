package net.teamfruit.chatnetwork;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Reference.MODID, name = Reference.MODID)
public class ModConfig {

    @Config.Name("API")
    @Config.Comment({ "Settings of Cloud API" })
    public static Api api = new Api();

    @Config.Name("Messages")
    @Config.Comment({ "Settings of Messages" })
    public static Messages messages = new Messages();

    public static class Api {

        @Config.Name("Port")
        @Config.Comment({ "Listening Port" })
        @Config.RequiresMcRestart
        public int port = 25541;

        @Config.Name("Name")
        @Config.Comment({ "Server Name" })
        public String name = "";

        @Config.Name("URL")
        @Config.Comment({ "Http Address to Send - ex) http://localhost:25542" })
        public String[] address = { "http://localhost:25542" };

        @Config.Name("User Agent")
        @Config.Comment({ "Http User Agent" })
        public String useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36";
    }

    public static class Messages {

        @Config.Name("Display Name Message")
        @Config.Comment({ "'%s' -> '<PlayerName> Message'" })
        public String displayNameMessage = "%s";

        @Config.Name("Display Name with Server Message")
        @Config.Comment({ "'%s[%s]' -> '<PlayerName[ServerName]> Message'" })
        public String displayNameWithServerMessage = "%s[%s]";

        @Config.Name("Server Name Message")
        @Config.Comment({ "'%s Server' -> 'ServerName Server'" })
        public String serverNameMessage = "%s Server";

        @Config.Name("Server Start Message")
        @Config.Comment({ "'%s Server is started' -> 'ServerName Server is started'" })
        public String serverStartMessage = "%s Server is started";

        @Config.Name("Server Stop Message")
        @Config.Comment({ "'%s Server is started' -> 'ServerName Server is stopped'" })
        public String serverStopMessage = "%s Server is stopped";
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID)
    public static class Handler {

        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Reference.MODID))
                ConfigManager.load(Reference.MODID, Config.Type.INSTANCE);
        }
    }
}

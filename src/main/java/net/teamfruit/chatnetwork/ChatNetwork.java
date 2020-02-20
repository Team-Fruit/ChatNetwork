package net.teamfruit.chatnetwork;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.teamfruit.chatnetwork.command.ModCommand;
import net.teamfruit.chatnetwork.network.ChatReceiver;
import net.teamfruit.chatnetwork.network.ChatSender;
import net.teamfruit.chatnetwork.util.ServerThreadExecutor;

import java.io.IOException;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, acceptableRemoteVersions = "*")
public class ChatNetwork {
	private ChatSender sender = new ChatSender();
	private ChatReceiver receiver = new ChatReceiver();

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		Log.log.info("Starting ChatNetwork...");

		event.registerServerCommand(new ModCommand());

		try {
			receiver.start(event.getServer(), ModConfig.api.port);
		} catch (IOException e) {
			Log.log.error("ChatNetwork could not start listening: ", e);
		}
	}

	@Mod.EventHandler
	public void onServerStopping(FMLServerStoppingEvent event) {
		receiver.stop();
	}

	@SubscribeEvent
	public void onChatSend(ServerChatEvent event) {
		sender.send(event);
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			ServerThreadExecutor.INSTANCE.executeQueuedTaskImmediately();
		}
	}
}

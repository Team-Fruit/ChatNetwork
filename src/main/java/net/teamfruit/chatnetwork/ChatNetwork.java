package net.teamfruit.chatnetwork;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.teamfruit.chatnetwork.command.ModCommand;
import net.teamfruit.chatnetwork.event.NetworkClientChatEvent;
import net.teamfruit.chatnetwork.network.ChatReceiver;
import net.teamfruit.chatnetwork.network.ChatSender;
import net.teamfruit.chatnetwork.util.Base64Utils;
import net.teamfruit.chatnetwork.util.ServerThreadExecutor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

		ITextComponent component = new TextComponentString(String.format(ModConfig.messages.serverStartMessage, ModConfig.api.name));
		ChatData data = new ChatData();
		data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
		data.content = component.getUnformattedText();
		data.servername = ModConfig.api.name;
		data.component = component;
		sender.send(data);
	}

	@Mod.EventHandler
	public void onServerStopping(FMLServerStoppingEvent event) {
		ITextComponent component = new TextComponentString(String.format(ModConfig.messages.serverStopMessage, ModConfig.api.name));
		ChatData data = new ChatData();
		data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
		data.content = component.getUnformattedText();
		data.servername = ModConfig.api.name;
		data.component = component;
		sender.send(data);

		receiver.stop();
	}

	@SubscribeEvent
	public void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
		GameProfile gameprofile = event.player.getGameProfile();
		PlayerProfileCache playerprofilecache = event.player.getServer().getPlayerProfileCache();
		GameProfile gameprofile1 = playerprofilecache.getProfileByUUID(gameprofile.getId());
		String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();
		TextComponentTranslation textcomponenttranslation;
		if (event.player.getName().equalsIgnoreCase(s))
			textcomponenttranslation = new TextComponentTranslation("multiplayer.player.joined", event.player.getDisplayName());
		else
			textcomponenttranslation = new TextComponentTranslation("multiplayer.player.joined.renamed", event.player.getDisplayName(), s);
		textcomponenttranslation.getStyle().setColor(TextFormatting.YELLOW);

		ChatData data = new ChatData();
		data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
		data.content = textcomponenttranslation.getUnformattedText();
		data.servername = ModConfig.api.name;
		data.component = textcomponenttranslation;
		sender.send(data);
	}

	@SubscribeEvent
	public void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent event) {
		TextComponentTranslation textcomponenttranslation = new TextComponentTranslation("multiplayer.player.left", event.player.getDisplayName());
		textcomponenttranslation.getStyle().setColor(TextFormatting.YELLOW);

		ChatData data = new ChatData();
		data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
		data.content = textcomponenttranslation.getUnformattedText();
		data.servername = ModConfig.api.name;
		data.component = textcomponenttranslation;
		sender.send(data);
	}

	@SubscribeEvent
	public void onPlayerAdvancementEvent(AdvancementEvent event) {
		Advancement advancement = event.getAdvancement();
		EntityPlayer player = event.getEntityPlayer();
		if (advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceToChat() && event.getEntityPlayer().world.getGameRules().getBoolean("announceAdvancements"))
		{
			TextComponentTranslation textcomponenttranslation = new TextComponentTranslation("chat.type.advancement." + advancement.getDisplay().getFrame().getName(), player.getDisplayName(), advancement.getDisplayText());

			ChatData data = new ChatData();
			data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
			data.content = textcomponenttranslation.getUnformattedText();
			data.servername = ModConfig.api.name;
			data.component = textcomponenttranslation;
			sender.send(data);
		}
	}

	@SubscribeEvent
	public void onPlayerDeathEvent(LivingDeathEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			ITextComponent message = event.getSource().getDeathMessage(player);

			ChatData data = new ChatData();
			data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
			data.content = message.getUnformattedText();
			data.servername = ModConfig.api.name;
			data.component = message;
			sender.send(data);
		}
	}

	@SubscribeEvent
	public void onChatSend(ServerChatEvent event) {
		ChatData data = new ChatData();
		data.username = event.getUsername();
		data.content = event.getMessage();
		data.servername = ModConfig.api.name;
		data.component = event.getComponent();
		sender.send(data);
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			ServerThreadExecutor.INSTANCE.executeQueuedTaskImmediately();
		}
	}

	@SubscribeEvent
	public void onReplace(NetworkClientChatEvent event) {
		String message = event.data.content;
		message = StringUtils.replace(message, "@everyone", "\uFF20everyone");
		message = StringUtils.replace(message, "@here", "\uFF20here");
		event.data.content = message;
	}

	@Nonnull
	static final Pattern pattern = Pattern.compile("<(a?)\\:(\\w+?)\\:([a-zA-Z0-9+/=]+?)>");

	public static String toDecimalId(final String id) {
		try {
			return Long.toString(Base64Utils.decode(id));
		} catch (final IllegalArgumentException e) {
		}
		return id;
	}

	@SubscribeEvent
	public void onEmojiReplace(NetworkClientChatEvent event) {
		String message = event.data.content;

		StringBuffer sb = new StringBuffer();
		Matcher matcher = pattern.matcher(message);
		while (matcher.find()) {
			final String g1 = matcher.group(1);
			final String g2 = matcher.group(2);
			String g3 = matcher.group(3);
			if (!StringUtils.isEmpty(g3))
				if (StringUtils.length(g3)<=12)
					g3 = toDecimalId(g3);
			matcher.appendReplacement(sb, String.format("<%s:%s:%s>", g1, g2, g3));
		}
		matcher.appendTail(sb);

		event.data.content = sb.toString();
	}
}

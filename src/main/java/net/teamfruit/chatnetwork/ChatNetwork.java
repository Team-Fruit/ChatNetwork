package net.teamfruit.chatnetwork;

import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
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
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.teamfruit.chatnetwork.command.ModCommand;
import net.teamfruit.chatnetwork.event.NetworkClientChatEvent;
import net.teamfruit.chatnetwork.event.PlayerListUpdateEvent;
import net.teamfruit.chatnetwork.network.ChatReceiver;
import net.teamfruit.chatnetwork.network.ChatSender;
import net.teamfruit.chatnetwork.util.Base64Utils;
import net.teamfruit.chatnetwork.util.ServerThreadExecutor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, acceptableRemoteVersions = "*")
public class ChatNetwork {
    public static MinecraftServer server;

    private ChatSender sender = new ChatSender();
    private ChatReceiver receiver = new ChatReceiver();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void sendChat(List<EntityPlayerMP> players, ChatData data) {
        data.servername = ModConfig.api.name;
        data.players = players.stream().map(ChatData.PlayerData::createFromEntityPlayer).collect(Collectors.toList());
        sender.send(data);
    }

    private void sendChatAsServer(List<EntityPlayerMP> players, ITextComponent message) {
        ChatData data = new ChatData();
        data.username = String.format(ModConfig.messages.serverNameMessage, ModConfig.api.name);
        data.content = message.getUnformattedText();
        data.component = message;
        sendChat(players, data);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        Log.log.info("Starting ChatNetwork...");

        server = event.getServer();

        event.registerServerCommand(new ModCommand());

        try {
            receiver.start(ModConfig.api.port);
        } catch (IOException e) {
            Log.log.error("ChatNetwork could not start listening: ", e);
        }

        ITextComponent message = new TextComponentString(String.format(ModConfig.messages.serverStartMessage, ModConfig.api.name));
        sendChatAsServer(server.getPlayerList().getPlayers(), message);
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        ITextComponent message = new TextComponentString(String.format(ModConfig.messages.serverStopMessage, ModConfig.api.name));
        sendChatAsServer(server.getPlayerList().getPlayers(), message);

        receiver.stop();
    }

    private Map<String, Map<String, ChatData.PlayerData>> players = Maps.newHashMap();

    @SubscribeEvent
    public void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
        GameProfile gameprofile = event.player.getGameProfile();
        PlayerProfileCache playerprofilecache = event.player.getServer().getPlayerProfileCache();
        GameProfile gameprofile1 = playerprofilecache.getProfileByUUID(gameprofile.getId());
        String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();
        ITextComponent message;
        if (event.player.getName().equalsIgnoreCase(s))
            message = new TextComponentTranslation("multiplayer.player.joined", event.player.getDisplayName());
        else
            message = new TextComponentTranslation("multiplayer.player.joined.renamed", event.player.getDisplayName(), s);
        message.getStyle().setColor(TextFormatting.YELLOW);

        sendChatAsServer(server.getPlayerList().getPlayers(), message);
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) event.player;
            Collection<ChatData.PlayerData> packetPlayers = players.values().stream().flatMap(e -> e.entrySet().stream()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (i, j) -> i, HashMap::new)).values();
            createPlayerListPacket(SPacketPlayerListItem.Action.ADD_PLAYER, packetPlayers).ifPresent(playerMP.connection::sendPacket);
        }
    }

    @SubscribeEvent
    public void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent event) {
        ITextComponent message = new TextComponentTranslation("multiplayer.player.left", event.player.getDisplayName());
        message.getStyle().setColor(TextFormatting.YELLOW);

        List<EntityPlayerMP> playerList = Lists.newArrayList(server.getPlayerList().getPlayers());
        playerList.remove(event.player);
        sendChatAsServer(playerList, message);
    }

    private Optional<SPacketPlayerListItem> createPlayerListPacket(SPacketPlayerListItem.Action action, Collection<ChatData.PlayerData> changes) {
        if (changes.isEmpty())
            return Optional.empty();
        SPacketPlayerListItem packet = new SPacketPlayerListItem(action);
        List<SPacketPlayerListItem.AddPlayerData> packetPlayers = ObfuscationReflectionHelper.getPrivateValue(SPacketPlayerListItem.class, packet, "field_179769_b");
        packetPlayers.addAll(changes.stream().map(e -> packet.new AddPlayerData(e.profile.toGameProfile(), e.ping, e.gamemode, e.displayName)).collect(Collectors.toList()));
        return Optional.of(packet);
    }

    @SubscribeEvent
    public void onPlayerListUpdate(PlayerListUpdateEvent event) {
        Map<String, ChatData.PlayerData> before = players.computeIfAbsent(event.data.servername, e -> Maps.newHashMap());
        Map<String, ChatData.PlayerData> after = event.data.players.stream().collect(Collectors.toMap(p -> p.profile.name, q -> q));
        MapDifference<String, ChatData.PlayerData> diff = Maps.difference(before, after);
        Map<String, ChatData.PlayerData> removed = diff.entriesOnlyOnLeft();
        Map<String, ChatData.PlayerData> added = diff.entriesOnlyOnRight();
        before.clear();
        before.putAll(after);

        createPlayerListPacket(SPacketPlayerListItem.Action.REMOVE_PLAYER, removed.values()).ifPresent(server.getPlayerList()::sendPacketToAllPlayers);
        createPlayerListPacket(SPacketPlayerListItem.Action.ADD_PLAYER, added.values()).ifPresent(server.getPlayerList()::sendPacketToAllPlayers);
    }

    @SubscribeEvent
    public void onPlayerAdvancementEvent(AdvancementEvent event) {
        Advancement advancement = event.getAdvancement();
        EntityPlayer player = event.getEntityPlayer();
        if (advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceToChat() && event.getEntityPlayer().world.getGameRules().getBoolean("announceAdvancements")) {
            ITextComponent message = new TextComponentTranslation("chat.type.advancement." + advancement.getDisplay().getFrame().getName(), player.getDisplayName(), advancement.getDisplayText());
            sendChatAsServer(server.getPlayerList().getPlayers(), message);
        }
    }

    @SubscribeEvent
    public void onEntityDeathEvent(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        boolean flag = entity.world.getGameRules().getBoolean("showDeathMessages");
        if (flag) {
            if (entity instanceof EntityPlayerMP) {
                Team team = entity.getTeam();
                if (!(team != null && team.getDeathMessageVisibility() != Team.EnumVisible.ALWAYS)) {
                    ITextComponent message = event.getEntityLiving().getCombatTracker().getDeathMessage();
                    sendChatAsServer(server.getPlayerList().getPlayers(), message);
                }
            } else {
                boolean enableLivingEntityDeathMessages = false;
                try {
                    @SuppressWarnings("unchecked")
                    Boolean b = ObfuscationReflectionHelper.getPrivateValue((Class<? super Object>) Class.forName("cofh.core.init.CoreProps"), null, "enableLivingEntityDeathMessages");
                    enableLivingEntityDeathMessages = b;
                } catch (Exception e) {
                    ; // ignore
                }
                if (enableLivingEntityDeathMessages && !entity.world.isRemote && event.getEntityLiving().hasCustomName()) {
                    ITextComponent message = event.getEntityLiving().getCombatTracker().getDeathMessage();
                    sendChatAsServer(server.getPlayerList().getPlayers(), message);
                }
            }
        }
    }

    @SubscribeEvent
    public void onChatSend(ServerChatEvent event) {
        ChatData data = new ChatData();
        data.username = event.getUsername();
        data.content = event.getMessage();
        data.component = event.getComponent();
        sendChat(server.getPlayerList().getPlayers(), data);
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
    private static final Pattern colorPattern = Pattern.compile("(?i)\u00A7([0-9A-FK-OR])");
    @Nonnull
    private static final Pattern pattern = Pattern.compile("<(a?)\\:(\\w+?)\\:([a-zA-Z0-9+/=]+?)>");

    private static String toDecimalId(final String id) {
        try {
            return Long.toString(Base64Utils.decode(id));
        } catch (final IllegalArgumentException e) {
        }
        return id;
    }

    @SubscribeEvent
    public void onColorReplace(NetworkClientChatEvent event) {
        String message = event.data.content;
        message = colorPattern.matcher(message).replaceAll("");
        event.data.content = message;
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
                if (StringUtils.length(g3) <= 12)
                    g3 = toDecimalId(g3);
            matcher.appendReplacement(sb, String.format("<%s:%s:%s>", g1, g2, g3));
        }
        matcher.appendTail(sb);

        event.data.content = sb.toString();
    }
}

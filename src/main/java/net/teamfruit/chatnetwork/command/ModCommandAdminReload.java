package net.teamfruit.chatnetwork.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.teamfruit.chatnetwork.Reference;

import java.io.File;
import java.util.Map;

public class ModCommandAdminReload extends CommandBase {
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cloud admin reload";
    }

    public final ModCommand.Level level = ModCommand.Level.OP.or(ModCommand.Level.SERVER);

    @Override
    public int getRequiredPermissionLevel() {
        return level.requiredPermissionLevel;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return level.permissionChecker.checkPermission(server, sender, this);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        File configDir = Loader.instance().getConfigDir();
        File configFile = new File(configDir, Reference.MODID + ".cfg");
        Map<String, Configuration> CONFIGS = ObfuscationReflectionHelper.getPrivateValue(ConfigManager.class, null, "CONFIGS");
        CONFIGS.remove(configFile.getAbsolutePath());
        ConfigManager.load(Reference.MODID, Config.Type.INSTANCE);

        sender.sendMessage(new TextComponentString("Config Reloaded"));
    }
}

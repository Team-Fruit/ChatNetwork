package net.teamfruit.chatnetwork.command;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.command.CommandTreeHelp;

public class ModCommand extends CommandTreeBase {
    public static class Level {
        public static final Level ALL = new Level(0, (server, sender, command) -> true);
        public static final Level SP = new Level(2, (server, sender, command) -> server.isSinglePlayer());
        public static final Level OP = new Level(2, (server, sender, command) -> sender.canUseCommand(2, command.getName()));
        public static final Level STRONG_OP = new Level(4, (server, sender, command) -> sender.canUseCommand(4, command.getName()));
        public static final Level SERVER = new Level(4, (server, sender, command) -> sender instanceof MinecraftServer);

        public interface PermissionChecker {
            boolean checkPermission(MinecraftServer server, ICommandSender sender, ICommand command);
        }

        public final int requiredPermissionLevel;
        public final PermissionChecker permissionChecker;

        public Level(int l, PermissionChecker p) {
            requiredPermissionLevel = l;
            permissionChecker = p;
        }

        public Level or(Level other) {
            return new Level(requiredPermissionLevel, (server, sender, command)
                    -> permissionChecker.checkPermission(server, sender, command)
                    || other.permissionChecker.checkPermission(server, sender, command));
        }
    }

    public ModCommand() {
        addSubcommand(new ModCommandAdmin());
        addSubcommand(new CommandTreeHelp(this));
    }

    @Override
    public String getName() {
        return "chatnetwork";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/chatnetwork [admin]";
    }

    public final Level level = Level.ALL;

    @Override
    public int getRequiredPermissionLevel() {
        return level.requiredPermissionLevel;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return level.permissionChecker.checkPermission(server, sender, this);
    }
}
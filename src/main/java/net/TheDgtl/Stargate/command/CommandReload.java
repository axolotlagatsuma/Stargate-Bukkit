package net.TheDgtl.Stargate.command;

import net.TheDgtl.Stargate.Stargate;
import net.TheDgtl.Stargate.formatting.TranslatableMessage;
import net.TheDgtl.Stargate.property.CommandPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;

/**
 * This command represents the plugin's reload command
 */
public class CommandReload implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s,
                             @NotNull String[] args) {
        if (!commandSender.hasPermission(CommandPermission.RELOAD.getPermissionNode())) {
            commandSender.sendMessage(Stargate.getLanguageManagerStatic().getErrorMessage(TranslatableMessage.DENY));
            return true;
        }

        Stargate stargate = Stargate.getInstance();
        if (!new File(stargate.getDataFolder(), "config.yml").exists()) {
            stargate.saveDefaultConfig();
        }
        stargate.reloadConfig();
        stargate.reload();
        Stargate.log(Level.INFO, "Reloaded stargate.");
        commandSender.sendMessage(Stargate.getLanguageManagerStatic().getMessage(TranslatableMessage.COMMAND_RELOAD));
        return true;
    }

}

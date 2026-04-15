package com.kurz.savechests.commands;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.chest.SaveChest;
import com.kurz.savechests.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChestCommand implements CommandExecutor {

    private final SaveChests plugin;

    public ChestCommand(SaveChests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Basic info or help command
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                player.sendMessage(plugin.formatMessage("messages.no-chests"));
                return true;
            }
            try {
                UUID chestId = UUID.fromString(args[1]);
                SaveChest chest = plugin.getChestManager().getChest(chestId);
                if (chest == null || !chest.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(plugin.formatMessage("messages.chest-not-found"));
                    return true;
                }

                String time = TimeUtil.formatDuration(chest.getRemainingSeconds());
                String locationStr = String.format("%d, %d, %d in %s", chest.getLocation().getBlockX(), chest.getLocation().getBlockY(), chest.getLocation().getBlockZ(), chest.getLocation().getWorld().getName());
                String ownerName = plugin.getServer().getOfflinePlayer(chest.getOwner()).getName();

                player.sendMessage(plugin.formatMessage("messages.chest-info")
                        .replace("%owner%", ownerName)
                        .replace("%location%", locationStr)
                        .replace("%time%", time));

            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.formatMessage("messages.chest-not-found"));
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            // List player's chests
        } else if (args[0].equalsIgnoreCase("purge")) {
            // Purge player's chests
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("savechests.admin")) {
                plugin.reloadConfig();
                player.sendMessage(plugin.formatMessage("messages.reload-success"));
            }
        } else {
            player.sendMessage("Unknown subcommand.");
        }

        return true;
    }
}

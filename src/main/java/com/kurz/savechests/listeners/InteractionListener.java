package com.kurz.savechests.listeners;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.chest.SaveChest;
import com.kurz.savechests.gui.GuiManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class InteractionListener implements Listener {

    private final SaveChests plugin;

    public InteractionListener(SaveChests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || (clickedBlock.getType() != Material.CHEST && clickedBlock.getType() != Material.OBSIDIAN)) {
            return;
        }

        SaveChest saveChest = plugin.getChestManager().getChestByLocation(clickedBlock.getLocation());
        if (saveChest == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean isOwner = saveChest.getOwner().equals(player.getUniqueId());

        if (player.isSneaking() && isOwner) {
            plugin.getGuiManager().retrieveItems(player, saveChest, true);
        } else {
            plugin.getChestManager().openChest(player, saveChest);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof GuiManager) {
            Player player = (Player) event.getWhoClicked();
            SaveChest saveChest = plugin.getChestManager().getChestByLocation(player.getTargetBlock(null, 5).getLocation());

            if (saveChest == null) {
                return;
            }

            boolean isOwner = saveChest.getOwner().equals(player.getUniqueId());

            if (!isOwner) {
                event.setCancelled(true);
                player.sendMessage(plugin.formatMessage("messages.cannot-take-items"));
                return;
            }
            
            // Allow players to take items from the GUI
            if (event.getRawSlot() < 54) {
                // The actual item retrieval is handled when the inventory is closed or via shift-click
            } else {
                // Prevent moving items in their own inventory
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof GuiManager) {
            // Logic to check if the chest is empty and should be removed
            // For now, we will assume retrieval is handled by shift-click or command
        }
    }
}

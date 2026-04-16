package com.kurz.savechests.listeners;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.chest.SaveChest;
import com.kurz.savechests.gui.GuiManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
        if (clickedBlock == null) {
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
            plugin.getGuiManager().retrieveAllItems(player, saveChest);
        } else {
            plugin.getChestManager().openChest(player, saveChest);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiManager.SaveChestViewHolder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        GuiManager.SaveChestViewHolder holder = (GuiManager.SaveChestViewHolder) event.getInventory().getHolder();
        SaveChest saveChest = holder.getSaveChest();

        boolean isOwner = saveChest.getOwner().equals(player.getUniqueId());

        if (!isOwner && !plugin.getConfig().getBoolean("chest-protection.allow-others-to-view")) {
            player.closeInventory();
            player.sendMessage(plugin.formatMessage("messages.not-your-chest"));
            return;
        }
        
        if (!isOwner && event.getClick() != ClickType.LEFT) { // Allow viewing but not taking
             player.sendMessage(plugin.formatMessage("messages.cannot-take-items"));
             return;
        }

        int clickedSlot = event.getRawSlot();
        int expBottleSlot = plugin.getConfig().getInt("gui.experience-bottle-slot", 4);

        if (clickedSlot == expBottleSlot) {
            plugin.getGuiManager().claimExperience(player, saveChest);
        } else if (clickedSlot < 54) { // Inside the GUI
            plugin.getGuiManager().handleItemRemoval(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiManager.SaveChestViewHolder)) {
            return;
        }

        GuiManager.SaveChestViewHolder holder = (GuiManager.SaveChestViewHolder) event.getInventory().getHolder();
        SaveChest saveChest = holder.getSaveChest();

        if (saveChest != null && saveChest.isEmpty()) {
            plugin.getChestManager().removeChest(saveChest, false);
            Player player = (Player) event.getPlayer();
            player.sendMessage(plugin.formatMessage("messages.chest-retrieved"));
        }
    }
}

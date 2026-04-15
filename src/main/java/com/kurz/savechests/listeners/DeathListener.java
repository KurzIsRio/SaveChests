package com.kurz.savechests.listeners;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.guard.WorldGuardManager;
import com.kurz.savechests.tasks.SafeLocationTask;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeathListener implements Listener {

    private final SaveChests plugin;

    public DeathListener(SaveChests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerInventory inventory = player.getInventory();
        Location deathLocation = player.getLocation();

        // Make defensive copies of all item categories
        List<ItemStack> storageContents = Arrays.stream(inventory.getStorageContents())
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        List<ItemStack> armorContents = Arrays.stream(inventory.getArmorContents())
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        List<ItemStack> offHandContents = Arrays.stream(inventory.getExtraContents()) // ExtraContents includes the off-hand
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        int experienceToSave = event.getDroppedExp();

        if (storageContents.isEmpty() && armorContents.isEmpty() && offHandContents.isEmpty() && experienceToSave == 0) {
            return; // Nothing to save
        }

        // Prevent vanilla drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Clear the player's inventory slots after we have copied the items
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);

        // Find a safe location asynchronously to avoid server lag
        new SafeLocationTask(plugin, deathLocation, (safeLocation) -> {
            WorldGuardManager wgManager = plugin.getWorldGuardManager();
            boolean canPlaceChest = safeLocation != null && (wgManager == null || wgManager.canPlaceChest(safeLocation));

            if (canPlaceChest) {
                plugin.getChestManager().createChest(player, safeLocation, storageContents, armorContents, offHandContents, experienceToSave);
                player.sendMessage(plugin.formatMessage("messages.chest-created"));
            } else {
                // If no safe spot is found, drop items at the original death location
                dropItemsManually(deathLocation, storageContents, armorContents, offHandContents, experienceToSave);
                player.sendMessage(plugin.formatMessage("messages.chest-failed-location"));
            }
        }).runTaskAsynchronously(plugin);
    }

    @SafeVarargs
    private final void dropItemsManually(Location location, List<ItemStack>... itemLists) {
        for (List<ItemStack> itemList : itemLists) {
            for (ItemStack item : itemList) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }
    
    private void dropItemsManually(Location location, List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> offhand, int experience) {
        dropItemsManually(location, storage, armor, offhand);
        if (experience > 0) {
            location.getWorld().spawn(location, ExperienceOrb.class, orb -> orb.setExperience(experience));
        }
    }
}

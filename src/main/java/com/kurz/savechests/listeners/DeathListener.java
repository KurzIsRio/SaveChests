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

        List<ItemStack> storageContents = Arrays.stream(inventory.getStorageContents())
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        List<ItemStack> armorContents = Arrays.stream(inventory.getArmorContents())
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        List<ItemStack> offHandContents = Arrays.stream(inventory.getExtraContents())
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        int expPercentage = plugin.getConfig().getInt("chest-settings.exp-save-percentage", 100);
        int experienceToSave = (int) (event.getDroppedExp() * (expPercentage / 100.0));
        int expToDrop = event.getDroppedExp() - experienceToSave;

        if (storageContents.isEmpty() && armorContents.isEmpty() && offHandContents.isEmpty() && experienceToSave == 0) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        inventory.clear();

        new SafeLocationTask(plugin, deathLocation, (safeLocation) -> {
            if (safeLocation != null) {
                plugin.getChestManager().createChest(player, safeLocation, storageContents, armorContents, offHandContents, experienceToSave);
                if (expToDrop > 0) {
                    dropExperience(safeLocation, expToDrop);
                }
            } else {
                dropItemsManually(deathLocation, storageContents, armorContents, offHandContents, event.getDroppedExp());
                player.sendMessage(plugin.formatMessage("messages.chest-failed-location"));
            }
        }).runTaskAsynchronously(plugin);
    }

    private void dropItemsManually(Location location, List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> offhand, int experience) {
        storage.forEach(item -> location.getWorld().dropItemNaturally(location, item));
        armor.forEach(item -> location.getWorld().dropItemNaturally(location, item));
        offhand.forEach(item -> location.getWorld().dropItemNaturally(location, item));
        if (experience > 0) {
            dropExperience(location, experience);
        }
    }

    private void dropExperience(Location location, int experience) {
        if (experience > 0) {
            location.getWorld().spawn(location, ExperienceOrb.class, orb -> orb.setExperience(experience));
        }
    }
}

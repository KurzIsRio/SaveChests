package com.kurz.savechests;

import com.google.gson.reflect.TypeToken;
import com.kurz.savechests.chest.SaveChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChestManager {

    private final SaveChests plugin;
    private final Map<UUID, SaveChest> chests = new ConcurrentHashMap<>();
    private final File chestsFile;
    private final Type chestMapType = new TypeToken<Map<UUID, SaveChest>>() {}.getType();
    private BukkitTask expirationTask;
    private BukkitTask saveTask;

    public ChestManager(SaveChests plugin) {
        this.plugin = plugin;
        this.chestsFile = new File(plugin.getDataFolder(), "chests.json");
        loadChests();
        startExpirationTask();
        startAutoSaveTask();
    }

    public void createChest(Player player, Location location, List<ItemStack> storageContents, List<ItemStack> armorContents, List<ItemStack> offHandContents, int experience) {
        long duration = plugin.getConfig().getLong("chest-settings.duration-seconds", 3600);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Material chestMaterial;
            try {
                chestMaterial = Material.valueOf(plugin.getConfig().getString("chest-settings.chest-material", "CHEST").toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid chest-material in config.yml. Defaulting to CHEST.");
                chestMaterial = Material.CHEST;
            }
            location.getBlock().setType(chestMaterial);

            SaveChest chest = new SaveChest(player.getUniqueId(), location, storageContents, armorContents, offHandContents, experience, duration);
            chests.put(chest.getId(), chest);

            // Correctly pass the OfflinePlayer object
            plugin.getHologramManager().createHologram(chest, plugin.getServer().getOfflinePlayer(player.getUniqueId()));

            plugin.getLogger().info("Created SaveChest for " + player.getName() + " at " + location);
        });
    }

    public void removeChest(SaveChest chest, boolean save) {
        if (chest == null) return;
        plugin.getHologramManager().removeHologram(chest);
        chests.remove(chest.getId());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (chest.getLocation().getBlock().getType().name().equals(plugin.getConfig().getString("chest-settings.chest-material", "CHEST"))) {
                chest.getLocation().getBlock().setType(Material.AIR);
            }
        });
        if (save) {
            saveAllChests();
        }
    }

    public void removeChest(UUID chestId, boolean save) {
        SaveChest chest = getChest(chestId);
        if (chest != null) {
            removeChest(chest, save);
        }
    }

    public SaveChest getChest(UUID id) {
        return chests.get(id);
    }

    public SaveChest getChestAt(Location location) {
        for (SaveChest chest : chests.values()) {
            if (chest.getLocation().getWorld().equals(location.getWorld()) && chest.getLocation().distanceSquared(location) < 1) {
                return chest;
            }
        }
        return null;
    }

    public List<SaveChest> getPlayerChests(UUID uuid) {
        return chests.values().stream()
                .filter(chest -> chest.getOwner().equals(uuid))
                .collect(Collectors.toList());
    }

    public void autoClaim(Player player, SaveChest chest) {
        if (!player.getUniqueId().equals(chest.getOwner())) {
            player.sendMessage(plugin.formatMessage("messages.not-your-chest"));
            return;
        }

        PlayerInventory inv = player.getInventory();
        List<ItemStack> remainingStorage = giveItems(inv, chest.getStorageContents());
        List<ItemStack> remainingArmor = giveArmor(inv, chest.getArmorContents());
        List<ItemStack> remainingOffHand = giveOffHand(inv, chest.getOffHandContents());

        chest.updateContents(remainingStorage, remainingArmor, remainingOffHand);

        if (chest.isEmpty()) {
            removeChest(chest, true);
            player.sendMessage(plugin.formatMessage("messages.chest-retrieved"));
            if (chest.getExperience() > 0) {
                player.giveExp(chest.getExperience());
                player.sendMessage("You recovered " + chest.getExperience() + " experience points.");
            }
        } else {
            player.sendMessage(plugin.formatMessage("messages.inventory-full-dropped"));
            saveAllChests();
        }
    }

    private List<ItemStack> giveItems(PlayerInventory inv, List<ItemStack> items) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> couldNotFit = inv.addItem(item.clone());
            if (!couldNotFit.isEmpty()) {
                remaining.addAll(couldNotFit.values());
            }
        }
        return remaining;
    }

    private List<ItemStack> giveArmor(PlayerInventory inv, List<ItemStack> armor) {
        List<ItemStack> remaining = new ArrayList<>();
        ItemStack[] armorContents = inv.getArmorContents();
        for (int i = 0; i < armor.size(); i++) {
            ItemStack item = armor.get(i);
            if (item == null) continue;

            int armorSlot = 3 - i;
            if (armorContents[armorSlot] == null || armorContents[armorSlot].getType() == Material.AIR) {
                armorContents[armorSlot] = item.clone();
            } else {
                remaining.add(item);
            }
        }
        inv.setArmorContents(armorContents);
        return giveItems(inv, remaining);
    }

    private List<ItemStack> giveOffHand(PlayerInventory inv, List<ItemStack> offHandItems) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : offHandItems) {
            if (inv.getItemInOffHand().getType() == Material.AIR) {
                inv.setItemInOffHand(item.clone());
            } else {
                remaining.add(item);
            }
        }
        return giveItems(inv, remaining);
    }

    public void saveAllChests() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAllChestsSync);
    }

    public void saveAllChestsSync() {
        try (FileWriter writer = new FileWriter(chestsFile)) {
            plugin.getGson().toJson(chests, chestMapType, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save chests to chests.json: " + e.getMessage());
        }
    }

    private void loadChests() {
        if (!chestsFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(chestsFile)) {
            Map<UUID, SaveChest> loadedChests = plugin.getGson().fromJson(reader, chestMapType);
            if (loadedChests != null && !loadedChests.isEmpty()) {
                chests.putAll(loadedChests);
                plugin.getLogger().info("Loaded " + chests.size() + " chests.");

                for (SaveChest chest : chests.values()) {
                    if (chest.isExpired()) {
                        removeChest(chest, false);
                    } else {
                        plugin.getHologramManager().createHologram(chest, plugin.getServer().getOfflinePlayer(chest.getOwner()));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load chests from chests.json: " + e.getMessage());
        }
    }

    private void startExpirationTask() {
        expirationTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (SaveChest chest : chests.values()) {
                if (chest.isExpired()) {
                    removeChest(chest, true);
                }
            }
        }, 100L, 100L);
    }

    private void startAutoSaveTask() {
        long saveInterval = plugin.getConfig().getLong("chest-settings.auto-save-interval-seconds", 300) * 20;
        saveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllChests, saveInterval, saveInterval);
    }

    public void shutdown() {
        if (expirationTask != null) expirationTask.cancel();
        if (saveTask != null) saveTask.cancel();
        plugin.getHologramManager().removeAllHolograms();
        saveAllChestsSync();
        plugin.getLogger().info("Saved " + chests.size() + " chests during shutdown.");
    }
}

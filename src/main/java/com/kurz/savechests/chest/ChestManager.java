package com.kurz.savechests.chest;

import com.google.gson.reflect.TypeToken;
import com.kurz.savechests.SaveChests;
import com.kurz.savechests.gui.GuiManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChestManager {

    private final SaveChests plugin;
    private final GuiManager guiManager;
    private final Map<UUID, SaveChest> activeChests = new ConcurrentHashMap<>();
    private final File dataFile;

    public ChestManager(SaveChests plugin) {
        this.plugin = plugin;
        this.guiManager = new GuiManager(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "chests.json");
        loadChests();
    }

    public void createChest(Player player, Location location, List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> offhand, int experience) {
        // This is the line that had the error. The final 'long' argument has been removed.
        SaveChest saveChest = new SaveChest(player.getUniqueId(), location, storage, armor, offhand, experience);
        activeChests.put(saveChest.getId(), saveChest);

        // Set block to chest, or obsidian if in lava
        Material chestMaterial = (location.getBlock().getType() == Material.LAVA) ? Material.OBSIDIAN : Material.CHEST;
        location.getBlock().setType(chestMaterial);

        plugin.getHologramManager().createHologram(saveChest, player);
        
        // Send the location message
        String worldName = location.getWorld().getName();
        String message = plugin.getConfig().getString("messages.chest-created-location", "")
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()))
                .replace("%world%", worldName);
        player.sendMessage(plugin.getMiniMessage().deserialize(message));
    }

    public void openChest(Player player, SaveChest saveChest) {
        boolean canView = plugin.getConfig().getBoolean("chest-protection.allow-others-to-view", false);

        if (!saveChest.getOwner().equals(player.getUniqueId()) && !canView) {
            player.sendMessage(plugin.formatMessage("messages.not-your-chest"));
            return;
        }

        player.openInventory(guiManager.createChestGui(saveChest));
    }

    public void removeChest(SaveChest saveChest, boolean dropItems) {
        if (saveChest == null) return;

        plugin.getHologramManager().removeHologram(saveChest);
        saveChest.getLocation().getBlock().setType(Material.AIR);
        activeChests.remove(saveChest.getId());

        if (dropItems) {
            // Logic to drop items if needed
        }
    }

    public SaveChest getChest(UUID chestId) {
        return activeChests.get(chestId);
    }
    
    public SaveChest getChestByLocation(Location location) {
        for (SaveChest chest : activeChests.values()) {
            if (chest.getLocation().equals(location)) {
                return chest;
            }
        }
        return null;
    }

    public Map<UUID, SaveChest> getActiveChests() {
        return activeChests;
    }

    public void shutdown() {
        saveChests();
        plugin.getHologramManager().removeAllHolograms();
    }

    public void saveChests() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            plugin.getGson().toJson(activeChests, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chests to file: " + e.getMessage());
        }
    }

    private void loadChests() {
        if (!dataFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, SaveChest>>() {}.getType();
            Map<UUID, SaveChest> loadedChests = plugin.getGson().fromJson(reader, type);
            if (loadedChests != null) {
                activeChests.putAll(loadedChests);
                plugin.getLogger().info("Loaded " + activeChests.size() + " chests from file.");
                // Re-initialize holograms for loaded chests
                for (SaveChest chest : activeChests.values()) {
                    OfflinePlayer owner = plugin.getServer().getOfflinePlayer(chest.getOwner());
                    plugin.getHologramManager().createHologram(chest, owner);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load chests from file: " + e.getMessage());
        }
    }
}

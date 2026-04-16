package com.kurz.savechests.chest;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.reflect.TypeToken;
import com.kurz.savechests.SaveChests;
import com.kurz.savechests.gui.GuiManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        long duration = plugin.getConfig().getLong("chest-settings.duration-seconds", 3600);
        double xpPercentage = plugin.getConfig().getDouble("chest-settings.exp-save-percentage", 100) / 100.0;
        int storedExperience = (int) (experience * xpPercentage);

        SaveChest saveChest = new SaveChest(player.getUniqueId(), location, storage, armor, offhand, storedExperience, duration);
        activeChests.put(saveChest.getId(), saveChest);

        String blockTypeString = plugin.getConfig().getString("chest-settings.spawn-block-type", "CHEST");
        Material chestMaterial = getChestMaterial(blockTypeString);

        Block block = location.getBlock();
        block.setType(chestMaterial);

        if (block.getState() instanceof Skull) {
            setPlayerSkull(player, (Skull) block.getState());
        }

        plugin.getHologramManager().createHologram(saveChest, player);
        sendCreationMessage(player, location);
    }

    private void setPlayerSkull(Player player, Skull skull) {
        if (plugin.isSkinsRestorerEnabled()) {
            try {
                net.skinsrestorer.api.SkinsRestorer api = net.skinsrestorer.api.SkinsRestorerProvider.get();
                Optional<net.skinsrestorer.api.property.SkinProperty> skin = api.getPlayerStorage().getSkinForPlayer(player.getUniqueId(), player.getName());
                if (skin.isPresent()) {
                    String textureValue = skin.get().getValue();
                    PlayerProfile profile = player.getPlayerProfile();
                    PlayerTextures textures = profile.getTextures();
                    try {
                        textures.setSkin(new URL("http://textures.minecraft.net/texture/" + textureValue));
                        profile.setTextures(textures);
                        skull.setPlayerProfile(profile);
                    } catch (MalformedURLException e) {
                        plugin.getLogger().warning("Malformed URL from SkinsRestorer, skipping skin set.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("An error occurred with the SkinsRestorer hook, skipping skin set.");
            }
        } else {
            skull.setOwningPlayer(player);
        }
        skull.update(true);
    }

    private void sendCreationMessage(Player player, Location location) {
        String worldName = location.getWorld().getName();
        String message = plugin.getConfig().getString("messages.chest-created-location", "")
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()))
                .replace("%world%", worldName);
        player.sendMessage(plugin.getMiniMessage().deserialize(message));
    }

    private Material getChestMaterial(String blockTypeString) {
        try {
            return Material.valueOf(blockTypeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid spawn-block-type in config.yml: " + blockTypeString + ". Defaulting to CHEST.");
            return Material.CHEST;
        }
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
            List<ItemStack> allItems = new ArrayList<>();
            if(saveChest.getStorageContents() != null) allItems.addAll(saveChest.getStorageContents());
            if(saveChest.getArmorContents() != null) allItems.addAll(saveChest.getArmorContents());
            if(saveChest.getExtraContents() != null) allItems.addAll(saveChest.getExtraContents());

            allItems.forEach(item -> saveChest.getLocation().getWorld().dropItemNaturally(saveChest.getLocation(), item));
        }
    }

    public SaveChest getChest(UUID chestId) {
        return activeChests.get(chestId);
    }

    public SaveChest getChestByLocation(Location location) {
        for (SaveChest chest : activeChests.values()) {
            if (chest.getLocation().getBlock().getLocation().equals(location)) {
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
                for (SaveChest chest : activeChests.values()) {
                    OfflinePlayer owner = plugin.getServer().getOfflinePlayer(chest.getOwner());
                    plugin.getHologramManager().createHologram(chest, owner);
                }
            }
        } catch (FileNotFoundException e) {
            plugin.getLogger().severe("Could not find chests.json: " + e.getMessage());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load chests from file: " + e.getMessage());
        }
    }
}

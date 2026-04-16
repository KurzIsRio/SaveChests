package com.kurz.savechests.hologram;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.chest.SaveChest;
import com.kurz.savechests.utils.TimeUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final SaveChests plugin;
    private final Map<UUID, TextDisplay> activeHolograms = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public HologramManager(SaveChests plugin) {
        this.plugin = plugin;
    }

    public void createHologram(SaveChest saveChest, OfflinePlayer player) {
        if (saveChest.getLocation().getWorld() == null) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            double heightOffset = plugin.getConfig().getDouble("hologram.height-offset", 1.5);
            Location hologramLocation = saveChest.getLocation().getBlock().getLocation().add(0.5, heightOffset, 0.5);

            TextDisplay textDisplay = (TextDisplay) saveChest.getLocation().getWorld().spawnEntity(hologramLocation, EntityType.TEXT_DISPLAY);

            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setSeeThrough(true);
            textDisplay.setShadowed(true);

            saveChest.setHologramUuid(textDisplay.getUniqueId());
            activeHolograms.put(saveChest.getId(), textDisplay);

            updateHologramText(saveChest);
        });

        if (updateTask == null || updateTask.isCancelled()) {
            startHologramUpdateTask();
        }
    }

    public void removeHologram(SaveChest saveChest) {
        if (saveChest == null) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            TextDisplay textDisplay = activeHolograms.remove(saveChest.getId());
            if (textDisplay != null && !textDisplay.isDead()) {
                textDisplay.remove();
            } else if (saveChest.getHologramUuid() != null) {
                org.bukkit.entity.Entity entity = Bukkit.getServer().getEntity(saveChest.getHologramUuid());
                if (entity instanceof TextDisplay && !entity.isDead()) {
                    entity.remove();
                }
            }
        });
    }

    public void removeAllHolograms() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (TextDisplay textDisplay : activeHolograms.values()) {
                if (textDisplay != null && !textDisplay.isDead()) {
                    textDisplay.remove();
                }
            }
            activeHolograms.clear();
        });
    }

    private void startHologramUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeHolograms.isEmpty()) {
                    this.cancel();
                    updateTask = null;
                    return;
                }
                for (UUID chestId : activeHolograms.keySet()) {
                    SaveChest chest = plugin.getChestManager().getChest(chestId);
                    if (chest == null || chest.isExpired()) {
                        removeHologram(chest);
                        continue;
                    }
                    updateHologramText(chest);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    private void updateHologramText(SaveChest saveChest) {
        TextDisplay textDisplay = activeHolograms.get(saveChest.getId());
        if (textDisplay == null || textDisplay.isDead()) {
            activeHolograms.remove(saveChest.getId());
            return;
        }

        long remainingSeconds = saveChest.getRemainingSeconds();
        String formattedTime = TimeUtil.formatDuration(remainingSeconds);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(saveChest.getOwner());
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

        List<String> formatLines = plugin.getConfig().getStringList("hologram.lines");
        Component fullHologramText = Component.empty();

        for (int i = 0; i < formatLines.size(); i++) {
            String line = formatLines.get(i);
            line = line.replace("%player_name%", ownerName);
            line = line.replace("%time%", formattedTime);
            line = line.replace("%experience%", String.valueOf(saveChest.getExperience()));

            if (plugin.isPapiEnabled()) {
                line = PlaceholderAPI.setPlaceholders(owner, line);
            }

            fullHologramText = fullHologramText.append(plugin.getMiniMessage().deserialize(line));
            if (i < formatLines.size() - 1) {
                fullHologramText = fullHologramText.append(Component.newline());
            }
        }

        final Component finalComponent = fullHologramText;
        plugin.getServer().getScheduler().runTask(plugin, () -> textDisplay.text(finalComponent));
    }
}

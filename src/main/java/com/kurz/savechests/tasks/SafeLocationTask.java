package com.kurz.savechests.tasks;

import com.kurz.savechests.SaveChests;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class SafeLocationTask extends BukkitRunnable {

    private final SaveChests plugin;
    private final Location origin;
    private final Consumer<Location> callback;

    public SafeLocationTask(SaveChests plugin, Location origin, Consumer<Location> callback) {
        this.plugin = plugin;
        this.origin = origin.clone();
        this.callback = callback;
    }

    @Override
    public void run() {
        Location safeLocation = findSafeLocation();

        // Run the callback on the main server thread
        new BukkitRunnable() {
            @Override
            public void run() {
                callback.accept(safeLocation);
            }
        }.runTask(plugin);
    }

    private Location findSafeLocation() {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }

        // --- Void Save Logic ---
        // If player died below the world's minimum height, start search from the configured Y-level.
        int startY = origin.getBlockY();
        if (startY < world.getMinHeight()) {
            startY = plugin.getConfig().getInt("environment-logic.void-save-y-level", 0);
        }

        int searchRadius = plugin.getConfig().getInt("environment-logic.void-search-radius", 10);

        // Search in a spiral pattern outwards from the starting point
        for (int radius = 0; radius <= searchRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only check blocks on the perimeter of the current radius to form a spiral
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    Location checkLoc = new Location(world, origin.getBlockX() + x, startY, origin.getBlockZ() + z);

                    // Attempt to find a valid spot by searching vertically from the check location
                    for (int y = startY; y < world.getMaxHeight() && y > world.getMinHeight(); y++) {
                        Location finalLoc = checkLoc.clone();
                        finalLoc.setY(y);
                        if (isSafe(finalLoc)) {
                            return finalLoc;
                        }
                    }
                    // Also check downwards briefly, in case startY is above ground
                    for (int y = startY - 1; y > world.getMinHeight() && y > startY - 10; y--) {
                        Location finalLoc = checkLoc.clone();
                        finalLoc.setY(y);
                        if (isSafe(finalLoc)) {
                            return finalLoc;
                        }
                    }
                }
            }
        }

        // As a last resort, check the original death location if all else fails
        if (isSafe(origin)) {
            return origin;
        }

        return null; // No safe location found
    }

    private boolean isSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        // Lava check for float feature
        boolean isLavaFloatEnabled = plugin.getConfig().getBoolean("environment-logic.lava-float-enabled", true);
        if (isLavaFloatEnabled && location.getBlock().getType() == Material.LAVA) {
            return true; // Lava is considered a "safe" floating spot
        }

        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);

        // The space for the chest must be empty (air)
        boolean isSpaceEmpty = block.getType().isAir() || block.getType() == Material.WATER;
        // The block below must be solid to support the chest
        boolean isGroundSolid = below.getType().isSolid() && below.getType().isOccluding();

        return isSpaceEmpty && isGroundSolid;
    }
}

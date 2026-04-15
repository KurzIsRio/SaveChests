package com.kurz.savechests.guard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

public class WorldGuardManager {

    public static StateFlag ALLOW_SAVECHESTS;

    public void registerFlag() {
        ALLOW_SAVECHESTS = new StateFlag("allow-savechests", true);
        WorldGuard.getInstance().getFlagRegistry().register(ALLOW_SAVECHESTS);
    }

    public boolean canPlaceChest(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regions == null) return true; // No region manager, so allow

        return regions.getApplicableRegions(BukkitAdapter.asBlockVector(location)).testState(null, ALLOW_SAVECHESTS);
    }
}

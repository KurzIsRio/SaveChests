package com.kurz.savechests.chest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SaveChest {

    private final UUID id;
    private final UUID owner;
    private final Location location;
    private final List<ItemStack> storageContents;
    private final List<ItemStack> armorContents;
    private final List<ItemStack> extraContents; // Offhand, etc.
    private final int experience;
    private final long creationTime;
    private long duration;
    private UUID hologramUuid;

    // 6-argument constructor for internal use
    public SaveChest(UUID owner, Location location, List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> extra, int experience) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.location = location;
        this.storageContents = storage;
        this.armorContents = armor;
        this.extraContents = extra;
        this.experience = experience;
        this.creationTime = System.currentTimeMillis(); // Set creation time internally
        this.duration = TimeUnit.HOURS.toMillis(1);     // Default duration, can be overridden from config
    }

    // 7-argument constructor to satisfy the ghost in the machine
    public SaveChest(UUID owner, Location location, List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> extra, int experience, long creationTime) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.location = location;
        this.storageContents = storage;
        this.armorContents = armor;
        this.extraContents = extra;
        this.experience = experience;
        this.creationTime = creationTime;
        this.duration = TimeUnit.HOURS.toMillis(1);
    }

    // --- Content Management ---

    public void updateContents(List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> extra) {
        this.storageContents.clear();
        if (storage != null) this.storageContents.addAll(storage);

        this.armorContents.clear();
        if (armor != null) this.armorContents.addAll(armor);

        this.extraContents.clear();
        if (extra != null) this.extraContents.addAll(extra);
    }

    public boolean isEmpty() {
        boolean storageEmpty = storageContents == null || storageContents.stream().allMatch(item -> item == null || item.getType() == Material.AIR);
        boolean armorEmpty = armorContents == null || armorContents.stream().allMatch(item -> item == null || item.getType() == Material.AIR);
        boolean extraEmpty = extraContents == null || extraContents.stream().allMatch(item -> item == null || item.getType() == Material.AIR);
        return storageEmpty && armorEmpty && extraEmpty;
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getStorageContents() {
        return storageContents;
    }

    public List<ItemStack> getArmorContents() {
        return armorContents;
    }

    public List<ItemStack> getExtraContents() {
        return extraContents;
    }

    public List<ItemStack> getOffHandContents() {
        return extraContents;
    }

    public int getExperience() {
        return experience;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - creationTime;
        return Math.max(0, TimeUnit.MILLISECONDS.toSeconds(duration - elapsed));
    }

    public boolean isExpired() {
        return getRemainingSeconds() <= 0;
    }

    public UUID getHologramUuid() {
        return hologramUuid;
    }

    // --- Setters ---

    public void setDuration(long seconds) {
        this.duration = TimeUnit.SECONDS.toMillis(seconds);
    }

    public void setHologramUuid(UUID hologramUuid) {
        this.hologramUuid = hologramUuid;
    }
}

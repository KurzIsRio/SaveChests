package com.kurz.savechests.chest;

import com.google.gson.annotations.Expose;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SaveChest {

    @Expose
    private final UUID id;
    @Expose
    private final UUID owner;
    @Expose
    private final Location location;
    @Expose
    private final List<ItemStack> storageContents;
    @Expose
    private final List<ItemStack> armorContents;
    @Expose
    private final List<ItemStack> extraContents; // Offhand, etc.
    @Expose
    private final int experience;
    @Expose
    private final long creationTime;
    @Expose
    private long duration;

    private UUID hologramUuid;

    public SaveChest(UUID owner, Location location, List<ItemStack> storage, List<ItemStack> armor, List<ItemStack> extra, int experience, long durationSeconds) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.location = location;
        this.storageContents = storage;
        this.armorContents = armor;
        this.extraContents = extra;
        this.experience = experience;
        this.creationTime = System.currentTimeMillis();
        this.duration = TimeUnit.SECONDS.toMillis(durationSeconds);
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

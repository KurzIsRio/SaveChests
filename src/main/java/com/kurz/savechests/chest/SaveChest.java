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
    private List<ItemStack> storageContents;
    @Expose
    private List<ItemStack> armorContents;
    @Expose
    private List<ItemStack> extraContents;
    @Expose
    private int experience;
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

    public boolean isEmpty() {
        return (storageContents == null || storageContents.stream().allMatch(item -> item == null || item.getType() == Material.AIR)) &&
               (armorContents == null || armorContents.stream().allMatch(item -> item == null || item.getType() == Material.AIR)) &&
               (extraContents == null || extraContents.stream().allMatch(item -> item == null || item.getType() == Material.AIR)) &&
               experience <= 0;
    }

    public void clearContents() {
        if (storageContents != null) storageContents.clear();
        if (armorContents != null) armorContents.clear();
        if (extraContents != null) extraContents.clear();
    }

    // --- Getters ---
    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public Location getLocation() { return location; }
    public List<ItemStack> getStorageContents() { return storageContents; }
    public List<ItemStack> getArmorContents() { return armorContents; }
    public List<ItemStack> getExtraContents() { return extraContents; }
    public int getExperience() { return experience; }
    public UUID getHologramUuid() { return hologramUuid; }

    public long getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - creationTime;
        long remainingMillis = duration - elapsed;
        return Math.max(0, TimeUnit.MILLISECONDS.toSeconds(remainingMillis));
    }

    public boolean isExpired() {
        return getRemainingSeconds() <= 0;
    }

    // --- Setters ---
    public void setHologramUuid(UUID hologramUuid) {
        this.hologramUuid = hologramUuid;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public void setStorageContents(List<ItemStack> storageContents) {
        this.storageContents = storageContents;
    }
}

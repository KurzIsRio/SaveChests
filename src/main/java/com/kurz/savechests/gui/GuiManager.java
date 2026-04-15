package com.kurz.savechests.gui;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.chest.SaveChest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GuiManager implements InventoryHolder {

    private final SaveChests plugin;
    private final Material glassPaneMaterial;
    private final Set<UUID> messagedPlayers = new HashSet<>();

    public GuiManager(SaveChests plugin) {
        this.plugin = plugin;
        this.glassPaneMaterial = Material.matchMaterial(plugin.getConfig().getString("gui.glass-pane-color", "GRAY_STAINED_GLASS_PANE"));
    }

    @Override
    public Inventory getInventory() {
        // This is required for the InventoryHolder implementation
        return null;
    }

    public Inventory createChestGui(SaveChest saveChest) {
        Inventory gui = Bukkit.createInventory(this, 54, plugin.getMiniMessage().deserialize("<dark_gray>SaveChest</dark_gray>"));

        ItemStack glassPane = new ItemStack(glassPaneMaterial);
        for (int i = 36; i < 45; i++) {
            gui.setItem(i, glassPane);
        }

        int storageSlot = 0;
        for (ItemStack item : saveChest.getStorageContents()) {
            if (storageSlot < 36) {
                gui.setItem(storageSlot++, item);
            }
        }

        List<ItemStack> armor = saveChest.getArmorContents();
        if (armor.size() > 0) gui.setItem(45, armor.get(3));
        if (armor.size() > 1) gui.setItem(46, armor.get(2));
        if (armor.size() > 2) gui.setItem(47, armor.get(1));
        if (armor.size() > 3) gui.setItem(48, armor.get(0));

        List<ItemStack> offhand = saveChest.getExtraContents();
        if (!offhand.isEmpty()) {
            gui.setItem(53, offhand.get(0));
        }

        return gui;
    }

    public void retrieveItems(Player player, SaveChest saveChest, boolean isShiftClick) {
        List<ItemStack> leftoverItems = new ArrayList<>();
        messagedPlayers.remove(player.getUniqueId()); // Clear previous message status

        if (isShiftClick) {
            autoEquipItems(player, saveChest, leftoverItems);
        } else {
            leftoverItems.addAll(saveChest.getStorageContents());
            leftoverItems.addAll(saveChest.getArmorContents());
            leftoverItems.addAll(saveChest.getExtraContents());
        }

        for (ItemStack item : leftoverItems) {
            if (item != null) {
                 if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    if (!messagedPlayers.contains(player.getUniqueId())) {
                        player.sendMessage(plugin.formatMessage("messages.inventory-full-dropped"));
                        messagedPlayers.add(player.getUniqueId());
                    }
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }

        if (saveChest.getExperience() > 0) {
            player.giveExp(saveChest.getExperience());
        }

        plugin.getChestManager().removeChest(saveChest, false);
        player.sendMessage(plugin.formatMessage("messages.chest-retrieved"));
    }

    private void autoEquipItems(Player player, SaveChest saveChest, List<ItemStack> leftoverItems) {
        leftoverItems.addAll(saveChest.getStorageContents());
        
        List<ItemStack> armor = new ArrayList<>(saveChest.getArmorContents());
        equipOrAdd(player.getInventory().getBoots(), player.getInventory()::setBoots, armor, 3, leftoverItems, player);
        equipOrAdd(player.getInventory().getLeggings(), player.getInventory()::setLeggings, armor, 2, leftoverItems, player);
        equipOrAdd(player.getInventory().getChestplate(), player.getInventory()::setChestplate, armor, 1, leftoverItems, player);
        equipOrAdd(player.getInventory().getHelmet(), player.getInventory()::setHelmet, armor, 0, leftoverItems, player);

        List<ItemStack> offhand = saveChest.getExtraContents();
        if (!offhand.isEmpty() && offhand.get(0) != null) {
            if (player.getInventory().getItemInOffHand().getType() == Material.AIR) {
                player.getInventory().setItemInOffHand(offhand.get(0));
            } else {
                leftoverItems.add(offhand.get(0));
            }
        }
    }

    private void equipOrAdd(ItemStack currentArmor, java.util.function.Consumer<ItemStack> equipSlot, List<ItemStack> savedArmor, int index, List<ItemStack> leftovers, Player player) {
        if (index < savedArmor.size() && savedArmor.get(index) != null) {
            ItemStack itemToEquip = savedArmor.get(index);
            if (currentArmor == null || currentArmor.getType() == Material.AIR) {
                equipSlot.accept(itemToEquip);
            } else {
                leftovers.add(itemToEquip);
            }
        }
    }
}

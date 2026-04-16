package com.kurz.savechests.gui;

import com.kurz.savechests.SaveChests;
import com.kurz.savechests.chest.SaveChest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GuiManager {

    private final SaveChests plugin;
    private final Material glassPaneMaterial;
    private final int experienceBottleSlot;

    public GuiManager(SaveChests plugin) {
        this.plugin = plugin;
        String paneColor = plugin.getConfig().getString("gui.glass-pane-color", "GRAY_STAINED_GLASS_PANE");
        this.glassPaneMaterial = Optional.ofNullable(Material.matchMaterial(paneColor)).orElse(Material.GRAY_STAINED_GLASS_PANE);
        this.experienceBottleSlot = plugin.getConfig().getInt("gui.experience-bottle-slot", 4);
    }

    public Inventory createChestGui(SaveChest saveChest) {
        Inventory gui = Bukkit.createInventory(new SaveChestViewHolder(saveChest), 54, plugin.getMiniMessage().deserialize("<dark_gray>SaveChest</dark_gray>"));

        ItemStack glassPane = new ItemStack(glassPaneMaterial);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassPane.setItemMeta(glassMeta);

        for (int i = 36; i < 45; i++) {
            gui.setItem(i, glassPane);
        }

        if (saveChest.getExperience() > 0) {
            ItemStack expBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta meta = expBottle.getItemMeta();
            meta.displayName(plugin.getMiniMessage().deserialize("<gold>Click to claim your experience</gold>"));
            meta.lore(Collections.singletonList(plugin.getMiniMessage().deserialize("<gray>Levels: <white>" + saveChest.getExperience() + "</white></gray>")));
            expBottle.setItemMeta(meta);
            gui.setItem(experienceBottleSlot, expBottle);
        }

        setGuiContents(gui, saveChest);
        return gui;
    }

    private void setGuiContents(Inventory gui, SaveChest saveChest) {
        gui.clear(); // Clear current items before setting new ones

        // Re-add glass panes and other decorative items
        ItemStack glassPane = new ItemStack(glassPaneMaterial);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassPane.setItemMeta(glassMeta);
        for (int i = 36; i < 45; i++) {
            gui.setItem(i, glassPane);
        }

        int storageSlot = 0;
        if (saveChest.getStorageContents() != null) {
            for (ItemStack item : saveChest.getStorageContents()) {
                if (storageSlot < 36) gui.setItem(storageSlot++, item);
            }
        }

        List<ItemStack> armor = saveChest.getArmorContents();
        if (armor != null) {
            if (armor.size() > 3) gui.setItem(45, armor.get(3)); // Helmet
            if (armor.size() > 2) gui.setItem(46, armor.get(2)); // Chestplate
            if (armor.size() > 1) gui.setItem(47, armor.get(1)); // Leggings
            if (armor.size() > 0) gui.setItem(48, armor.get(0)); // Boots
        }

        List<ItemStack> offhand = saveChest.getExtraContents();
        if (offhand != null && !offhand.isEmpty()) {
            gui.setItem(53, offhand.get(0));
        }
    }

    public void handleItemRemoval(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SaveChestViewHolder)) return;

        SaveChest saveChest = ((SaveChestViewHolder) event.getInventory().getHolder()).getSaveChest();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        // Directly remove the item from the correct list
        boolean removed = saveChest.getStorageContents().remove(clickedItem) ||
                saveChest.getArmorContents().remove(clickedItem) ||
                saveChest.getExtraContents().remove(clickedItem);

        if (removed) {
            event.getWhoClicked().getInventory().addItem(clickedItem);
            event.getInventory().setItem(event.getSlot(), null); // Update the GUI
        }
    }

    public void retrieveAllItems(Player player, SaveChest saveChest) {
        List<ItemStack> allItems = new ArrayList<>();
        if (saveChest.getStorageContents() != null) allItems.addAll(saveChest.getStorageContents());
        if (saveChest.getArmorContents() != null) allItems.addAll(saveChest.getArmorContents());
        if (saveChest.getExtraContents() != null) allItems.addAll(saveChest.getExtraContents());

        for (ItemStack item : allItems) {
            if (item != null) player.getInventory().addItem(item).forEach((index, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        saveChest.clearContents();
        plugin.getChestManager().removeChest(saveChest, false);
        player.closeInventory();
        player.sendMessage(plugin.formatMessage("messages.chest-retrieved"));
    }

    public void claimExperience(Player player, SaveChest saveChest) {
        if (saveChest.getExperience() > 0) {
            player.giveExp(saveChest.getExperience());
            saveChest.setExperience(0);

            // Refresh the GUI to remove the experience bottle
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof SaveChestViewHolder) {
                player.getOpenInventory().getTopInventory().setItem(experienceBottleSlot, null);
            }
        }
    }

    public static class SaveChestViewHolder implements InventoryHolder {
        private final SaveChest saveChest;

        public SaveChestViewHolder(SaveChest saveChest) {
            this.saveChest = saveChest;
        }

        public SaveChest getSaveChest() {
            return saveChest;
        }

        @Override
        public @NotNull Inventory getInventory() {
            // This is intentionally not implemented as the GUI is managed by the GuiManager
            return null;
        }
    }
}

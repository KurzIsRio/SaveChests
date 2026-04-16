package com.kurz.savechests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kurz.savechests.chest.ChestManager;
import com.kurz.savechests.commands.ChestCommand;
import com.kurz.savechests.config.GsonAdapter;
import com.kurz.savechests.guard.WorldGuardManager;
import com.kurz.savechests.gui.GuiManager;
import com.kurz.savechests.hologram.HologramManager;
import com.kurz.savechests.listeners.DeathListener;
import com.kurz.savechests.listeners.InteractionListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SaveChests extends JavaPlugin {

    private Gson gson;
    private MiniMessage miniMessage;
    private ChestManager chestManager;
    private HologramManager hologramManager;
    private WorldGuardManager worldGuardManager;
    private GuiManager guiManager;
    private boolean papiEnabled = false;
    private boolean skinsRestorerEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.gson = createGson();
        this.miniMessage = MiniMessage.miniMessage();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiEnabled = true;
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        }
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuardManager = new WorldGuardManager();
            getLogger().info("Successfully hooked into WorldGuard.");
        } else {
            getLogger().warning("WorldGuard not found, chest placement protection will not be available.");
        }
        if (getServer().getPluginManager().getPlugin("SkinsRestorer") != null) {
            skinsRestorerEnabled = true;
            getLogger().info("Successfully hooked into SkinsRestorer.");
        } else {
            getLogger().warning("SkinsRestorer not found, player heads will use default skins.");
        }

        this.hologramManager = new HologramManager(this);
        this.chestManager = new ChestManager(this);
        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getCommand("savechests").setExecutor(new ChestCommand(this));

        getLogger().info("SaveChests has been enabled!");
    }

    @Override
    public void onDisable() {
        if (chestManager != null) {
            chestManager.shutdown();
        }
        getLogger().info("SaveChests has been disabled.");
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(ItemStack.class, new GsonAdapter.ItemStackAdapter())
                .enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .create();
    }

    public String formatMessage(String key) {
        String message = getConfig().getString(key, "&cMessage not found: " + key);
        return message;
    }

    // --- Accessors ---

    public Gson getGson() {
        return gson;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }

    public boolean isSkinsRestorerEnabled() {
        return skinsRestorerEnabled;
    }
}

# ⭐ SaveChests - Alpha/Beta ⭐
> A lightweight Saving Chests plugin created by **Kurz**. This plugin is currently in its alpha/beta version, so please report any bugs!

## 📜 Features
> SaveChests is packed with features to enhance the player experience and provide a reliable way to save items upon death.

*   **💎 Premium GUI**: A sleek and modern graphical user interface for interacting with saved chests. The GUI is designed with glass dividers for a premium feel.
*   **🤖 Smart Auto-Equip**: Automatically equips armor and other items from the saved chest, saving players time and effort.
*   **✨ Dynamic Holograms**: Displays a hologram above the saved chest, showing the remaining time until the chest expires.
*   **🌌 Void/End Safety**: Protects players' items from being lost in the void or the End.
*   **📍 Coordinate Tracking**: Tracks the coordinates of each saved chest, allowing players to easily find their items.

## 🔑 Commands & Permissions
> The following commands are available for players and administrators.

### Player Commands
```yaml
/savechests info <chestId> - View information about a specific chest.
/savechests list - List all of your saved chests.
/savechests purge - Purge all of your saved chests.
```

### Admin Commands
```yaml
/savechests reload - Reload the plugin's configuration.
```
*   **Permission**: `savechests.admin`

## ⚙️ Configuration
> The `config.yml` file allows you to customize various aspects of the plugin.

```yaml
# messages.yml
messages:
  no-chests: "&cYou don't have any saved chests."
  chest-not-found: "&cCould not find a chest with that ID."
  chest-info: |-
    &aChest Information:
    &7Owner: %owner%
    &7Location: %location%
    &7Time Remaining: %time%
  reload-success: "&aSuccessfully reloaded the configuration."

# settings.yml
settings:
  hologram-height: 1.5
  chest-duration: 3600 # in seconds
```

## 💡 Additional Insights
> Here are some additional details about the plugin.

*   **Dependencies**: This plugin has soft dependencies on `WorldGuard` and `PlaceholderAPI`.
*   **API Version**: The plugin is built against API version `1.21`.
*   **Maven Project**: The project is managed using Maven, with the `pom.xml` file defining the project structure and dependencies.

package yyz.chl.phantomcontrol.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.api.PhantomStatusChangeSource;
import yyz.chl.phantomcontrol.event.PhantomStatusChangeEvent;
import yyz.chl.phantomcontrol.event.PhantomStatusPreChangeEvent;

import java.util.List;
import java.util.UUID;

public class PhantomManager {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final PhantomControl plugin;

    private volatile boolean worldWhitelistEnabled;
    private volatile boolean worldBlacklistEnabled;
    private volatile List<String> worldWhitelist;
    private volatile List<String> worldBlacklist;

    public PhantomManager(PhantomControl plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        refreshWorldConfig();
    }

    public void reloadConfig() {
        refreshWorldConfig();
    }

    private void refreshWorldConfig() {
        this.worldWhitelistEnabled = configManager.getBoolean("whitelist.world-whitelist-enabled");
        this.worldBlacklistEnabled = configManager.getBoolean("whitelist.world-blacklist-enabled");
        this.worldWhitelist = configManager.getStringList("whitelist.world-whitelist");
        this.worldBlacklist = configManager.getStringList("whitelist.world-blacklist");
    }

    public boolean enablePhantoms(Player player) {
        return setPhantomsEnabled(player, true, PhantomStatusChangeSource.PLUGIN);
    }

    public boolean enablePhantoms(Player player, PhantomStatusChangeSource source) {
        return setPhantomsEnabled(player, true, source);
    }

    public boolean canDisablePhantoms(Player player) {
        return player.hasPermission("phantomcontrol.use");
    }

    public boolean disablePhantoms(Player player) {
        return setPhantomsEnabled(player, false, PhantomStatusChangeSource.PLUGIN);
    }

    public boolean disablePhantoms(Player player, PhantomStatusChangeSource source) {
        return setPhantomsEnabled(player, false, source);
    }

    public boolean setPhantomsEnabled(Player player, boolean enabled, PhantomStatusChangeSource source) {
        if (source == null) {
            source = PhantomStatusChangeSource.PLUGIN;
        }

        if (!enabled && !canDisablePhantoms(player)) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        boolean oldEnabled = databaseManager.getPlayerPhantomsStatus(playerId);
        if (oldEnabled == enabled) {
            return true;
        }

        PhantomStatusPreChangeEvent preChangeEvent = new PhantomStatusPreChangeEvent(player, oldEnabled, enabled, source);
        Bukkit.getPluginManager().callEvent(preChangeEvent);
        if (preChangeEvent.isCancelled()) {
            return false;
        }

        databaseManager.setPlayerPhantomsStatus(playerId, enabled);
        Bukkit.getPluginManager().callEvent(new PhantomStatusChangeEvent(player, oldEnabled, enabled, source));

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Updated phantom status for " + player.getName() + " (" + playerId + ") to "
                    + (enabled ? "enabled" : "disabled") + " via " + source);
        }

        return true;
    }

    public boolean hasPhantomsEnabled(Player player) {
        UUID playerId = player.getUniqueId();
        return databaseManager.getPlayerPhantomsStatus(playerId);
    }

    public void applyPhantomSettings(Player player) {
        if (!isWorldAllowed(player.getWorld().getName())) {
            return;
        }

        if (!canDisablePhantoms(player) && !hasPhantomsEnabled(player)) {
            enablePhantoms(player, PhantomStatusChangeSource.PERMISSION_ENFORCE);
        }
    }

    public boolean isWorldAllowed(String worldName) {
        if (worldWhitelistEnabled && !worldWhitelist.isEmpty() && !worldWhitelist.contains(worldName)) {
            return false;
        }

        return !worldBlacklistEnabled || worldBlacklist.isEmpty() || !worldBlacklist.contains(worldName);
    }

    public void shutdown() {
    }
}

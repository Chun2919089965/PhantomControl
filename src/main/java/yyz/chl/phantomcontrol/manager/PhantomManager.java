package yyz.chl.phantomcontrol.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.event.PhantomStatusChangeEvent;

import java.util.List;
import java.util.UUID;

/**
 * 幻翼控制核心管理器。
 * 幻翼拦截由 {@link yyz.chl.phantomcontrol.listener.PhantomSpawnListener} 通过 Paper 预生成事件负责。
 */
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

    public void enablePhantoms(Player player) {
        UUID playerId = player.getUniqueId();

        databaseManager.setPlayerPhantomsStatus(playerId, true);

        Bukkit.getPluginManager().callEvent(new PhantomStatusChangeEvent(player, true));

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("已为玩家 " + player.getName() + " (" + playerId + ") 启用幻翼");
        }
    }

    public boolean canDisablePhantoms(Player player) {
        return player.hasPermission("phantomcontrol.use");
    }

    public void disablePhantoms(Player player) {
        if (!canDisablePhantoms(player)) {
            return;
        }

        UUID playerId = player.getUniqueId();

        databaseManager.setPlayerPhantomsStatus(playerId, false);

        Bukkit.getPluginManager().callEvent(new PhantomStatusChangeEvent(player, false));

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("已为玩家 " + player.getName() + " (" + playerId + ") 禁用幻翼");
        }
    }

    public boolean hasPhantomsEnabled(Player player) {
        UUID playerId = player.getUniqueId();
        return databaseManager.getPlayerPhantomsStatus(playerId);
    }

    /**
     * 应用幻翼设置。玩家加入或切换世界时调用。
     * 对无权限玩家强制启用幻翼（防御性修复，正常流程不会走到这里）。
     */
    public void applyPhantomSettings(Player player) {
        if (!isWorldAllowed(player.getWorld().getName())) {
            return;
        }

        if (!canDisablePhantoms(player)) {
            if (!hasPhantomsEnabled(player)) {
                enablePhantoms(player);
            }
        }
    }

    /**
     * 检查世界是否在幻翼控制范围内（公开方法，供事件监听器调用）。
     */
    public boolean isWorldAllowed(String worldName) {
        if (worldWhitelistEnabled && !worldWhitelist.isEmpty()) {
            if (!worldWhitelist.contains(worldName)) {
                return false;
            }
        }

        if (worldBlacklistEnabled && !worldBlacklist.isEmpty()) {
            if (worldBlacklist.contains(worldName)) {
                return false;
            }
        }

        return true;
    }

    public void shutdown() {
        // 不再有定时任务需要取消，保留方法兼容 onDisable 调用
    }
}

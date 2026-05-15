package yyz.chl.phantomcontrol.manager;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.event.PhantomStatusChangeEvent;
import yyz.chl.phantomcontrol.util.SchedulerUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
        startPhantomIsolationTask();
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
        
        applyPhantomIsolation(player);
        
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("已为玩家 " + player.getName() + " (" + playerId + ") 禁用幻翼");
        }
    }
    
    public boolean hasPhantomsEnabled(Player player) {
        UUID playerId = player.getUniqueId();
        return databaseManager.getPlayerPhantomsStatus(playerId);
    }
    
    public void applyPhantomSettings(Player player) {
        if (!isWorldAllowed(player)) {
            return;
        }
        
        if (!canDisablePhantoms(player)) {
            if (!hasPhantomsEnabled(player)) {
                enablePhantoms(player);
            }
            return;
        }
        
        if (!hasPhantomsEnabled(player)) {
            applyPhantomIsolation(player);
        }
    }
    
    private void applyPhantomIsolation(Player player) {
        try {
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        } catch (NoSuchFieldError | NoSuchMethodError e) {
            plugin.getLogger().warning("服务器版本不支持 TIME_SINCE_REST 统计");
        } catch (Exception e) {
            plugin.getLogger().warning("应用幻翼隔离失败: " + e.getMessage());
        }
    }
    
    private volatile Object taskId;
    
    private void startPhantomIsolationTask() {
        cancelPhantomIsolationTask();
        
        int initialDelay = configManager.getInt("settings.task.initial-delay-ticks") / 20;
        int period = configManager.getInt("settings.task.period-ticks") / 20;
        
        if (initialDelay < 1) initialDelay = 1;
        if (period < 1) period = 120;
        
        this.taskId = runSyncScheduler(this::applyPhantomSettings, initialDelay, period);
    }
    
    private void restartTaskIfNeeded() {
        cancelPhantomIsolationTask();
        this.taskId = null;
        startPhantomIsolationTask();
    }
    
    private void cancelPhantomIsolationTask() {
        if (this.taskId != null) {
            SchedulerUtil.cancelTask(this.taskId);
            this.taskId = null;
        }
    }
    
    public void shutdown() {
        cancelPhantomIsolationTask();
    }
    
    private Object runSyncScheduler(Consumer<Player> playerTask, int initialDelay, int period) {
        return SchedulerUtil.runSyncTimer(() -> {
            Bukkit.getOnlinePlayers().forEach(playerTask);
        }, initialDelay, period);
    }
    
    private boolean isWorldAllowed(Player player) {
        String playerWorld = player.getWorld().getName();
        
        if (worldWhitelistEnabled && !worldWhitelist.isEmpty()) {
            if (!worldWhitelist.contains(playerWorld)) {
                return false;
            }
        }
        
        if (worldBlacklistEnabled && !worldBlacklist.isEmpty()) {
            if (worldBlacklist.contains(playerWorld)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPhantomSettings(player);
        }
    }
}
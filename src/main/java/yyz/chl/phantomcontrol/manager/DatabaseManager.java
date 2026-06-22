package yyz.chl.phantomcontrol.manager;

import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    
    private static class CacheEntry {
        public final boolean value;
        public volatile long timestamp;
        
        public CacheEntry(boolean value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private DatabaseHandler databaseHandler;
    private final ConcurrentHashMap<UUID, CacheEntry> playerDataCache;
    private final ConfigManager configManager;
    private final PhantomControl plugin;
    private final ScheduledExecutorService cleanupExecutor;
    private final ExecutorService saveExecutor;
    
    public DatabaseManager(PhantomControl plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        this.saveExecutor = Executors.newSingleThreadExecutor();
        initializeDatabase();
        startCacheCleanup();
    }
    
    public void initializeDatabase() {
        String databaseType = configManager.getDatabaseType();
        
        switch (databaseType) {
            case "mysql":
                this.databaseHandler = new MySQLDatabaseHandler(plugin, configManager);
                break;
            case "flatfile":
            default:
                this.databaseHandler = new FlatFileDatabaseHandler(plugin);
                break;
        }
        
        databaseHandler.connect();
        databaseHandler.initialize();
    }
    
    private void startCacheCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            long timeout = configManager.getLong("database.cache-timeout-minutes", 60) * 60 * 1000L;
            
            playerDataCache.entrySet().removeIf(entry -> {
                if (now - entry.getValue().timestamp > timeout) {
                    UUID playerId = entry.getKey();
                    boolean phantomsEnabled = entry.getValue().value;
                    
                    saveExecutor.submit(() -> {
                        databaseHandler.savePlayerData(playerId, phantomsEnabled);
                    });
                    
                    return true;
                }
                return false;
            });
        }, 1, 1, TimeUnit.HOURS);
    }
    
    public void reloadDatabase() {
        databaseHandler.closeConnection();
        
        initializeDatabase();
    }
    
    public void loadPlayerData(Player player) {
        loadPlayerData(player.getUniqueId());
        if (configManager.isDebugEnabled()) {
            CacheEntry entry = playerDataCache.get(player.getUniqueId());
            plugin.getLogger().info("已加载玩家数据: " + player.getName() + " (" + player.getUniqueId() + "), 幻翼: " + (entry != null && entry.value ? "启用" : "禁用"));
        }
    }
    
    public void loadPlayerData(UUID playerId) {
        CacheEntry existingEntry = playerDataCache.get(playerId);
        if (existingEntry != null) {
            existingEntry.timestamp = System.currentTimeMillis();
            return;
        }
        
        boolean phantomsEnabled = databaseHandler.loadPlayerData(playerId);
        playerDataCache.put(playerId, new CacheEntry(phantomsEnabled));
    }
    
    public void savePlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        
        CacheEntry entry = playerDataCache.remove(playerId);
        
        if (entry != null) {
            final boolean phantomsEnabled = entry.value;
            
            saveExecutor.submit(() -> {
                databaseHandler.savePlayerData(playerId, phantomsEnabled);
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("已保存玩家数据: " + playerName + " (" + playerId + ")");
                }
            });
        }
    }
    
    public void saveAllData() {
        Map<UUID, Boolean> playerDataMap = new java.util.HashMap<>();
        for (Map.Entry<UUID, CacheEntry> entry : playerDataCache.entrySet()) {
            playerDataMap.put(entry.getKey(), entry.getValue().value);
        }
        
        // 直接同步保存到数据库，避免插件禁用时的调度器问题
        databaseHandler.saveAllData(playerDataMap);
    }
    
    public boolean getPlayerPhantomsStatus(UUID playerId) {
        CacheEntry entry = playerDataCache.get(playerId);
        if (entry != null) {
            entry.timestamp = System.currentTimeMillis();
            return entry.value;
        }
        return true;
    }

    /**
     * 直接从数据库读取玩家幻翼状态（绕过缓存）。
     * 用于查询离线玩家等缓存未命中的场景。
     */
    public boolean getPlayerPhantomsStatusDirect(UUID playerId) {
        CacheEntry entry = playerDataCache.get(playerId);
        if (entry != null) {
            return entry.value;
        }
        return databaseHandler.loadPlayerData(playerId);
    }

    /**
     * 直接写入数据库并更新缓存（同步操作）。
     * 用于管理离线玩家等场景。
     */
    public void setPlayerPhantomsStatusDirect(UUID playerId, boolean enabled) {
        playerDataCache.put(playerId, new CacheEntry(enabled));
        databaseHandler.savePlayerData(playerId, enabled);
    }
    
    public void setPlayerPhantomsStatus(UUID playerId, boolean enabled) {
        playerDataCache.compute(playerId, (key, currentEntry) -> {
            boolean currentStatus = currentEntry != null ? currentEntry.value : true;
            if (currentStatus == enabled) {
                if (currentEntry != null) {
                    currentEntry.timestamp = System.currentTimeMillis();
                }
                return currentEntry;
            }

            CacheEntry newEntry = new CacheEntry(enabled);

            saveExecutor.submit(() -> {
                databaseHandler.savePlayerData(playerId, enabled);
            });

            return newEntry;
        });
    }
    
    public void flushSaveExecutor() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("数据保存线程池未能在10秒内关闭");
            }
        } catch (InterruptedException e) {
            plugin.getLogger().warning("等待数据保存线程池关闭时被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public void closeConnection() {
        cleanupExecutor.shutdownNow();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("缓存清理线程池未能在5秒内关闭");
            }
        } catch (InterruptedException e) {
            plugin.getLogger().warning("关闭缓存清理线程池时被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        databaseHandler.closeConnection();
    }
}

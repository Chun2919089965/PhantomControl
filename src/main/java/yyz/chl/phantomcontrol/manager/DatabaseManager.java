package yyz.chl.phantomcontrol.manager;

import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DatabaseManager {

    private static class CacheEntry {
        private final boolean value;
        private volatile long timestamp;

        private CacheEntry(boolean value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private DatabaseHandler databaseHandler;
    private final ConcurrentHashMap<UUID, CacheEntry> playerDataCache;
    private final Set<UUID> activePlayerIds;
    private final Set<UUID> loadingPlayerIds;
    private final ConfigManager configManager;
    private final PhantomControl plugin;
    private final ScheduledExecutorService cleanupExecutor;
    private final ExecutorService databaseExecutor;
    private final Object databaseIoLock = new Object();

    public DatabaseManager(PhantomControl plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.activePlayerIds = ConcurrentHashMap.newKeySet();
        this.loadingPlayerIds = ConcurrentHashMap.newKeySet();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        this.databaseExecutor = Executors.newSingleThreadExecutor();
        initializeDatabase();
        startCacheCleanup();
    }

    private void initializeDatabase() {
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
                UUID playerId = entry.getKey();
                if (activePlayerIds.contains(playerId)) {
                    entry.getValue().timestamp = now;
                    return false;
                }

                if (now - entry.getValue().timestamp > timeout) {
                    queueSave(playerId, entry.getValue().value, null);
                    return true;
                }
                return false;
            });
        }, 1, 1, TimeUnit.HOURS);
    }

    public void reloadDatabase() {
        synchronized (databaseIoLock) {
            waitForPendingDatabaseTasksLocked();
            saveAllData();
            databaseHandler.closeConnection();
            initializeDatabase();
            saveAllData();
        }
    }

    public CompletableFuture<Boolean> loadPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        activePlayerIds.add(playerId);

        CacheEntry existingEntry = playerDataCache.get(playerId);
        if (existingEntry != null) {
            existingEntry.timestamp = System.currentTimeMillis();
            return CompletableFuture.completedFuture(existingEntry.value);
        }

        loadingPlayerIds.add(playerId);
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        synchronized (databaseIoLock) {
            try {
                databaseExecutor.submit(() -> {
                    try {
                        boolean phantomsEnabled = databaseHandler.loadPlayerData(playerId);
                        if (activePlayerIds.contains(playerId)) {
                            playerDataCache.putIfAbsent(playerId, new CacheEntry(phantomsEnabled));
                            CacheEntry cachedEntry = playerDataCache.get(playerId);
                            if (cachedEntry != null) {
                                cachedEntry.timestamp = System.currentTimeMillis();
                            }
                        }

                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("已异步加载玩家数据: " + playerName + " (" + playerId + "), 幻翼: "
                                    + (phantomsEnabled ? "启用" : "禁用"));
                        }

                        result.complete(phantomsEnabled);
                    } catch (RuntimeException e) {
                        result.completeExceptionally(e);
                    } finally {
                        loadingPlayerIds.remove(playerId);
                    }
                });
            } catch (RejectedExecutionException e) {
                loadingPlayerIds.remove(playerId);
                result.completeExceptionally(e);
            }
        }
        return result;
    }

    public void savePlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        activePlayerIds.remove(playerId);
        loadingPlayerIds.remove(playerId);

        CacheEntry entry = playerDataCache.remove(playerId);
        if (entry != null) {
            queueSave(playerId, entry.value, "已保存玩家数据: " + playerName + " (" + playerId + ")");
        }
    }

    public void saveAllData() {
        Map<UUID, Boolean> playerDataMap = new HashMap<>();
        for (Map.Entry<UUID, CacheEntry> entry : playerDataCache.entrySet()) {
            playerDataMap.put(entry.getKey(), entry.getValue().value);
        }

        databaseHandler.saveAllData(playerDataMap);
    }

    /**
     * Returns the cached status for online logic. This method never performs database I/O.
     */
    public boolean getPlayerPhantomsStatus(UUID playerId) {
        CacheEntry entry = playerDataCache.get(playerId);
        if (entry != null) {
            entry.timestamp = System.currentTimeMillis();
            return entry.value;
        }
        if (loadingPlayerIds.contains(playerId)) {
            return false;
        }
        return true;
    }

    /**
     * Directly reads a player status from storage. Intended for offline admin queries.
     */
    public boolean getPlayerPhantomsStatusDirect(UUID playerId) {
        CacheEntry entry = playerDataCache.get(playerId);
        if (entry != null) {
            return entry.value;
        }
        synchronized (databaseIoLock) {
            waitForPendingDatabaseTasksLocked();
            return databaseHandler.loadPlayerData(playerId);
        }
    }

    /**
     * Directly writes storage and updates cache. Intended for offline admin operations.
     */
    public void setPlayerPhantomsStatusDirect(UUID playerId, boolean enabled) {
        playerDataCache.put(playerId, new CacheEntry(enabled));
        synchronized (databaseIoLock) {
            waitForPendingDatabaseTasksLocked();
            databaseHandler.savePlayerData(playerId, enabled);
        }
    }

    public void setPlayerPhantomsStatus(UUID playerId, boolean enabled) {
        playerDataCache.compute(playerId, (key, currentEntry) -> {
            boolean currentStatus = currentEntry != null ? currentEntry.value : true;
            if (currentStatus == enabled) {
                CacheEntry entry = currentEntry != null ? currentEntry : new CacheEntry(enabled);
                entry.timestamp = System.currentTimeMillis();
                return entry;
            }

            CacheEntry newEntry = new CacheEntry(enabled);
            queueSave(playerId, enabled, null);
            return newEntry;
        });
    }

    private void queueSave(UUID playerId, boolean phantomsEnabled, String debugMessage) {
        Runnable saveTask = () -> {
            try {
                databaseHandler.savePlayerData(playerId, phantomsEnabled);
                if (debugMessage != null && configManager.isDebugEnabled()) {
                    plugin.getLogger().info(debugMessage);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().severe("保存玩家数据失败: " + playerId + " - " + e.getMessage());
            }
        };

        synchronized (databaseIoLock) {
            try {
                databaseExecutor.submit(saveTask);
            } catch (RejectedExecutionException e) {
                saveTask.run();
            }
        }
    }

    private void waitForPendingDatabaseTasksLocked() {
        try {
            Future<?> marker = databaseExecutor.submit(() -> {
            });
            marker.get(10, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
        } catch (TimeoutException e) {
            plugin.getLogger().warning("等待数据库任务队列完成超时");
        } catch (InterruptedException e) {
            plugin.getLogger().warning("等待数据库任务队列完成时被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            plugin.getLogger().warning("等待数据库任务队列完成时发生错误: " + e.getMessage());
        }
    }

    private void shutdownDatabaseExecutorLocked() {
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("数据库任务线程池未能在10秒内关闭");
            }
        } catch (InterruptedException e) {
            plugin.getLogger().warning("等待数据库任务线程池关闭时被中断: " + e.getMessage());
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

        synchronized (databaseIoLock) {
            waitForPendingDatabaseTasksLocked();
            saveAllData();
            shutdownDatabaseExecutorLocked();
            databaseHandler.closeConnection();
        }
    }
}

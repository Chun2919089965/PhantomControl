package yyz.chl.phantomcontrol;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.MultiLineChart;
import yyz.chl.phantomcontrol.api.DefaultPhantomControlAPI;
import yyz.chl.phantomcontrol.api.PhantomControlAPI;
import yyz.chl.phantomcontrol.command.CommandManager;
import yyz.chl.phantomcontrol.listener.ListenerManager;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.DatabaseManager;
import yyz.chl.phantomcontrol.manager.GUIManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;
import yyz.chl.phantomcontrol.util.MessageUtil;
import java.util.HashMap;
import java.util.Map;

public class PhantomControl extends JavaPlugin {
    public static PhantomControl instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PhantomManager phantomManager;
    private GUIManager guiManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;
    private MessageUtil messageUtil;
    private PhantomControlAPI api;
    private Object autoSaveTaskId;
    private int activeAutoSaveInterval = -1;

    @Override
    public void onEnable() {
        instance = this;
        
        if (!isPaperRuntime()) {
            getLogger().severe("PhantomControl 仅支持 Paper 或 Folia 服务器，不再支持 Spigot。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        yyz.chl.phantomcontrol.util.SchedulerUtil.setPluginEnabled(true);
        
        configManager = new ConfigManager(this);
        
        if (!configManager.validateConfig()) {
            getLogger().severe("配置验证失败，插件已禁用。请检查配置文件并修复错误。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        try {
            databaseManager = new DatabaseManager(this, configManager);
        } catch (RuntimeException e) {
            getLogger().severe("数据库初始化失败，插件已禁用: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        phantomManager = new PhantomManager(this, databaseManager, configManager);
        api = new DefaultPhantomControlAPI(phantomManager, databaseManager);
        getServer().getServicesManager().register(PhantomControlAPI.class, api, this, ServicePriority.Normal);
        
        guiManager = new GUIManager(phantomManager, configManager);
        
        messageUtil = new MessageUtil(configManager);
        
        commandManager = new CommandManager(this, configManager, phantomManager, guiManager, messageUtil);
        
        listenerManager = new ListenerManager(this, databaseManager, phantomManager, guiManager, configManager, messageUtil);
        
        startAutoSaveTask();
        
        registerPlaceholderAPI();
        
        if (configManager.getBoolean("settings.bstats.enabled", true)) {
            int pluginId = 29276;
            Metrics metrics = new Metrics(this, pluginId);
            
            metrics.addCustomChart(new MultiLineChart("players_and_servers", () -> {
                Map<String, Integer> valueMap = new HashMap<>();
                valueMap.put("servers", 1);
                valueMap.put("players", Bukkit.getOnlinePlayers().size());
                return valueMap;
            }));
        }
        
        getLogger().info("PhantomControl 已成功加载！作者：CHL_chun");
    }

    private boolean isPaperRuntime() {
        try {
            Class.forName("com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new yyz.chl.phantomcontrol.placeholder.PhantomControlPlaceholder(this, phantomManager, configManager).register();
            getLogger().info("已成功注册PlaceholderAPI扩展！");
        } else {
            getLogger().info("未检测到PlaceholderAPI，跳过扩展注册。");
        }
    }

    @Override
    public void onDisable() {
        if (isPaperRuntime()) {
            yyz.chl.phantomcontrol.util.SchedulerUtil.setPluginEnabled(false);
        }

        cancelAutoSaveTask();

        if (phantomManager != null) {
            phantomManager.shutdown();
        }

        if (api != null) {
            getServer().getServicesManager().unregister(PhantomControlAPI.class, api);
            api = null;
        }

        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        if (isPaperRuntime()) {
            yyz.chl.phantomcontrol.util.SchedulerUtil.shutdown();
        }

        instance = null;

        getLogger().info("PhantomControl 已成功卸载！");
    }

    public static PhantomControl getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PhantomManager getPhantomManager() {
        return phantomManager;
    }

    public PhantomControlAPI getAPI() {
        return api;
    }
    
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public ReloadResult reloadAll() {
        ConfigManager.RuntimeConfigSnapshot previousConfig = configManager.snapshotRuntimeConfig();
        boolean databaseReloaded;

        try {
            configManager.reloadConfig();
            databaseReloaded = databaseManager.reloadDatabase();
        } catch (RuntimeException e) {
            configManager.restoreRuntimeConfig(previousConfig);
            throw e;
        }

        phantomManager.reloadConfig();
        guiManager.refreshGUIConfig();
        refreshAutoSaveTask();

        return new ReloadResult(databaseReloaded,
                !commandManager.isConfiguredCommandRegistrationCurrent());
    }

    private void startAutoSaveTask() {
        int autoSaveInterval = configManager.getInt("database.auto-save-interval");
        this.autoSaveTaskId = createAutoSaveTask(autoSaveInterval);
        this.activeAutoSaveInterval = autoSaveInterval;
    }

    private void refreshAutoSaveTask() {
        int newInterval = configManager.getInt("database.auto-save-interval");
        if (newInterval == activeAutoSaveInterval) {
            return;
        }

        Object newTaskId = createAutoSaveTask(newInterval);
        Object oldTaskId = this.autoSaveTaskId;
        this.autoSaveTaskId = newTaskId;
        this.activeAutoSaveInterval = newInterval;
        if (oldTaskId != null) {
            yyz.chl.phantomcontrol.util.SchedulerUtil.cancelTask(oldTaskId);
        }
    }

    private Object createAutoSaveTask(int interval) {
        if (interval <= 0) {
            return null;
        }
        return yyz.chl.phantomcontrol.util.SchedulerUtil.runAsyncTimer(
                databaseManager::saveAllData, interval, interval);
    }

    private void cancelAutoSaveTask() {
        if (this.autoSaveTaskId != null) {
            yyz.chl.phantomcontrol.util.SchedulerUtil.cancelTask(this.autoSaveTaskId);
            this.autoSaveTaskId = null;
        }
        this.activeAutoSaveInterval = -1;
    }

    public record ReloadResult(boolean databaseReloaded, boolean commandsRequireRestart) {
    }
}

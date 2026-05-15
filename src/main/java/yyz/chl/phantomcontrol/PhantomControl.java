package yyz.chl.phantomcontrol;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.MultiLineChart;
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

    @Override
    public void onEnable() {
        instance = this;
        
        yyz.chl.phantomcontrol.util.SchedulerUtil.setPluginEnabled(true);
        
        configManager = new ConfigManager(this);
        
        if (!configManager.validateConfig()) {
            getLogger().severe("配置验证失败，插件已禁用。请检查配置文件并修复错误。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        databaseManager = new DatabaseManager(this, configManager);
        
        phantomManager = new PhantomManager(this, databaseManager, configManager);
        
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
        yyz.chl.phantomcontrol.util.SchedulerUtil.setPluginEnabled(false);
        
        if (phantomManager != null) {
            phantomManager.shutdown();
        }
        
        if (databaseManager != null) {
            databaseManager.flushSaveExecutor();
            databaseManager.saveAllData();
            databaseManager.closeConnection();
        }
        
        yyz.chl.phantomcontrol.util.SchedulerUtil.shutdown();
        
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
    
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public void reloadCommandManager() {
        this.commandManager = new CommandManager(this, configManager, phantomManager, guiManager, messageUtil);
    }

    public void reloadAll() {
        configManager.reloadConfig();
        databaseManager.reloadDatabase();
        phantomManager.reloadConfig();
        guiManager.refreshGUIConfig();
        reloadCommandManager();
    }

    private void startAutoSaveTask() {
        int autoSaveInterval = configManager.getInt("database.auto-save-interval");
        
        if (autoSaveInterval > 0) {
            yyz.chl.phantomcontrol.util.SchedulerUtil.runAsyncTimer(() -> {
                databaseManager.saveAllData();
            }, autoSaveInterval, autoSaveInterval);
        }
    }
}

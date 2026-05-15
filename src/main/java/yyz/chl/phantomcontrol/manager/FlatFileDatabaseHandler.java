package yyz.chl.phantomcontrol.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import yyz.chl.phantomcontrol.PhantomControl;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class FlatFileDatabaseHandler implements DatabaseHandler {
    
    private final PhantomControl plugin;
    private final File dataFile;
    private final Object dataLock = new Object();
    private YamlConfiguration dataConfig;
    private volatile boolean dirty;
    
    public FlatFileDatabaseHandler(PhantomControl plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    @Override
    public void connect() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建玩家数据文件: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public boolean loadPlayerData(UUID playerId) {
        synchronized (dataLock) {
            return dataConfig.getBoolean(playerId.toString(), true);
        }
    }
    
    @Override
    public void savePlayerData(UUID playerId, boolean phantomsEnabled) {
        synchronized (dataLock) {
            dataConfig.set(playerId.toString(), phantomsEnabled);
            dirty = true;
        }
    }
    
    @Override
    public void saveAllData(Map<UUID, Boolean> playerDataMap) {
        synchronized (dataLock) {
            for (Map.Entry<UUID, Boolean> entry : playerDataMap.entrySet()) {
                dataConfig.set(entry.getKey().toString(), entry.getValue());
            }
            dirty = true;
            saveConfig();
        }
    }
    
    @Override
    public void closeConnection() {
        synchronized (dataLock) {
            if (dirty) {
                saveConfig();
            }
        }
    }
    
    @Override
    public void reloadDatabase() {
        synchronized (dataLock) {
            if (dirty) {
                saveConfig();
            }
            this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        }
    }
    
    private void saveConfig() {
        try {
            dataConfig.save(dataFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家数据: " + e.getMessage());
        }
    }
}
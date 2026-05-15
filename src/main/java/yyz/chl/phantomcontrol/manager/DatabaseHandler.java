package yyz.chl.phantomcontrol.manager;

import java.util.Map;
import java.util.UUID;

public interface DatabaseHandler {
    
    void connect();
    
    void initialize();
    
    boolean loadPlayerData(UUID playerId);
    
    void savePlayerData(UUID playerId, boolean phantomsEnabled);
    
    void saveAllData(Map<UUID, Boolean> playerDataMap);
    
    void closeConnection();
    
    void reloadDatabase();
}
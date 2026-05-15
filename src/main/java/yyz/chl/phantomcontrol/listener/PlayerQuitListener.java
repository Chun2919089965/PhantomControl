package yyz.chl.phantomcontrol.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import yyz.chl.phantomcontrol.manager.DatabaseManager;

public class PlayerQuitListener implements Listener {
    
    private final DatabaseManager databaseManager;
    
    public PlayerQuitListener(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        databaseManager.savePlayerData(event.getPlayer());
    }
}

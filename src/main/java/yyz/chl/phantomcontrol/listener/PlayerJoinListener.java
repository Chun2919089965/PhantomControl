package yyz.chl.phantomcontrol.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import yyz.chl.phantomcontrol.manager.DatabaseManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;

public class PlayerJoinListener implements Listener {
    
    private final DatabaseManager databaseManager;
    private final PhantomManager phantomManager;
    
    public PlayerJoinListener(DatabaseManager databaseManager, PhantomManager phantomManager) {
        this.databaseManager = databaseManager;
        this.phantomManager = phantomManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        databaseManager.loadPlayerData(event.getPlayer());
        phantomManager.applyPhantomSettings(event.getPlayer());
    }
}

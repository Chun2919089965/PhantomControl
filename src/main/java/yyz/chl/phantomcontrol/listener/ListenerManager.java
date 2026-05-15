package yyz.chl.phantomcontrol.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.DatabaseManager;
import yyz.chl.phantomcontrol.manager.GUIManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;
import yyz.chl.phantomcontrol.util.MessageUtil;

public class ListenerManager {
    
    public ListenerManager(PhantomControl plugin, DatabaseManager databaseManager, 
                           PhantomManager phantomManager, GUIManager guiManager,
                           ConfigManager configManager, MessageUtil messageUtil) {
        registerListeners(plugin, databaseManager, phantomManager, guiManager, configManager, messageUtil);
    }
    
    private void registerListeners(PhantomControl plugin, DatabaseManager databaseManager, PhantomManager phantomManager,
                                   GUIManager guiManager, ConfigManager configManager, MessageUtil messageUtil) {
        registerListener(plugin, new PlayerJoinListener(databaseManager, phantomManager));
        registerListener(plugin, new PlayerQuitListener(databaseManager));
        registerListener(plugin, new GUIListener(guiManager, phantomManager, configManager, messageUtil));
    }
    
    private void registerListener(PhantomControl plugin, Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
}

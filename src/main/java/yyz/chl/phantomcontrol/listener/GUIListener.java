package yyz.chl.phantomcontrol.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.GUIManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;
import yyz.chl.phantomcontrol.util.MessageUtil;

public class GUIListener implements Listener {
    
    private final GUIManager guiManager;
    private final PhantomManager phantomManager;
    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    
    public GUIListener(GUIManager guiManager, PhantomManager phantomManager, ConfigManager configManager, MessageUtil messageUtil) {
        this.guiManager = guiManager;
        this.phantomManager = phantomManager;
        this.configManager = configManager;
        this.messageUtil = messageUtil;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!guiManager.isPhantomControlInventory(event.getInventory())) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        if (slot == GUIManager.SLOT_ENABLE) {
            enablePlayer(player);
        } else if (slot == GUIManager.SLOT_DISABLE) {
            disablePlayer(player);
        } else if (slot == GUIManager.SLOT_STATUS) {
            if (phantomManager.hasPhantomsEnabled(player)) {
                disablePlayer(player);
            } else {
                enablePlayer(player);
            }
        }
    }
    
    private void enablePlayer(Player player) {
        phantomManager.enablePhantoms(player);
        String message = configManager.getMessage(player, "command.enabled");
        messageUtil.sendOnChange(player, message,
            configManager.getMessage(player, "command.enabled"),
            configManager.getMessage(player, "command.enabled"));
        guiManager.updateGUI(player);
    }
    
    private void disablePlayer(Player player) {
        if (!phantomManager.canDisablePhantoms(player)) {
            messageUtil.sendMessage(player, configManager.getMessage(player, "command.cannot-disable"));
            return;
        }
        phantomManager.disablePhantoms(player);
        String message = configManager.getMessage(player, "command.disabled");
        messageUtil.sendOnChange(player, message,
            configManager.getMessage(player, "command.disabled"),
            configManager.getMessage(player, "command.disabled"));
        guiManager.updateGUI(player);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        guiManager.handleInventoryClose(player);
    }
}

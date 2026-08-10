package yyz.chl.phantomcontrol.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;

import java.util.logging.Level;

public class ReloadCommand implements CommandExecutor {
    
    private final PhantomControl plugin;
    private final ConfigManager configManager;
    
    public ReloadCommand(PhantomControl plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("phantomcontrol.reload")) {
            sender.sendMessage(configManager.getMessage("reload-command.no-permission"));
            return true;
        }
        
        try {
            PhantomControl.ReloadResult result = plugin.reloadAll();
            
            sender.sendMessage(configManager.getMessage("reload-command.success"));
            if (result.commandsRequireRestart()) {
                sender.sendMessage(configManager.getMessage(
                        "reload-command.commands-restart-required",
                        "Command names or aliases changed. Restart the server to apply them."));
            }
            plugin.getLogger().info(sender.getName() + " 重载了插件配置");
        } catch (Exception e) {
            String detail = e.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = e.getClass().getSimpleName();
            }
            String errorMessage = configManager.formatMessage(
                    "reload-command.error", "%error%", detail);
            sender.sendMessage(errorMessage);
            plugin.getLogger().log(Level.SEVERE, "重载配置失败", e);
        }
        
        return true;
    }
}

package yyz.chl.phantomcontrol.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;

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
            plugin.reloadAll();
            
            sender.sendMessage(configManager.getMessage("reload-command.success"));
            plugin.getLogger().info(sender.getName() + " 重载了插件配置");
        } catch (Exception e) {
            String errorMessage = configManager.formatMessage("reload-command.error", "%error%", e.getMessage());
            sender.sendMessage(errorMessage);
            plugin.getLogger().severe("重载配置失败: " + e.getMessage());
        }
        
        return true;
    }
}

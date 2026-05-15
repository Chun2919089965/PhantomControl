package yyz.chl.phantomcontrol.command;

import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.GUIManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;
import yyz.chl.phantomcontrol.util.MessageUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

public class CommandManager {
    private static final Constructor<PluginCommand> PLUGIN_COMMAND_CONSTRUCTOR;
    
    static {
        Constructor<PluginCommand> constructor = null;
        try {
            constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
        PLUGIN_COMMAND_CONSTRUCTOR = constructor;
    }
    
    private final PhantomControl plugin;
    private final ConfigManager configManager;
    private final PhantomManager phantomManager;
    private final GUIManager guiManager;
    private final MessageUtil messageUtil;
    private CommandMap commandMap;
    
    public CommandManager(PhantomControl plugin, ConfigManager configManager, 
                           PhantomManager phantomManager, GUIManager guiManager, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.phantomManager = phantomManager;
        this.guiManager = guiManager;
        this.messageUtil = messageUtil;
        this.commandMap = getCommandMap();
        registerCommands();
    }
    
    private CommandMap getCommandMap() {
        try {
            PluginManager pluginManager = plugin.getServer().getPluginManager();
            
            if (pluginManager instanceof SimplePluginManager) {
                Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(pluginManager);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().severe("获取CommandMap失败: " + e.getMessage());
        }
        return null;
    }
    
    private PluginCommand createPluginCommand(String name, Plugin pluginRef) {
        if (PLUGIN_COMMAND_CONSTRUCTOR == null) {
            plugin.getLogger().severe("PluginCommand构造函数未找到");
            return null;
        }
        try {
            return PLUGIN_COMMAND_CONSTRUCTOR.newInstance(name, pluginRef);
        } catch (Exception e) {
            plugin.getLogger().severe("创建PluginCommand失败: " + e.getMessage());
            return null;
        }
    }
    
    private void registerCommands() {
        String mainCommand = configManager.getString("settings.commands.main-command");
        List<String> mainAliases = configManager.getStringList("settings.commands.main-aliases");
        String reloadCommand = configManager.getString("settings.commands.reload-command");
        List<String> reloadAliases = configManager.getStringList("settings.commands.reload-aliases");
        
        PhantomControlCommand mainExecutor = new PhantomControlCommand(plugin, phantomManager, configManager, guiManager, messageUtil);
        PhantomControlTabCompleter mainTabCompleter = new PhantomControlTabCompleter();
        ReloadCommand reloadExecutor = new ReloadCommand(plugin, configManager);
        
        registerCommand(mainCommand, mainExecutor, mainTabCompleter, mainAliases);
        registerCommand(reloadCommand, reloadExecutor, null, reloadAliases);
    }
    
    private void registerCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter, List<String> aliases) {
        if (commandMap == null) {
            plugin.getLogger().severe("CommandMap 未初始化，无法注册命令: " + commandName);
            return;
        }
        
        if (commandName == null || commandName.isEmpty()) {
            plugin.getLogger().severe("命令名称不能为空");
            return;
        }
        
        Command oldCommand = commandMap.getCommand(commandName);
        if (oldCommand != null) {
            oldCommand.unregister(commandMap);
        }
        
        for (String alias : aliases) {
            if (!alias.equals(commandName)) {
                Command oldAliasCommand = commandMap.getCommand(alias);
                if (oldAliasCommand != null) {
                    oldAliasCommand.unregister(commandMap);
                }
            }
        }
        
        PluginCommand mainCmd = createPluginCommand(commandName, plugin);
        if (mainCmd != null) {
            mainCmd.setExecutor(executor);
            if (tabCompleter != null) {
                mainCmd.setTabCompleter(tabCompleter);
            }
            commandMap.register(plugin.getName().toLowerCase(), mainCmd);
        }
        
        for (String alias : aliases) {
            if (!alias.equals(commandName)) {
                PluginCommand aliasCommand = createPluginCommand(alias, plugin);
                if (aliasCommand != null) {
                    aliasCommand.setExecutor(executor);
                    if (tabCompleter != null) {
                        aliasCommand.setTabCompleter(tabCompleter);
                    }
                    commandMap.register(plugin.getName().toLowerCase(), aliasCommand);
                }
            }
        }
    }
}

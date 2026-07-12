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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandManager {

    private final PhantomControl plugin;
    private final ConfigManager configManager;
    private final PhantomManager phantomManager;
    private final GUIManager guiManager;
    private final MessageUtil messageUtil;
    private CommandMap commandMap;
    private Map<String, Command> knownCommands;
    
    public CommandManager(PhantomControl plugin, ConfigManager configManager, 
                           PhantomManager phantomManager, GUIManager guiManager, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.phantomManager = phantomManager;
        this.guiManager = guiManager;
        this.messageUtil = messageUtil;
        this.commandMap = getCommandMap();
        this.knownCommands = getKnownCommands();
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

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() {
        if (commandMap == null) {
            return null;
        }

        Class<?> currentClass = commandMap.getClass();
        while (currentClass != null) {
            try {
                Field knownCommandsField = currentClass.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                return (Map<String, Command>) knownCommandsField.get(commandMap);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException e) {
                plugin.getLogger().warning("获取 knownCommands 失败: " + e.getMessage());
                return null;
            }
        }

        plugin.getLogger().warning("CommandMap 不包含 knownCommands 字段，旧命令别名可能无法完全清理");
        return null;
    }
    
    private void registerCommands() {
        unregisterPluginCommands();

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

    private void unregisterPluginCommands() {
        if (commandMap == null || knownCommands == null) {
            return;
        }

        Set<Command> pluginCommands = new HashSet<>();
        for (Command command : knownCommands.values()) {
            if (isPluginCommand(command)) {
                pluginCommands.add(command);
            }
        }

        for (Command command : pluginCommands) {
            command.unregister(commandMap);
        }

        knownCommands.entrySet().removeIf(entry -> isPluginCommand(entry.getValue()));
    }

    private boolean isPluginCommand(Command command) {
        return command instanceof PluginIdentifiableCommand
                && ((PluginIdentifiableCommand) command).getPlugin().equals(plugin);
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
        
        Command existingCommand = knownCommands != null ? knownCommands.get(commandName.toLowerCase()) : null;
        if (existingCommand != null && !isPluginCommand(existingCommand)) {
            plugin.getLogger().warning("命令 /" + commandName + " 已被其他插件占用，可能只能通过命名空间命令访问");
        }
        
        for (String alias : aliases) {
            Command existingAlias = knownCommands != null ? knownCommands.get(alias.toLowerCase()) : null;
            if (!alias.equals(commandName) && existingAlias != null && !isPluginCommand(existingAlias)) {
                plugin.getLogger().warning("命令别名 /" + alias + " 已被其他插件占用，已跳过该别名");
            }
        }

        List<String> availableAliases = new ArrayList<>();
        for (String alias : aliases) {
            if (!alias.equals(commandName) && (knownCommands == null || !knownCommands.containsKey(alias.toLowerCase()))) {
                availableAliases.add(alias);
            }
        }

        DynamicPluginCommand dynamicCommand = new DynamicPluginCommand(commandName, availableAliases, plugin, executor, tabCompleter);
        commandMap.register(plugin.getName().toLowerCase(), dynamicCommand);
    }

    private static class DynamicPluginCommand extends Command implements PluginIdentifiableCommand {

        private final Plugin plugin;
        private final CommandExecutor executor;
        private final TabCompleter tabCompleter;

        private DynamicPluginCommand(String name, List<String> aliases, Plugin plugin,
                                     CommandExecutor executor, TabCompleter tabCompleter) {
            super(name, "", "/" + name, aliases);
            this.plugin = plugin;
            this.executor = executor;
            this.tabCompleter = tabCompleter;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return executor.onCommand(sender, this, commandLabel, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
            if (tabCompleter == null) {
                return super.tabComplete(sender, alias, args);
            }
            List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
            return completions != null ? completions : super.tabComplete(sender, alias, args);
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}

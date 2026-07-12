package yyz.chl.phantomcontrol.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhantomControlTabCompleter implements TabCompleter {
    
    private static final List<String> SUB_COMMANDS = Arrays.asList("enable", "disable", "toggle", "status", "check", "admin", "help", "gui", "menu", "界面", "切换");
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            completions.addAll(StringUtil.copyPartialMatches(partial, SUB_COMMANDS, new ArrayList<>()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("admin")) {
                if (sender.hasPermission("phantomcontrol.admin")) {
                    List<String> adminCommands = Arrays.asList("enable", "disable", "status", "batch", "server");
                    String partial = args[1].toLowerCase();
                    completions.addAll(StringUtil.copyPartialMatches(partial, adminCommands, new ArrayList<>()));
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String adminSubCommand = args[1].toLowerCase();
            
            if (subCommand.equals("admin") && sender.hasPermission("phantomcontrol.admin")) {
                if (adminSubCommand.equals("enable") || adminSubCommand.equals("disable") || adminSubCommand.equals("status")) {
                    String partial = args[2].toLowerCase();
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList()));
                } else if (adminSubCommand.equals("batch")) {
                    List<String> batchCommands = Arrays.asList("enable", "disable");
                    String partial = args[2].toLowerCase();
                    completions.addAll(StringUtil.copyPartialMatches(partial, batchCommands, new ArrayList<>()));
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String adminSubCommand = args[1].toLowerCase();
            String subSubCommand = args[2].toLowerCase();
            
            if (subCommand.equals("admin") && sender.hasPermission("phantomcontrol.admin")) {
                if (adminSubCommand.equals("batch") && (subSubCommand.equals("enable") || subSubCommand.equals("disable"))) {
                    String partial = args[3].toLowerCase();
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList()));
                }
            }
        }

        return completions;
    }
}
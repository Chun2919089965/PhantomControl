package yyz.chl.phantomcontrol.command;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.GUIManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;
import yyz.chl.phantomcontrol.util.MessageUtil;

public class PhantomControlCommand implements CommandExecutor {
    
    private final PhantomControl plugin;
    private final PhantomManager phantomManager;
    private final ConfigManager configManager;
    private final GUIManager guiManager;
    private final MessageUtil messageUtil;
    
    public PhantomControlCommand(PhantomControl plugin, PhantomManager phantomManager, 
                                  ConfigManager configManager, GUIManager guiManager, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.phantomManager = phantomManager;
        this.configManager = configManager;
        this.guiManager = guiManager;
        this.messageUtil = messageUtil;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("command.not-player"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("phantomcontrol.use")) {
            messageUtil.sendMessage(player, configManager.getMessage(player, "command.no-permission"));
            return true;
        }
        
        if (args.length < 1) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "enable":
            case "on":
                enablePlayer(player);
                break;
            case "disable":
            case "off":
                disablePlayer(player);
                break;
            case "toggle":
            case "switch":
            case "切换":
                if (phantomManager.hasPhantomsEnabled(player)) {
                    disablePlayer(player);
                } else {
                    enablePlayer(player);
                }
                break;
            case "status":
            case "check":
                boolean status = phantomManager.hasPhantomsEnabled(player);
                String statusText = status ? configManager.getMessage(player, "command.status_enabled") : configManager.getMessage(player, "command.status_disabled");
                String statusMessage = configManager.formatMessage(player, "command.status", 
                    "%status%", statusText);
                messageUtil.sendMessage(player, statusMessage);
                break;
            case "gui":
            case "menu":
            case "界面":
                guiManager.openPhantomControlGUI(player);
                break;
            case "admin":
                handleAdminCommand(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                messageUtil.sendMessage(player, configManager.getMessage("command.usage"));
                break;
        }
        
        return true;
    }
    
    private void handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("phantomcontrol.admin")) {
            messageUtil.sendMessage(player, configManager.getMessage(player, "command.no-permission"));
            return;
        }

        if (args.length < 3) {
            String mainCommand = configManager.getString("settings.commands.main-command");
            String message = configManager.formatMessage(player, "admin.usage", "%maincommand%", mainCommand);
            messageUtil.sendMessage(player, message);
            return;
        }

        String adminSubCommand = args[1].toLowerCase();
        String targetPlayerName = args[2];

        // 先查在线玩家，再查离线玩家
        Player targetPlayer = org.bukkit.Bukkit.getPlayer(targetPlayerName);
        boolean targetOffline = (targetPlayer == null);
        java.util.UUID targetUuid = null;

        if (targetOffline) {
            OfflinePlayer offlinePlayer = resolveOfflinePlayer(targetPlayerName);
            if (offlinePlayer == null) {
                String message = configManager.formatMessage(player, "admin.player-not-found", "%player%", targetPlayerName);
                messageUtil.sendMessage(player, message);
                return;
            }
            targetUuid = offlinePlayer.getUniqueId();
        }

        String mainCommand = configManager.getString("settings.commands.main-command");

        switch (adminSubCommand) {
            case "enable":
                if (targetOffline) {
                    plugin.getDatabaseManager().setPlayerPhantomsStatusDirect(targetUuid, true);
                } else {
                    enablePlayer(targetPlayer);
                }
                String enableMsg = configManager.formatMessage(player, "admin.enable-success", "%player%", targetPlayerName);
                messageUtil.sendMessage(player, enableMsg);
                break;
            case "disable":
                if (targetOffline) {
                    plugin.getDatabaseManager().setPlayerPhantomsStatusDirect(targetUuid, false);
                } else {
                    disablePlayer(targetPlayer);
                }
                String disableMsg = configManager.formatMessage(player, "admin.disable-success", "%player%", targetPlayerName);
                messageUtil.sendMessage(player, disableMsg);
                break;
            case "status":
                boolean status;
                if (targetOffline) {
                    status = plugin.getDatabaseManager().getPlayerPhantomsStatusDirect(targetUuid);
                } else {
                    status = phantomManager.hasPhantomsEnabled(targetPlayer);
                }
                String statusText = status ? configManager.getMessage(player, "admin.status-enabled") : configManager.getMessage(player, "admin.status-disabled");
                String statusMsg = configManager.formatMessage(player, "admin.status", "%player%", targetPlayerName, "%status%", statusText);
                if (targetOffline) {
                    statusMsg += " " + configManager.getMessage(player, "admin.status-offline");
                }
                messageUtil.sendMessage(player, statusMsg);
                break;
            case "batch":
                handleBatchCommand(player, args);
                break;
            case "server":
                handleServerCommand(player, args);
                break;
            default:
                String invalidMsg = configManager.formatMessage(player, "admin.invalid-subcommand", "%maincommand%", mainCommand);
                messageUtil.sendMessage(player, invalidMsg);
                break;
        }
    }
    
    private void showHelp(Player player) {
        String mainCommand = configManager.getString("settings.commands.main-command");
        String reloadCommand = configManager.getString("settings.commands.reload-command");
        
        messageUtil.sendMessage(player,
            configManager.getMessage(player, "help.header") + "\n"
            + configManager.formatMessage(player, "help.enable", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.disable", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.toggle", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.status", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.gui", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.admin", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.batch", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.help", "%maincommand%", mainCommand) + "\n"
            + configManager.formatMessage(player, "help.reload", "%maincommand%", mainCommand, "%reloadcommand%", reloadCommand)
        );
    }
    
    private void handleBatchCommand(Player player, String[] args) {
        if (args.length < 4) {
            String mainCommand = configManager.getString("settings.commands.main-command");
            String batchUsage = configManager.formatMessage(player, "admin.batch-usage", "%maincommand%", mainCommand);
            messageUtil.sendMessage(player, batchUsage);
            return;
        }

        String batchSubCommand = args[2].toLowerCase();
        if (!batchSubCommand.equals("enable") && !batchSubCommand.equals("disable")) {
            String mainCommand = configManager.getString("settings.commands.main-command");
            String batchInvalid = configManager.formatMessage(player, "admin.batch-invalid-subcommand", "%maincommand%", mainCommand);
            messageUtil.sendMessage(player, batchInvalid);
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (int i = 3; i < args.length; i++) {
            String targetPlayerName = args[i];
            Player targetPlayer = org.bukkit.Bukkit.getPlayer(targetPlayerName);

            if (targetPlayer != null) {
                // 在线玩家：走正常流程
                if (batchSubCommand.equals("enable")) {
                    enablePlayer(targetPlayer);
                } else {
                    disablePlayer(targetPlayer);
                }
                successCount++;
            } else {
                // 尝试离线玩家
                OfflinePlayer offlinePlayer = resolveOfflinePlayer(targetPlayerName);
                if (offlinePlayer != null) {
                    boolean enabled = batchSubCommand.equals("enable");
                    plugin.getDatabaseManager().setPlayerPhantomsStatusDirect(offlinePlayer.getUniqueId(), enabled);
                    successCount++;
                } else {
                    failCount++;
                }
            }
        }

        String action = batchSubCommand.equals("enable")
            ? configManager.getMessage(player, "command.status_enabled", "Enabled")
            : configManager.getMessage(player, "command.status_disabled", "Disabled");
        String batchResult = configManager.formatMessage(player, "admin.batch-success",
            "%action%", action,
            "%success%", String.valueOf(successCount),
            "%fail%", String.valueOf(failCount));
        messageUtil.sendMessage(player, batchResult);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("管理员 " + player.getName() + " 批量" + action + "幻翼: 成功 " + successCount + " 个, 失败 " + failCount + " 个");
        }
    }
    
    private void enablePlayer(Player player) {
        phantomManager.enablePhantoms(player);
        String message = configManager.getMessage(player, "command.enabled");
        messageUtil.sendOnChange(player, message,
            configManager.getMessage(player, "command.enabled"),
            configManager.getMessage(player, "command.enabled"));
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
    }
    
    private void handleServerCommand(Player player, String[] args) {
        int totalPlayers = org.bukkit.Bukkit.getOnlinePlayers().size();
        int enabledCount = 0;
        int disabledCount = 0;
        
        for (Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (phantomManager.hasPhantomsEnabled(onlinePlayer)) {
                enabledCount++;
            } else {
                disabledCount++;
            }
        }
        
        double enabledPercentage = totalPlayers > 0 ? (double) enabledCount / totalPlayers * 100 : 0;
        double disabledPercentage = totalPlayers > 0 ? (double) disabledCount / totalPlayers * 100 : 0;
        
        messageUtil.sendMessage(player, configManager.getMessage(player, "admin.server-status-header"));
        messageUtil.sendMessage(player, configManager.formatMessage(player, "admin.server-status-total", "%count%", String.valueOf(totalPlayers)));
        messageUtil.sendMessage(player, configManager.formatMessage(player, "admin.server-status-enabled", 
            "%count%", String.valueOf(enabledCount), 
            "%percentage%", String.format("%.1f", enabledPercentage)));
        messageUtil.sendMessage(player, configManager.formatMessage(player, "admin.server-status-disabled", 
            "%count%", String.valueOf(disabledCount), 
            "%percentage%", String.format("%.1f", disabledPercentage)));
        
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("管理员 " + player.getName() + " 查看服务器幻翼状态: 总在线 " + totalPlayers + " 人, 启用 " + enabledCount + " 人, 禁用 " + disabledCount + " 人");
        }
    }

    /**
     * 通过玩家名解析离线玩家。
     * 返回 null 表示该玩家从未登录过服务器。
     */
    private OfflinePlayer resolveOfflinePlayer(String playerName) {
        OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return null;
        }
        return offlinePlayer;
    }
}

package yyz.chl.phantomcontrol.util;

import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.manager.ConfigManager;

public class MessageUtil {

    private final ConfigManager configManager;

    public MessageUtil(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void sendMessage(Player player, String message) {
        if (message == null || message.isEmpty()) return;

        switch (getDefaultType()) {
            case ACTION_BAR:
                sendActionBar(player, message);
                break;
            case TITLE:
                sendTitle(player, message);
                break;
            case CHAT:
            default:
                player.sendMessage(message);
                break;
        }
    }

    public void sendOnChange(Player player, String chatMessage, String titleMessage, String actionBarMessage) {
        sendMessage(player, chatMessage);

        if (configManager.getBoolean("settings.message.show-title-on-change", false) && titleMessage != null) {
            sendTitle(player, titleMessage);
        }

        if (configManager.getBoolean("settings.message.show-actionbar-on-change", false) && actionBarMessage != null) {
            sendActionBar(player, actionBarMessage);
        }
    }

    private MessageType getDefaultType() {
        String typeStr = configManager.getString("settings.message.default-type", "CHAT");
        try {
            return MessageType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageType.CHAT;
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(message);
        } catch (NoSuchMethodError e) {
            player.sendMessage(message);
        }
    }

    private void sendTitle(Player player, String message) {
        player.sendTitle("", message, 10, 70, 20);
    }

    public enum MessageType {
        CHAT,
        ACTION_BAR,
        TITLE
    }
}

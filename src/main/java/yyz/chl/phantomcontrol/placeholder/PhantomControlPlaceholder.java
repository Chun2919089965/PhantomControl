package yyz.chl.phantomcontrol.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;

public class PhantomControlPlaceholder extends PlaceholderExpansion {

    private final PhantomControl plugin;
    private final PhantomManager phantomManager;
    private final ConfigManager configManager;

    public PhantomControlPlaceholder(PhantomControl plugin, PhantomManager phantomManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.phantomManager = phantomManager;
        this.configManager = configManager;
    }

    @Override
    public String getIdentifier() {
        return "phantomcontrol";
    }

    @Override
    public String getAuthor() {
        return "CHL_chun";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "enabled":
                return String.valueOf(phantomManager.hasPhantomsEnabled(player));
            case "status":
                return phantomManager.hasPhantomsEnabled(player) 
                    ? configManager.getMessage(player, "command.status_enabled", "Enabled") 
                    : configManager.getMessage(player, "command.status_disabled", "Disabled");
            default:
                return null;
        }
    }
}

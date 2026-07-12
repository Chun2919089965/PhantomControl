package yyz.chl.phantomcontrol.listener;

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import yyz.chl.phantomcontrol.PhantomControl;
import yyz.chl.phantomcontrol.manager.ConfigManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;

/**
 * Paper 原生幻翼预生成拦截。
 * PhantomPreSpawnEvent 会直接提供触发幻翼生成的目标实体，
 * 不需要再通过生成位置推断玩家。
 */
public class PhantomSpawnListener implements Listener {

    private final PhantomManager phantomManager;
    private final ConfigManager configManager;
    private final PhantomControl plugin;

    public PhantomSpawnListener(PhantomControl plugin, PhantomManager phantomManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.phantomManager = phantomManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhantomPreSpawn(PhantomPreSpawnEvent event) {
        Entity spawningEntity = event.getSpawningEntity();
        if (!(spawningEntity instanceof Player target)) return;

        if (!phantomManager.isWorldAllowed(target.getWorld().getName())) return;

        if (!phantomManager.hasPhantomsEnabled(target)) {
            event.setCancelled(true);
            event.setShouldAbortSpawn(true);
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[幻翼拦截] 已拦截 " + target.getName() + " 的幻翼生成 ("
                        + event.getReason() + ")");
            }
        }
    }
}

package yyz.chl.phantomcontrol.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import yyz.chl.phantomcontrol.manager.PhantomManager;

/**
 * 玩家切换世界时立即刷新幻翼设置。
 * 避免玩家从黑名单世界传送到白名单世界时需等定时任务周期。
 */
public class PlayerWorldChangeListener implements Listener {

    private final PhantomManager phantomManager;

    public PlayerWorldChangeListener(PhantomManager phantomManager) {
        this.phantomManager = phantomManager;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        phantomManager.applyPhantomSettings(event.getPlayer());
    }
}

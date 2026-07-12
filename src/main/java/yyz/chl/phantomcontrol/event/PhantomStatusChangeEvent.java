package yyz.chl.phantomcontrol.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PhantomStatusChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final boolean enabled;

    public PhantomStatusChangeEvent(Player player, boolean enabled) {
        this.player = player;
        this.enabled = enabled;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

package yyz.chl.phantomcontrol.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import yyz.chl.phantomcontrol.api.PhantomStatusChangeSource;

public class PhantomStatusPreChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final boolean oldEnabled;
    private final boolean newEnabled;
    private final PhantomStatusChangeSource source;
    private boolean cancelled;

    public PhantomStatusPreChangeEvent(Player player, boolean oldEnabled, boolean newEnabled,
                                       PhantomStatusChangeSource source) {
        this.player = player;
        this.oldEnabled = oldEnabled;
        this.newEnabled = newEnabled;
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean wasEnabled() {
        return oldEnabled;
    }

    public boolean getOldEnabled() {
        return oldEnabled;
    }

    public boolean isEnabled() {
        return newEnabled;
    }

    public boolean getNewEnabled() {
        return newEnabled;
    }

    public boolean willBeEnabled() {
        return newEnabled;
    }

    public PhantomStatusChangeSource getSource() {
        return source;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

package yyz.chl.phantomcontrol.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PhantomSpawnBlockedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final String spawnReason;

    public PhantomSpawnBlockedEvent(Player player, String spawnReason) {
        this.player = player;
        this.spawnReason = spawnReason;
    }

    public Player getPlayer() {
        return player;
    }

    public String getSpawnReason() {
        return spawnReason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

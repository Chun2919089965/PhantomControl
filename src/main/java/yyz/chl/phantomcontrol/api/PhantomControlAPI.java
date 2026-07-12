package yyz.chl.phantomcontrol.api;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PhantomControlAPI {

    /**
     * Returns the cached status for an online player.
     */
    boolean arePhantomsEnabled(Player player);

    /**
     * Loads a player status without blocking the caller thread.
     */
    CompletableFuture<Boolean> arePhantomsEnabled(UUID playerId);

    /**
     * Changes an online player's status through the normal event pipeline.
     */
    boolean setPhantomsEnabled(Player player, boolean enabled);

    /**
     * Changes an online player's status and marks the source for listeners.
     */
    boolean setPhantomsEnabled(Player player, boolean enabled, PhantomStatusChangeSource source);

    /**
     * Stores a UUID status asynchronously. Online players should prefer the Player overload.
     */
    CompletableFuture<Boolean> setPhantomsEnabled(UUID playerId, boolean enabled);

    boolean enablePhantoms(Player player);

    boolean disablePhantoms(Player player);

    boolean canDisablePhantoms(Player player);

    boolean isWorldControlled(World world);
}

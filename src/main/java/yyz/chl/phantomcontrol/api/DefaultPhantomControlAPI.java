package yyz.chl.phantomcontrol.api;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import yyz.chl.phantomcontrol.manager.DatabaseManager;
import yyz.chl.phantomcontrol.manager.PhantomManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultPhantomControlAPI implements PhantomControlAPI {

    private final PhantomManager phantomManager;
    private final DatabaseManager databaseManager;

    public DefaultPhantomControlAPI(PhantomManager phantomManager, DatabaseManager databaseManager) {
        this.phantomManager = Objects.requireNonNull(phantomManager, "phantomManager");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
    }

    @Override
    public boolean arePhantomsEnabled(Player player) {
        Objects.requireNonNull(player, "player");
        return phantomManager.hasPhantomsEnabled(player);
    }

    @Override
    public CompletableFuture<Boolean> arePhantomsEnabled(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return databaseManager.getPlayerPhantomsStatusAsync(playerId);
    }

    @Override
    public boolean setPhantomsEnabled(Player player, boolean enabled) {
        return setPhantomsEnabled(player, enabled, PhantomStatusChangeSource.API);
    }

    @Override
    public boolean setPhantomsEnabled(Player player, boolean enabled, PhantomStatusChangeSource source) {
        Objects.requireNonNull(player, "player");
        return phantomManager.setPhantomsEnabled(player, enabled, source);
    }

    @Override
    public CompletableFuture<Boolean> setPhantomsEnabled(UUID playerId, boolean enabled) {
        Objects.requireNonNull(playerId, "playerId");
        Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(
                phantomManager.setPhantomsEnabled(onlinePlayer, enabled, PhantomStatusChangeSource.API));
        }
        return databaseManager.setPlayerPhantomsStatusAsync(playerId, enabled);
    }

    @Override
    public boolean enablePhantoms(Player player) {
        return setPhantomsEnabled(player, true, PhantomStatusChangeSource.API);
    }

    @Override
    public boolean disablePhantoms(Player player) {
        return setPhantomsEnabled(player, false, PhantomStatusChangeSource.API);
    }

    @Override
    public boolean canDisablePhantoms(Player player) {
        Objects.requireNonNull(player, "player");
        return phantomManager.canDisablePhantoms(player);
    }

    @Override
    public boolean isWorldControlled(World world) {
        Objects.requireNonNull(world, "world");
        return phantomManager.isWorldAllowed(world.getName());
    }
}

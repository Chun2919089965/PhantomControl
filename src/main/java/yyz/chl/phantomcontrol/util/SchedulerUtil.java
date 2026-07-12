package yyz.chl.phantomcontrol.util;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import yyz.chl.phantomcontrol.PhantomControl;

import java.util.concurrent.TimeUnit;

public class SchedulerUtil {

    private static volatile boolean isPluginEnabled = true;

    public static void setPluginEnabled(boolean enabled) {
        isPluginEnabled = enabled;
    }

    /**
     * Schedules an async repeating task.
     *
     * @param initialDelay initial delay in seconds
     * @param period repeating period in seconds
     */
    public static Object runAsyncTimer(Runnable task, int initialDelay, int period) {
        if (!isPluginEnabled) {
            return null;
        }

        AsyncScheduler scheduler = Bukkit.getAsyncScheduler();
        return scheduler.runAtFixedRate(
                getPlugin(),
                scheduledTask -> task.run(),
                Math.max(1L, initialDelay),
                Math.max(1L, period),
                TimeUnit.SECONDS);
    }

    public static void cancelTask(Object taskId) {
        if (taskId instanceof ScheduledTask scheduledTask) {
            scheduledTask.cancel();
        }
    }

    public static void shutdown() {
        PhantomControl plugin = PhantomControl.getInstance();
        if (plugin == null) {
            return;
        }

        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }

    private static PhantomControl getPlugin() {
        PhantomControl plugin = PhantomControl.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("PhantomControl plugin instance is not available");
        }
        return plugin;
    }
}

package yyz.chl.phantomcontrol.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import yyz.chl.phantomcontrol.PhantomControl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SchedulerUtil {
    
    private static volatile boolean isPluginEnabled = true;
    
    public static void setPluginEnabled(boolean enabled) {
        isPluginEnabled = enabled;
    }
    
    public static boolean isPluginEnabled() {
        return isPluginEnabled;
    }
    
    private interface SchedulerStrategy {
        void runAsync(Runnable task);
        Object runAsyncTimer(Runnable task, int initialDelay, int period);
        Object runSyncTimer(Runnable task, int initialDelay, int period);
        void runSync(Runnable task);
        void runSyncLater(Runnable task, int delay);
        void cancelTask(Object taskId);
        void shutdown();
    }
    
    private static class FoliaSchedulerStrategy implements SchedulerStrategy {
        private static final java.lang.reflect.Method ASYNC_RUN_NOW;
        private static final java.lang.reflect.Method ASYNC_RUN_AT_FIXED_RATE;
        private static final java.lang.reflect.Method GLOBAL_RUN;
        private static final java.lang.reflect.Method GLOBAL_RUN_AT_FIXED_RATE;
        private static final java.lang.reflect.Method GLOBAL_RUN_DELAYED;
        private static final java.lang.reflect.Method SCHEDULED_TASK_CANCEL;
        
        static {
            java.lang.reflect.Method asyncRunNow = null;
            java.lang.reflect.Method asyncRunAtFixedRate = null;
            java.lang.reflect.Method globalRun = null;
            java.lang.reflect.Method globalRunAtFixedRate = null;
            java.lang.reflect.Method globalRunDelayed = null;
            java.lang.reflect.Method scheduledTaskCancel = null;
            try {
                Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                asyncRunNow = asyncSchedulerClass.getMethod("runNow", org.bukkit.plugin.Plugin.class, Runnable.class);
                asyncRunAtFixedRate = asyncSchedulerClass.getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, Runnable.class, long.class, long.class, TimeUnit.class);
                
                Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
                globalRun = regionSchedulerClass.getMethod("run", org.bukkit.plugin.Plugin.class, Runnable.class);
                globalRunAtFixedRate = regionSchedulerClass.getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, Runnable.class, long.class, long.class, TimeUnit.class);
                globalRunDelayed = regionSchedulerClass.getMethod("runDelayed", org.bukkit.plugin.Plugin.class, Runnable.class, long.class);
                
                Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                scheduledTaskCancel = scheduledTaskClass.getMethod("cancel");
            } catch (Exception ignored) {
            }
            ASYNC_RUN_NOW = asyncRunNow;
            ASYNC_RUN_AT_FIXED_RATE = asyncRunAtFixedRate;
            GLOBAL_RUN = globalRun;
            GLOBAL_RUN_AT_FIXED_RATE = globalRunAtFixedRate;
            GLOBAL_RUN_DELAYED = globalRunDelayed;
            SCHEDULED_TASK_CANCEL = scheduledTaskCancel;
        }
        
        private final ExecutorService fallbackExecutor = Executors.newCachedThreadPool();
        private final Object asyncScheduler;
        private final Object globalRegionScheduler;
        private final List<ScheduledExecutorService> scheduledExecutors = new ArrayList<>();
        
        public FoliaSchedulerStrategy() {
            Object asyncScheduler = null;
            Object globalRegionScheduler = null;
            
            try {
                Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                asyncScheduler = bukkitClass.getMethod("getAsyncScheduler").invoke(null);
                globalRegionScheduler = bukkitClass.getMethod("getGlobalRegionScheduler").invoke(null);
            } catch (Exception e) {
            }
            
            this.asyncScheduler = asyncScheduler;
            this.globalRegionScheduler = globalRegionScheduler;
        }
        
        @Override
        public void runAsync(Runnable task) {
            PhantomControl plugin = PhantomControl.getInstance();
            
            if (ASYNC_RUN_NOW != null && asyncScheduler != null) {
                try {
                    ASYNC_RUN_NOW.invoke(asyncScheduler, plugin, (Runnable) () -> task.run());
                    return;
                } catch (Exception e) {
                    if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException) {
                        fallbackExecutor.execute(task);
                        return;
                    }
                }
            }
            
            fallbackExecutor.execute(task);
        }
        
        @Override
        public Object runAsyncTimer(Runnable task, int initialDelay, int period) {
            PhantomControl plugin = PhantomControl.getInstance();
            
            if (ASYNC_RUN_AT_FIXED_RATE != null && asyncScheduler != null) {
                try {
                    return ASYNC_RUN_AT_FIXED_RATE.invoke(asyncScheduler, plugin, (Runnable) () -> task.run(), (long) initialDelay, (long) period, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }
            
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            synchronized (scheduledExecutors) {
                scheduledExecutors.add(executor);
            }
            return executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
        }
        
        @Override
        public void runSync(Runnable task) {
            PhantomControl plugin = PhantomControl.getInstance();
            
            if (GLOBAL_RUN != null && globalRegionScheduler != null) {
                try {
                    GLOBAL_RUN.invoke(globalRegionScheduler, plugin, (Runnable) () -> task.run());
                    return;
                } catch (Exception e) {
                }
            }
            
            fallbackExecutor.execute(task);
        }
        
        @Override
        public Object runSyncTimer(Runnable task, int initialDelay, int period) {
            PhantomControl plugin = PhantomControl.getInstance();
            
            if (GLOBAL_RUN_AT_FIXED_RATE != null && globalRegionScheduler != null) {
                try {
                    return GLOBAL_RUN_AT_FIXED_RATE.invoke(globalRegionScheduler, plugin, (Runnable) () -> task.run(), (long) initialDelay, (long) period, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }
            
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            synchronized (scheduledExecutors) {
                scheduledExecutors.add(executor);
            }
            return executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
        }
        
        @Override
        public void runSyncLater(Runnable task, int delay) {
            PhantomControl plugin = PhantomControl.getInstance();
            
            if (GLOBAL_RUN_DELAYED != null && globalRegionScheduler != null) {
                try {
                    GLOBAL_RUN_DELAYED.invoke(globalRegionScheduler, plugin, (Runnable) () -> task.run(), (long) delay * 20L);
                    return;
                } catch (Exception e) {
                }
            }
            
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            synchronized (scheduledExecutors) {
                scheduledExecutors.add(executor);
            }
            executor.schedule(task, delay, TimeUnit.SECONDS);
        }
        
        @Override
        public void cancelTask(Object taskId) {
            if (taskId == null) {
                return;
            }
            
            if (taskId instanceof ScheduledFuture) {
                ((ScheduledFuture<?>) taskId).cancel(true);
                return;
            }
            
            if (SCHEDULED_TASK_CANCEL != null) {
                try {
                    SCHEDULED_TASK_CANCEL.invoke(taskId);
                } catch (Exception ignored) {
                }
            }
        }
        
        @Override
        public void shutdown() {
            synchronized (scheduledExecutors) {
                for (ScheduledExecutorService executor : scheduledExecutors) {
                    executor.shutdown();
                }
                scheduledExecutors.clear();
            }
            fallbackExecutor.shutdown();
        }
    }
    
    private static class BukkitSchedulerStrategy implements SchedulerStrategy {
        private final ExecutorService fallbackExecutor = Executors.newCachedThreadPool();
        private final org.bukkit.scheduler.BukkitScheduler bukkitScheduler;
        private final List<ScheduledExecutorService> scheduledExecutors = new ArrayList<>();
        
        public BukkitSchedulerStrategy() {
            this.bukkitScheduler = Bukkit.getScheduler();
        }
        
        @Override
        public void runAsync(Runnable task) {
            PhantomControl plugin = PhantomControl.getInstance();
            
            try {
                bukkitScheduler.runTaskAsynchronously(plugin, task);
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("not enabled")) {
                    fallbackExecutor.execute(task);
                }
            } catch (Exception e) {
                fallbackExecutor.execute(task);
            }
        }
        
        @Override
        public Object runAsyncTimer(Runnable task, int initialDelay, int period) {
            PhantomControl plugin = PhantomControl.getInstance();
            long initialDelayTicks = initialDelay * 20L;
            long periodTicks = period * 20L;
            
            try {
                return bukkitScheduler.runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks);
            } catch (Exception e) {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                synchronized (scheduledExecutors) {
                    scheduledExecutors.add(executor);
                }
                return executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
            }
        }
        
        @Override
        public Object runSyncTimer(Runnable task, int initialDelay, int period) {
            PhantomControl plugin = PhantomControl.getInstance();
            long initialDelayTicks = initialDelay * 20L;
            long periodTicks = period * 20L;
            
            try {
                return bukkitScheduler.runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            } catch (Exception e) {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                synchronized (scheduledExecutors) {
                    scheduledExecutors.add(executor);
                }
                return executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
            }
        }
        
        @Override
        public void runSync(Runnable task) {
            PhantomControl plugin = PhantomControl.getInstance();
            try {
                bukkitScheduler.runTask(plugin, task);
            } catch (Exception e) {
                fallbackExecutor.execute(task);
            }
        }
        
        @Override
        public void runSyncLater(Runnable task, int delay) {
            PhantomControl plugin = PhantomControl.getInstance();
            long delayTicks = delay * 20L;
            
            try {
                bukkitScheduler.runTaskLater(plugin, task, delayTicks);
            } catch (Exception e) {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                synchronized (scheduledExecutors) {
                    scheduledExecutors.add(executor);
                }
                executor.schedule(task, delay, TimeUnit.SECONDS);
            }
        }
        
        @Override
        public void cancelTask(Object taskId) {
            if (taskId == null) {
                return;
            }
            
            if (taskId instanceof ScheduledFuture) {
                ((ScheduledFuture<?>) taskId).cancel(true);
                return;
            }
            
            if (taskId instanceof BukkitTask) {
                ((BukkitTask) taskId).cancel();
            }
        }
        
        @Override
        public void shutdown() {
            synchronized (scheduledExecutors) {
                for (ScheduledExecutorService executor : scheduledExecutors) {
                    executor.shutdown();
                }
                scheduledExecutors.clear();
            }
            fallbackExecutor.shutdown();
        }
    }
    
    private static final SchedulerStrategy schedulerStrategy;
    
    static {
        SchedulerStrategy strategy;
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            strategy = new FoliaSchedulerStrategy();
        } catch (ClassNotFoundException e) {
            strategy = new BukkitSchedulerStrategy();
        }
        
        schedulerStrategy = strategy;
    }
    
    public static void runAsync(Runnable task) {
        if (!isPluginEnabled) {
            return;
        }
        schedulerStrategy.runAsync(task);
    }
    
    public static Object runAsyncTimer(Runnable task, int initialDelay, int period) {
        if (!isPluginEnabled) {
            return null;
        }
        return schedulerStrategy.runAsyncTimer(task, initialDelay, period);
    }
    
    public static Object runSyncTimer(Runnable task, int initialDelay, int period) {
        if (!isPluginEnabled) {
            return null;
        }
        return schedulerStrategy.runSyncTimer(task, initialDelay, period);
    }
    
    public static void runSync(Runnable task) {
        if (!isPluginEnabled) {
            return;
        }
        schedulerStrategy.runSync(task);
    }
    
    public static void runSyncLater(Runnable task, int delay) {
        if (!isPluginEnabled) {
            return;
        }
        schedulerStrategy.runSyncLater(task, delay);
    }
    
    public static void cancelTask(Object taskId) {
        schedulerStrategy.cancelTask(taskId);
    }
    
    public static void shutdown() {
        schedulerStrategy.shutdown();
    }
}
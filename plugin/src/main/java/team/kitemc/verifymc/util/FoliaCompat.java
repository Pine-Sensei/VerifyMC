package team.kitemc.verifymc.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaCompat {

    private static final boolean FOLIA;
    private static final Method GET_ASYNC_SCHEDULER;
    private static final Method ASYNC_RUN_AT_FIXED_RATE;
    private static final Method GET_GLOBAL_REGION_SCHEDULER;
    private static final Method GLOBAL_RUN;
    private static final Method SCHEDULED_TASK_CANCEL;

    static {
        boolean folia = false;
        Method getAsyncScheduler = null;
        Method asyncRunAtFixedRate = null;
        Method getGlobalRegionScheduler = null;
        Method globalRun = null;
        Method scheduledTaskCancel = null;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;

            getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            asyncRunAtFixedRate = asyncSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);

            getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Class<?> globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            globalRun = globalRegionSchedulerClass.getMethod("run", Plugin.class, Consumer.class);

            Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            scheduledTaskCancel = scheduledTaskClass.getMethod("cancel");
        } catch (Exception ignored) {
        }

        FOLIA = folia;
        GET_ASYNC_SCHEDULER = getAsyncScheduler;
        ASYNC_RUN_AT_FIXED_RATE = asyncRunAtFixedRate;
        GET_GLOBAL_REGION_SCHEDULER = getGlobalRegionScheduler;
        GLOBAL_RUN = globalRun;
        SCHEDULED_TASK_CANCEL = scheduledTaskCancel;
    }

    private FoliaCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static Object runTaskTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            try {
                Object asyncScheduler = GET_ASYNC_SCHEDULER.invoke(null);
                long delayMs = ticksToMs(delayTicks);
                long periodMs = ticksToMs(periodTicks);
                Consumer<Object> wrappedTask = scheduledTask -> task.run();
                return ASYNC_RUN_AT_FIXED_RATE.invoke(asyncScheduler, plugin, wrappedTask, delayMs, periodMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                plugin.getLogger().severe("[VerifyMC] Folia async scheduler failed: " + e.getMessage());
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runTaskGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) {
            try {
                Object globalScheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(null);
                Consumer<Object> wrappedTask = scheduledTask -> task.run();
                GLOBAL_RUN.invoke(globalScheduler, plugin, wrappedTask);
            } catch (Exception e) {
                plugin.getLogger().severe("[VerifyMC] Folia global scheduler failed: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void cancelTasks(Plugin plugin, List<Object> tasks) {
        if (tasks == null) return;
        for (Object task : tasks) {
            cancelTask(task);
        }
        tasks.clear();
    }

    public static void cancelTask(Object task) {
        if (task == null) return;
        try {
            if (FOLIA) {
                if (task instanceof java.util.concurrent.Future) {
                    ((java.util.concurrent.Future<?>) task).cancel(false);
                } else {
                    SCHEDULED_TASK_CANCEL.invoke(task);
                }
            } else {
                if (task instanceof BukkitTask) {
                    ((BukkitTask) task).cancel();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static long ticksToMs(long ticks) {
        return ticks * 50L;
    }
}

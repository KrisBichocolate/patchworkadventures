package krisbichocolate.patchworkadventures.dimensions;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class TickScheduler {
    private static final List<ScheduledTask> activeTasks = new CopyOnWriteArrayList<>();
    private static final Queue<ScheduledTask> pendingTasks = new ConcurrentLinkedQueue<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> TickScheduler.tick());
    }

    public static void schedule(int ticksDelay, Runnable task) {
        if (ticksDelay < 0 || task == null) return;
        pendingTasks.add(new ScheduledTask(ticksDelay, task));
    }

    private static void tick() {
        // Move pending tasks to active list
        ScheduledTask task;
        while ((task = pendingTasks.poll()) != null) {
            activeTasks.add(task);
        }

        // Tick and collect completed tasks
        List<ScheduledTask> completed = new ArrayList<>();
        for (ScheduledTask t : activeTasks) {
            if (t.tick()) {
                completed.add(t);
            }
        }

        // Remove completed tasks
        activeTasks.removeAll(completed);
    }

    private static class ScheduledTask {
        private int ticksRemaining;
        private final Runnable task;

        ScheduledTask(int ticksDelay, Runnable task) {
            this.ticksRemaining = ticksDelay;
            this.task = task;
        }

        boolean tick() {
            if (--ticksRemaining <= 0) {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        }
    }
}
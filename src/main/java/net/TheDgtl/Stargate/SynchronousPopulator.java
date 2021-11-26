package net.TheDgtl.Stargate;

import net.TheDgtl.Stargate.actions.PopulatorAction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Cycles through a queue of actions everytime the {@link SynchronousPopulator#run()} function is triggered.
 *
 * <p>If used with the {@link org.bukkit.scheduler.BukkitScheduler#scheduleSyncRepeatingTask()} function, you can use
 * this as a handy way to do synchronous tasks (tasks that happens during a specific tick).
 * Warning: Running this once, even by running forceDoAllTasks does not guarantee all tasks to finish.</p>
 *
 * @author Thorin
 */
public class SynchronousPopulator implements Runnable {

    private final List<PopulatorAction> populatorQueue = new ArrayList<>();
    private final List<PopulatorAction> bungeePopulatorQueue = new ArrayList<>();
    private final List<PopulatorAction> addList = new ArrayList<>();
    private final List<PopulatorAction> bungeeAddList = new ArrayList<>();

    @Override
    public void run() {
        populatorQueue.addAll(addList);
        addList.clear();

        if (Stargate.knowsServerName) {
            bungeePopulatorQueue.addAll(bungeeAddList);
            bungeeAddList.clear();
        }

        cycleQueues(false);
    }

    /**
     * Adds a non-bungee populator action to the queue
     *
     * @param action <p>The action to add</p>
     */
    public void addAction(PopulatorAction action) {
        addAction(action, false);
    }

    /**
     * Adds a populator action to the queue
     *
     * @param action   <p>The action to add</p>
     * @param isBungee <p>Whether the action is to be performed on a BungeeCord portal</p>
     */
    public void addAction(PopulatorAction action, boolean isBungee) {
        if (action != null) {
            Stargate.log(Level.FINEST, "Adding action " + action);
        }
        (isBungee ? this.bungeeAddList : this.addList).add(action);
    }

    /**
     * Force all populator tasks to be performed
     */
    public void forceDoAllTasks() {
        cycleQueues(true);
    }

    /**
     * Cycle through all queues and perform necessary actions
     *
     * @param forceAction <p>Whether to force the actions to run, regardless of anything that might prevent them</p>
     */
    private void cycleQueues(boolean forceAction) {
        cycleQueue(populatorQueue, forceAction);
        if (!Stargate.knowsServerName) {
            return;
        }
        cycleQueue(bungeePopulatorQueue, forceAction);
    }

    /**
     * Goes through a populator action queue and runs the action
     *
     * <p>If more than 25 milliseconds pass, the cycling will stop. This is mainly a protection against infinite
     * loops, but it will also reduce lag when there are many actions in the queue.</p>
     *
     * @param queue       <p>The queue to cycle through</p>
     * @param forceAction <p>Whether to force the actions to run, regardless of anything that might prevent them</p>
     */
    private void cycleQueue(List<PopulatorAction> queue, boolean forceAction) {
        Iterator<PopulatorAction> iterator = queue.iterator();
        long initialSystemTime = System.nanoTime();

        //Go through all populator actions until 25 milliseconds have passed, or the queue is empty
        while (iterator.hasNext() && (System.nanoTime() - initialSystemTime < 25000000)) {
            PopulatorAction action = iterator.next();
            action.run(forceAction);
            if (action.isFinished()) {
                iterator.remove();
            }
        }
    }

}

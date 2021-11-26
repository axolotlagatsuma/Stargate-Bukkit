package net.TheDgtl.Stargate.actions;

import org.bukkit.block.BlockState;

/**
 * Represents the action of changing the state of a block
 */
public class BlockSetAction implements PopulatorAction {

    private final BlockState state;
    private final boolean force;
    private boolean isFinished = false;

    /**
     * Instantiates a new block state action
     *
     * @param state <p>The new block state of a block</p>
     * @param force <p>Whether to forcefully set the state, even if the block's type has changed</p>
     */
    public BlockSetAction(BlockState state, boolean force) {
        this.state = state;
        this.force = force;
    }

    @Override
    public void run(boolean forceEnd) {
        state.update(force || forceEnd);
        isFinished = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public String toString() {
        return state.toString();
    }

}
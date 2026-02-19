package net.nicotfpn.alientech.machine.core;

/**
 * Defines the process contract for a machine.
 * Each concrete machine implements this to provide its recipe/crafting logic
 * to the MachineProcessor without coupling to specific recipe types.
 */
public interface IMachineProcess {

    /**
     * Whether the machine can currently process (valid recipe + output space).
     */
    boolean canProcess();

    /**
     * Called when processing completes. Consume inputs and produce outputs here.
     */
    void onProcessComplete();

    /**
     * Total ticks required to complete one processing operation.
     */
    int getProcessTime();

    /**
     * Energy cost per tick while processing (FE/t).
     */
    int getEnergyCost();
}

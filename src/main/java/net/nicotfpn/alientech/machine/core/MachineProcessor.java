package net.nicotfpn.alientech.machine.core;

import net.minecraft.nbt.CompoundTag;

/**
 * Encapsulates processing progress logic for a machine.
 * Tracks progress, advances/resets based on IMachineProcess + MachineEnergy
 * state.
 *
 * NBT key preserved: "Progress"
 */
public class MachineProcessor {

    private int progress = 0;
    private final Runnable onChanged;

    /**
     * @param onChanged callback for dirty marking when progress changes
     */
    public MachineProcessor(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    // ==================== Accessors ====================

    public int getProgress() {
        return progress;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    /**
     * Set progress directly. Used by client-side ContainerData sync.
     */
    public void setProgress(int progress) {
        this.progress = progress;
    }

    // ==================== Tick Logic ====================

    /**
     * Advance or reset processing progress based on the current process and energy
     * state.
     *
     * Behavior (matches original AbstractMachineBlockEntity exactly):
     * - If canProcess AND hasPower: consume energy, advance progress, craft on
     * completion
     * - If canProcess AND NO power: reset progress
     * - If cannotProcess: reset progress
     *
     * @param process the machine's process definition
     * @param energy  the machine's energy component
     */
    public boolean tick(IMachineProcess process, MachineEnergy energy) {
        if (process.canProcess()) {
            if (energy.hasPower(process.getEnergyCost())) {
                // Consume energy (priority: FE first, fuel ticks down passively)
                energy.consumePower(process.getEnergyCost());

                // Advance progress
                progress++;
                onChanged.run();

                // Check completion
                if (progress >= process.getProcessTime()) {
                    process.onProcessComplete();
                    progress = 0;
                }
                return true;
            } else {
                // No power — reset progress
                resetProgress();
            }
        } else {
            // No valid recipe — reset progress
            resetProgress();
        }
        return false;
    }

    private void resetProgress() {
        if (progress > 0) {
            progress = 0;
            onChanged.run();
        }
    }

    // ==================== NBT Persistence ====================

    public void save(CompoundTag tag) {
        tag.putInt("Progress", progress);
    }

    public void load(CompoundTag tag) {
        progress = net.nicotfpn.alientech.util.StateValidator.ensureNonNegative(
                net.nicotfpn.alientech.util.SafeNBT.getInt(tag, "Progress", 0));
    }
}

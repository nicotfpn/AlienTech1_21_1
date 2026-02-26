package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;

import java.util.function.IntSupplier;

/**
 * Handles processing logic, ticks, and state changes.
 * This component stops ticking when 'isWorking' is false, optimizing TPS.
 */
public class ProcessingComponent extends AlienComponent {

    private boolean isWorking;
    private int progress;
    private final IntSupplier maxProgressSupplier;
    private final Runnable onComplete;

    public ProcessingComponent(AlienMachineBlockEntity tile, IntSupplier maxProgressSupplier, Runnable onComplete) {
        super(tile);
        this.maxProgressSupplier = maxProgressSupplier;
        this.onComplete = onComplete;
        this.isWorking = false; // Idle by default
        this.progress = 0;
    }

    @Override
    public String getId() {
        return "Processing";
    }

    @Override
    public boolean isActive() {
        return isWorking; // Only tick if processing
    }

    public void setWorking(boolean working) {
        if (this.isWorking != working) {
            this.isWorking = working;
            markActiveStateDirty(); // Adds or removes from active ticking list
        }
    }

    public boolean isWorking() {
        return isWorking;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        this.tile.setChanged();
    }

    public void resetProgress() {
        this.progress = 0;
        this.tile.setChanged();
    }

    public int getMaxProgress() {
        return maxProgressSupplier.getAsInt();
    }

    @Override
    public boolean tickServer() {
        if (!isWorking)
            return false;

        progress++;
        if (progress >= getMaxProgress()) {
            progress = 0;
            if (onComplete != null) {
                onComplete.run();
            }
        }
        return true;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("IsWorking", this.isWorking);
        tag.putInt("Progress", this.progress);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("IsWorking")) {
            this.setWorking(tag.getBoolean("IsWorking")); // Ensure state is restored actively
        }
        if (tag.contains("Progress")) {
            this.progress = tag.getInt("Progress");
        }
    }
}

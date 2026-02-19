package net.nicotfpn.alientech.machine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Centralizes tick orchestration for all machine components.
 * Ensures the exact same execution order as the original monolithic
 * implementation:
 *
 * 1. Try consume fuel (only when needed)
 * 2. Process recipe (advance/reset progress)
 * 3. Tick down fuel burn time
 * 4. Auto-push outputs to adjacent inventories
 *
 * Constructed once per machine with its specific fuel/output configuration.
 */
public class MachineTicker {

    private final int fuelSlot;
    private final Predicate<ItemStack> fuelValidator;
    private final ToIntFunction<ItemStack> burnTimeFunction;
    private final int[] outputSlots;
    private final SlotAccessRules rules;

    /**
     * @param fuelSlot         slot index for fuel (-1 if no fuel slot)
     * @param fuelValidator    predicate to check if an item is valid fuel
     * @param burnTimeFunction function returning burn time in ticks for a fuel item
     * @param outputSlots      indices of output slots for auto-push
     * @param rules            slot access rules for sided automation
     */
    public MachineTicker(int fuelSlot, Predicate<ItemStack> fuelValidator,
            ToIntFunction<ItemStack> burnTimeFunction,
            int[] outputSlots, SlotAccessRules rules) {
        this.fuelSlot = fuelSlot;
        this.fuelValidator = fuelValidator;
        this.burnTimeFunction = burnTimeFunction;
        this.outputSlots = outputSlots;
        this.rules = rules;
    }

    /**
     * Execute one server tick for all machine components.
     * Order matches original AbstractMachineBlockEntity.onUpdateServer() exactly.
     */
    public void tickServer(MachineInventory inventory, MachineEnergy energy,
            MachineProcessor processor, MachineAutomation automation,
            IMachineProcess process, Level level, BlockPos pos) {

        // Step 1: Try to consume fuel if needed
        // Only when: no burn time, insufficient energy for the cost, and recipe
        // available
        if (fuelSlot >= 0 && !energy.isBurning()
                && !energy.hasPower(process.getEnergyCost()) && process.canProcess()) {
            energy.tryConsumeFuel(inventory, fuelSlot, fuelValidator, burnTimeFunction);
        }

        // Step 2: Process recipe (advance progress, consume energy, craft on
        // completion)
        processor.tick(process, energy);

        // Step 3: Tick down fuel burn time
        energy.tickBurnTime();

        // Step 4: Auto-push outputs to adjacent inventories
        automation.autoPushOutputs(level, pos, inventory, outputSlots, rules);
    }
}

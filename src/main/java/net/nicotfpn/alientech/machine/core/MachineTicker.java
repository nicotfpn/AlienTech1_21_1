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
    private final int energyPushRate;
    private final int energyPullRate;

    /**
     * @param fuelSlot         slot index for fuel (-1 if no fuel slot)
     * @param fuelValidator    predicate to check if an item is valid fuel
     * @param burnTimeFunction function returning burn time in ticks for a fuel item
     * @param outputSlots      indices of output slots for auto-push
     * @param rules            slot access rules for sided automation
     * @param energyPushRate   max FE/t pushed to neighbors
     * @param energyPullRate   max FE/t pulled from neighbors
     */
    public MachineTicker(int fuelSlot, Predicate<ItemStack> fuelValidator,
            ToIntFunction<ItemStack> burnTimeFunction,
            int[] outputSlots, SlotAccessRules rules,
            int energyPushRate, int energyPullRate) {
        this.fuelSlot = fuelSlot;
        this.fuelValidator = fuelValidator;
        this.burnTimeFunction = burnTimeFunction;
        this.outputSlots = outputSlots;
        this.rules = rules;
        this.energyPushRate = energyPushRate;
        this.energyPullRate = energyPullRate;
    }

    // Fractional ticks accumulator to support non-integer speed multipliers
    private double fractionalAccumulator = 0.0;

    /**
     * Execute one server tick for all machine components.
     * Order matches original AbstractMachineBlockEntity.onUpdateServer() exactly.
     */
    public int tickServer(MachineInventory inventory, MachineEnergy energy,
            MachineProcessor processor, MachineAutomation automation,
            IMachineProcess process, Level level, BlockPos pos, float speedMultiplier) {

        // Step 1: Try to consume fuel if needed
        // Only when: no burn time, insufficient energy for the cost, and recipe
        // available
        if (fuelSlot >= 0 && !energy.isBurning()
                && !energy.hasPower(process.getEnergyCost()) && process.canProcess()) {
            energy.tryConsumeFuel(inventory, fuelSlot, fuelValidator, burnTimeFunction);
        }

        // Step 2: Process recipe (advance progress, consume energy, craft on
        // completion)
        // Apply speed multiplier by advancing the processor multiple times.
        // Fractional multipliers are accumulated across ticks.
        int integerTicks = (int) Math.floor(Math.max(0.0f, speedMultiplier));
        double fractional = Math.max(0.0f, speedMultiplier) - integerTicks;
        fractionalAccumulator += fractional;
        if (fractionalAccumulator >= 1.0) {
            integerTicks += (int) Math.floor(fractionalAccumulator);
            fractionalAccumulator -= Math.floor(fractionalAccumulator);
        }

        // Execute processor.tick() integerTicks times (may be zero).
        int successfulTicks = 0;
        int energyCost = process.getEnergyCost();

        for (int i = 0; i < integerTicks; i++) {
            // Fast-fail loop optimization: instantly break if we can't process anymore
            // Prevents executing redundant loop logic for empty machines
            if (!process.canProcess() && !energy.hasPower(energyCost)) {
                break;
            }

            if (processor.tick(process, energy)) {
                successfulTicks++;
            }
        }

        // Step 3: Tick down fuel burn time
        energy.tickBurnTime();

        // Step 4: Auto-push/pull energy to adjacent blocks
        if (energyPushRate > 0) {
            automation.autoPushEnergy(level, pos, energy, energyPushRate);
        }
        if (energyPullRate > 0) {
            automation.autoPullEnergy(level, pos, energy, energyPullRate);
        }

        // Step 5: Auto-push outputs to adjacent inventories
        automation.autoPushOutputs(level, pos, inventory, outputSlots, rules);

        return successfulTicks;
    }
}

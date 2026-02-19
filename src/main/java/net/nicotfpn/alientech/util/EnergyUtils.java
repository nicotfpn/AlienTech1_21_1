package net.nicotfpn.alientech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * High-performance utility for energy transfer operations.
 * Designed to minimize overhead and ensure consistent behavior across the mod.
 */
public final class EnergyUtils {

    private EnergyUtils() {
        // Prevent instantiation
    }

    /**
     * Pushes energy from a source to a target.
     * 
     * @param source      The source energy storage.
     * @param target      The target energy storage.
     * @param maxTransfer Maximum amount to transfer.
     * @return The amount of energy actually transferred.
     */
    public static int pushEnergy(IEnergyStorage source, IEnergyStorage target, int maxTransfer) {
        if (maxTransfer <= 0)
            return 0;

        int simulatedExtract = source.extractEnergy(maxTransfer, true);
        if (simulatedExtract <= 0)
            return 0;

        int simulatedReceive = target.receiveEnergy(simulatedExtract, true);
        if (simulatedReceive <= 0)
            return 0;

        // Perform actual transfer
        int extracted = source.extractEnergy(simulatedReceive, false);
        return target.receiveEnergy(extracted, false);
    }

    /**
     * Pulls energy from a target to a receiver.
     * 
     * @param receiver    The receiver energy storage (us).
     * @param target      The target energy storage (them).
     * @param maxTransfer Maximum amount to transfer.
     * @return The amount of energy actually transferred.
     */
    public static int pullEnergy(IEnergyStorage receiver, IEnergyStorage target, int maxTransfer) {
        if (maxTransfer <= 0)
            return 0;

        int simulatedExtract = target.extractEnergy(maxTransfer, true);
        if (simulatedExtract <= 0)
            return 0;

        int simulatedReceive = receiver.receiveEnergy(simulatedExtract, true);
        if (simulatedReceive <= 0)
            return 0;

        // Perform actual transfer
        int extracted = target.extractEnergy(simulatedReceive, false);
        return receiver.receiveEnergy(extracted, false);
    }

    /**
     * Tries to push energy to a block capability at the given position.
     * 
     * @param level       The level.
     * @param targetPos   The target position.
     * @param source      The source storage.
     * @param maxTransfer Max transfer amount.
     * @return Amount transferred.
     */
    public static int pushToPosition(Level level, BlockPos targetPos, @Nullable Direction side, IEnergyStorage source,
            int maxTransfer) {
        IEnergyStorage targetCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, side);
        if (targetCap == null)
            return 0;
        return pushEnergy(source, targetCap, maxTransfer);
    }

    /**
     * Charges an item from an energy source amount.
     * 
     * @param stack  The item stack to charge.
     * @param amount The amount of energy available to charge.
     * @return The amount of energy actually accepted by the item.
     */
    public static int chargeItem(net.minecraft.world.item.ItemStack stack, int amount) {
        if (stack.isEmpty())
            return 0;
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy == null || !itemEnergy.canReceive())
            return 0;
        return itemEnergy.receiveEnergy(amount, false);
    }

    /**
     * Discharges an item into an energy sink amount.
     * 
     * @param stack  The item stack to discharge.
     * @param amount The amount of energy the sink can accept.
     * @return The amount of energy actually extracted from the item.
     */
    public static int dischargeItem(net.minecraft.world.item.ItemStack stack, int amount) {
        if (stack.isEmpty())
            return 0;
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy == null || !itemEnergy.canExtract())
            return 0;
        return itemEnergy.extractEnergy(amount, false);
    }

    /**
     * Formats a large number into a compact string with suffixes (k, M, G, T).
     * Example: 1500 -> "1.5 k", 2000000 -> "2 M"
     * 
     * @param value The value to format.
     * @return Compact string representation.
     */
    public static String formatCompact(long value) {
        if (value < 1000)
            return String.valueOf(value);
        int exp = (int) (Math.log(value) / Math.log(1000));
        String suffix = "kMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.ROOT, "%.1f %s", value / Math.pow(1000, exp), suffix);
    }
}

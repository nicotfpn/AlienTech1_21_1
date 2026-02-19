package net.nicotfpn.alientech.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.text.NumberFormat;
import java.util.List;

/**
 * Activated Eye of Horus - Portable battery with 5M FE capacity.
 * Can receive energy from any source and stores it internally.
 */
public class ActivatedEyeOfHorusItem extends Item {
    public static final int MAX_ENERGY = 5_000_000; // 5M FE

    public ActivatedEyeOfHorusItem(Properties properties) {
        super(properties);
    }

    /**
     * Custom bar color (golden/orange for energy)
     */
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFAA00; // Golden orange
    }

    /**
     * Show energy bar based on stored energy
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return energy != null && energy.getEnergyStored() < energy.getMaxEnergyStored();
    }

    /**
     * Calculate bar width based on energy (0-13)
     */
    @Override
    public int getBarWidth(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy == null)
            return 0;
        return Math.round(13.0f * energy.getEnergyStored() / energy.getMaxEnergyStored());
    }

    /**
     * Add tooltip showing energy stored
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null) {
            NumberFormat format = NumberFormat.getInstance();
            String stored = format.format(energy.getEnergyStored());
            String max = format.format(energy.getMaxEnergyStored());
            tooltipComponents.add(Component.literal("§6Energy: §e" + stored + " / " + max + " FE"));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

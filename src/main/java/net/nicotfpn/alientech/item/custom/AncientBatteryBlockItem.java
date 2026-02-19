package net.nicotfpn.alientech.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.neoforged.neoforge.energy.IEnergyStorage;
import java.util.List;

public class AncientBatteryBlockItem extends BlockItem {
    public AncientBatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null) {
            tooltipComponents.add(Component
                    .literal(String.format("Energy: %,d / %,d FE", energy.getEnergyStored(),
                            energy.getMaxEnergyStored()))
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    // Capability provider is now handled via event or data components in 1.21.1+
    // But for basic "BlockEntityTag" support, vanilla handles simple NBT.
    // If we want "Real" energy on the item stack that transfers to the block, we
    // rely on standard NBT copy unless using DataComponents.
}

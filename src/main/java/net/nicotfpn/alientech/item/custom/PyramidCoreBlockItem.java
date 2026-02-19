package net.nicotfpn.alientech.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.nicotfpn.alientech.Config;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PyramidCoreBlockItem extends BlockItem {

    public PyramidCoreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
            @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        // Display the Configured Capacity
        int maxEnergy = Config.PYRAMID_CORE_CAPACITY.get();
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);

        // We show "Energy Capacity" because the item itself might not store energy
        // until placed/broken with NBT
        // But users want to know the POTENTIAL of the block.
        String capacityText = formatter.format(maxEnergy);

        tooltipComponents.add(Component.literal("§7Max Capacity: §e" + capacityText + " FE"));
        tooltipComponents.add(Component.translatable("block.alientech.pyramid_core.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

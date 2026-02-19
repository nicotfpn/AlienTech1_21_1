package net.nicotfpn.alientech.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Decaying Graviton — an unstable graviton that degrades over time.
 * <p>
 * When held in a player's inventory, a DecayTimer ticks down.
 * When the timer reaches zero, the item is consumed (destroyed).
 * Use it in a Quantum Vacuum Turbine before it decays!
 * <p>
 * NBT: DecayTimer (int) stored via DataComponents.CUSTOM_DATA.
 * Default lifetime: 6000 ticks (5 minutes).
 */
public class DecayingGravitonItem extends Item {

    private static final String KEY_DECAY_TIMER = "DecayTimer";
    public static final int DEFAULT_LIFETIME = 6000; // 5 minutes at 20 tps

    public DecayingGravitonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity,
            int slotId, boolean isSelected) {
        if (level.isClientSide())
            return;

        int timer = getDecayTimer(stack);

        // Initialize timer on first tick
        if (timer <= 0 && !hasDecayTimer(stack)) {
            setDecayTimer(stack, DEFAULT_LIFETIME);
            return;
        }

        // Tick down
        if (timer > 0) {
            setDecayTimer(stack, timer - 1);
        } else {
            // Decay complete — destroy item
            stack.shrink(stack.getCount());
        }
    }

    // ==================== NBT Helpers ====================

    private static boolean hasDecayTimer(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null)
            return false;
        return customData.copyTag().contains(KEY_DECAY_TIMER);
    }

    public static int getDecayTimer(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null)
            return 0;
        return customData.copyTag().getInt(KEY_DECAY_TIMER);
    }

    private static void setDecayTimer(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(KEY_DECAY_TIMER, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ==================== Visual Feedback ====================

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        // Glimmer effect that fades as item decays
        int timer = getDecayTimer(stack);
        return timer > DEFAULT_LIFETIME / 2;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return hasDecayTimer(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        int timer = getDecayTimer(stack);
        return Math.round(13.0F * timer / DEFAULT_LIFETIME);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        float ratio = (float) getDecayTimer(stack) / DEFAULT_LIFETIME;
        // Green → Yellow → Red gradient as decay progresses
        if (ratio > 0.5f)
            return 0x00FF00; // Green
        if (ratio > 0.25f)
            return 0xFFFF00; // Yellow
        return 0xFF0000; // Red
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int timer = getDecayTimer(stack);
        if (timer > 0) {
            int seconds = timer / 20;
            tooltip.add(Component.translatable("item.alientech.decaying_graviton.remaining",
                    String.format("%d:%02d", seconds / 60, seconds % 60))
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.alientech.decaying_graviton.unstable")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }
}

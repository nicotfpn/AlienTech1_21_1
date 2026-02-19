package net.nicotfpn.alientech.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnergyEyeOfHorusItem extends Item {
    public static final int MAX_ENERGY = 5_000_000;
    private static final Logger LOGGER = LoggerFactory.getLogger("AlienTech");
    private static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("alientech", "the_archives");

    public EnergyEyeOfHorusItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            try {
                net.nicotfpn.alientech.compat.PatchouliCompat.openBook(BOOK_ID);
            } catch (NoClassDefFoundError e) {
                LOGGER.debug("Patchouli not installed, wiki unavailable");
            } catch (Exception e) {
                LOGGER.error("Failed to open The Archives wiki", e);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(@NotNull ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        copy.setCount(1);
        return copy;
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }
}

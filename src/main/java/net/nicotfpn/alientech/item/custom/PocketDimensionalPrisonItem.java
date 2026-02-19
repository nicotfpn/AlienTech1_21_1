package net.nicotfpn.alientech.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.nicotfpn.alientech.util.EntityStorageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Pocket Dimensional Prison — Captures living mobs and stores them as NBT data.
 * <p>
 * Right-click a mob: capture and store in item NBT.
 * Right-click a Decay Chamber Controller: transfer mob into chamber.
 * <p>
 * Uses {@link EntityStorageUtil} for all mob snapshot operations.
 * Uses vanilla Ender Pearl model (set in datagen/model JSON).
 */
public class PocketDimensionalPrisonItem extends Item {

    public PocketDimensionalPrisonItem(Properties properties) {
        super(properties);
    }

    // ==================== Mob Capture (Right-click entity) ====================

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
            @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (player.level().isClientSide())
            return InteractionResult.SUCCESS;

        // Cannot capture if already holding a mob
        if (EntityStorageUtil.hasStoredMob(stack)) {
            player.displayClientMessage(
                    Component.translatable("item.alientech.pocket_prison.already_occupied")
                            .withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }

        // Cannot capture players
        if (target instanceof Player) {
            return InteractionResult.FAIL;
        }

        // Capture the mob
        ItemStack copy = stack.copy();
        boolean success = EntityStorageUtil.storeMob(copy, target);

        if (success) {
            // Remove mob from world
            target.discard();

            // Update held item
            player.setItemInHand(hand, copy);

            String entityName = EntityStorageUtil.getStoredEntityName(copy);
            player.displayClientMessage(
                    Component.translatable("item.alientech.pocket_prison.captured",
                            entityName != null ? entityName : "Unknown")
                            .withStyle(ChatFormatting.GREEN),
                    true);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    // ==================== Transfer to Decay Chamber (Right-click block)
    // ====================

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if (context.getLevel().isClientSide())
            return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        if (!EntityStorageUtil.hasStoredMob(stack))
            return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);

        // Check if the target block is a Decay Chamber Controller
        // Uses string check to avoid hard dependency on
        // DecayChamberControllerBlockEntity
        // (which doesn't exist yet in Phase 2)
        if (blockEntity != null && isDecayChamberController(blockEntity)) {
            // Transfer will be handled by the Decay Chamber in Phase 3
            // For now, this hook exists so the item is architecturally ready
            return InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Check if a block entity is a Decay Chamber Controller.
     * Uses class name check to avoid compile-time dependency on Phase 3 code.
     * Will be replaced with instanceof check once DecayChamberControllerBlockEntity
     * exists.
     */
    private boolean isDecayChamberController(BlockEntity blockEntity) {
        return blockEntity.getClass().getSimpleName().equals("DecayChamberControllerBlockEntity");
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (EntityStorageUtil.hasStoredMob(stack)) {
            String entityName = EntityStorageUtil.getStoredEntityName(stack);
            float health = EntityStorageUtil.getStoredHealth(stack);

            tooltip.add(Component.translatable("item.alientech.pocket_prison.contains",
                    entityName != null ? entityName : "Unknown")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            tooltip.add(Component.translatable("item.alientech.pocket_prison.health",
                    String.format("%.1f", health))
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.alientech.pocket_prison.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    // ==================== Item Properties ====================

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        // Enchantment glint when holding a mob
        return EntityStorageUtil.hasStoredMob(stack);
    }
}

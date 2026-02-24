package net.nicotfpn.alientech.machine.decay;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.item.custom.PocketDimensionalPrisonItem;
import net.nicotfpn.alientech.util.EntityStorageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decay Chamber Controller block — main functional block of the multiblock.
 * Accepts mobs from Pocket Dimensional Prison items.
 * HAS_MOB property drives visual state (model switch).
 */
public class DecayChamberControllerBlock extends BaseEntityBlock {

    public static final MapCodec<DecayChamberControllerBlock> CODEC = simpleCodec(DecayChamberControllerBlock::new);

    /** Whether the controller currently contains a mob being processed. */
    public static final BooleanProperty HAS_MOB = BooleanProperty.create("has_mob");

    public DecayChamberControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_MOB, false));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_MOB);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    // ==================== Block Entity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DecayChamberControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
            @NotNull BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return createTickerHelper(type, ModBlockEntities.DECAY_CHAMBER_CONTROLLER_BE.get(),
                (lvl, pos, st, be) -> DecayChamberControllerBlockEntity.tickServer(lvl, pos, st, be));
    }

    // ==================== Interaction ====================

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
            @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hitResult) {
        if (level.isClientSide())
            return ItemInteractionResult.SUCCESS;

        // If player is holding a Pocket Dimensional Prison with a stored mob, transfer
        // it
        if (stack.getItem() instanceof PocketDimensionalPrisonItem && EntityStorageUtil.hasStoredMob(stack)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DecayChamberControllerBlockEntity controller) {
                if (controller.acceptMob(stack)) {
                    return ItemInteractionResult.CONSUME;
                }
            }
        }

        // Fall through to useWithoutItem for GUI opening
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
            @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DecayChamberControllerBlockEntity controller
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(controller, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // ==================== Block Removal ====================

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DecayChamberControllerBlockEntity controller) {
                controller.drops();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}

package net.nicotfpn.alientech.machine.evolution;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evolution Chamber block — evolves players to higher evolution stages.
 * <p>
 * Players stand on top of this block to evolve.
 * Uses cube_all model with placeholder texture.
 */
public class EvolutionChamberBlock extends BaseEntityBlock {

    public static final MapCodec<EvolutionChamberBlock> CODEC = simpleCodec(EvolutionChamberBlock::new);

    public EvolutionChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    // ==================== Block Entity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new EvolutionChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull net.minecraft.world.level.Level level,
            @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null; // Server-side only
        }
        return createTickerHelper(type, ModBlockEntities.EVOLUTION_CHAMBER_BE.get(),
                (lvl, pos, st, be) -> EvolutionChamberBlockEntity.tickServer(lvl, pos, st, be));
    }

    // ==================== Block Removal ====================

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull net.minecraft.world.level.Level level,
            @NotNull BlockPos pos, @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EvolutionChamberBlockEntity chamber) {
                // No drops needed - stateless machine
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}

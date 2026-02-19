package net.nicotfpn.alientech.machine.entropy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.block.entity.base.AlienBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entropy Reservoir block — converts entropy biomass into decaying gravitons.
 * Uses cube_all model with placeholder texture.
 */
public class EntropyReservoirBlock extends BaseEntityBlock {

    public static final MapCodec<EntropyReservoirBlock> CODEC = simpleCodec(EntropyReservoirBlock::new);

    public EntropyReservoirBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new EntropyReservoirBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
            @NotNull BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return createTickerHelper(type, ModBlockEntities.ENTROPY_RESERVOIR_BE.get(),
                (lvl, pos, st, be) -> AlienBlockEntity.tickServer(lvl, pos, st, be));
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EntropyReservoirBlockEntity reservoir) {
                reservoir.drops();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}

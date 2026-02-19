package net.nicotfpn.alientech.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.block.entity.EntropyCableBlockEntity;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entropy Cable — transports entropy between adjacent IEntropyHandler blocks.
 * <p>
 * No GUI, no stored entropy. Pure transport conduit.
 * Uses cube_all model (simple visual, no custom rendering).
 */
public class EntropyCableBlock extends BaseEntityBlock {

    public static final MapCodec<EntropyCableBlock> CODEC = simpleCodec(EntropyCableBlock::new);

    public EntropyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new EntropyCableBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
            @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide)
            return null;
        return createTickerHelper(type, ModBlockEntities.ENTROPY_CABLE_BE.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}

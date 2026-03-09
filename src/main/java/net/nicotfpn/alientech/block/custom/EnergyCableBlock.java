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
import net.nicotfpn.alientech.block.entity.EnergyCableBlockEntity;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Energy Cable — transports Forge Energy (FE) between adjacent blocks that
 * expose
 * the NeoForge {@code Capabilities.EnergyStorage.BLOCK} capability.
 * <p>
 * This block delegates the actual transfer logic to
 * {@link EntropyCableBlockEntity}
 * (which is shared between entropy and energy cables).
 */
public class EnergyCableBlock extends BaseEntityBlock {

    public static final MapCodec<EnergyCableBlock> CODEC = simpleCodec(EnergyCableBlock::new);

    private final int transferRate;

    public EnergyCableBlock(Properties properties) {
        this(properties, 1000); // Default fallback
    }

    public EnergyCableBlock(Properties properties, int transferRate) {
        super(properties);
        this.transferRate = transferRate;
    }

    public int getTransferRate() {
        return transferRate;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    public BlockEntity newBlockEntity(@NotNull BlockPos pos,
            @NotNull net.minecraft.world.level.block.state.BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
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
        return createTickerHelper(type, ModBlockEntities.ENERGY_CABLE_BE.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}

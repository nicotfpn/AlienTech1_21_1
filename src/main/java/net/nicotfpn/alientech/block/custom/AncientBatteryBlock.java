package net.nicotfpn.alientech.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nicotfpn.alientech.block.entity.AncientBatteryBlockEntity;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Ancient Battery - An energy storage block that can charge/discharge items.
 * Has 1M FE capacity and can charge Eye of Horus Activated.
 */
public class AncientBatteryBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public AncientBatteryBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof AncientBatteryBlockEntity batteryEntity) {
                ((ServerPlayer) player).openMenu(batteryEntity, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AncientBatteryBlockEntity batteryEntity) {
                // Check for "Energy" in CustomData
                net.minecraft.world.item.component.CustomData customData = stack
                        .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null && customData.contains("Energy")) {
                    int energy = customData.copyTag().getInt("Energy");
                    if (batteryEntity
                            .getEnergyStorage() instanceof net.nicotfpn.alientech.util.SyncableEnergyStorage syncStorage) {
                        syncStorage.setEnergy(energy);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        // Save energy to item when block is cloned/broken
        if (level.getBlockEntity(pos) instanceof AncientBatteryBlockEntity battery) {
            int energy = battery.getEnergyStorage().getEnergyStored();
            if (energy > 0) {
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(
                                new net.minecraft.nbt.CompoundTag() {
                                    {
                                        putInt("Energy", energy);
                                    }
                                }));
            }
        }
        return stack;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AncientBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ANCIENT_BATTERY_BE.get(),
                level.isClientSide()
                        ? net.nicotfpn.alientech.block.entity.base.AlienBlockEntity::tickClient
                        : net.nicotfpn.alientech.block.entity.base.AlienBlockEntity::tickServer);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> serverType,
            BlockEntityType<E> clientType,
            BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Ancient Battery Model:
        // Base plate: 3,0,3 to 13,2,13
        // Core body: 3.6,2,3.6 to 12.4,10,12.4
        // Top plate: 3,10,3 to 13,12,13
        // Antenna: 7.5,12,4 to 8.5,14,12
        return Shapes.or(
                Block.box(3, 0, 3, 13, 2, 13), // Base
                Block.box(3.6, 2, 3.6, 12.4, 10, 12.4), // Core
                Block.box(3, 10, 3, 13, 12, 13), // Top
                Block.box(7.5, 12, 4, 8.5, 14, 12) // Antenna
        );
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AncientBatteryBlockEntity battery) {
                battery.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ==================== Comparator Output ====================

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AncientBatteryBlockEntity be) {
            int stored = be.getEnergyStorage().getEnergyStored();
            int max = be.getEnergyStorage().getMaxEnergyStored();
            return max > 0 ? (int) (15.0 * stored / max) : 0;
        }
        return 0;
    }
}

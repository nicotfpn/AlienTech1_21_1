package net.nicotfpn.alientech.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.nicotfpn.alientech.block.entity.CreativeAncientBatteryBlockEntity;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;

import org.jetbrains.annotations.Nullable;

public class CreativeAncientBatteryBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CreativeAncientBatteryBlock(Properties properties) {
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

    private static final VoxelShape SHAPE_NS = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(3.6, 2, 3.6, 12.4, 10, 12.4),
            Block.box(3, 10, 3, 13, 12, 13),
            Block.box(7.5, 12, 4, 8.5, 14, 12));

    private static final VoxelShape SHAPE_EW = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(3.6, 2, 3.6, 12.4, 10, 12.4),
            Block.box(3, 10, 3, 13, 12, 13),
            Block.box(4, 12, 7.5, 12, 14, 8.5));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        if (dir == Direction.EAST || dir == Direction.WEST) {
            return SHAPE_EW;
        }
        return SHAPE_NS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeAncientBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != ModBlockEntities.CREATIVE_ANCIENT_BATTERY_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof CreativeAncientBatteryBlockEntity battery) {
                battery.tick();
            }
        };
    }
}

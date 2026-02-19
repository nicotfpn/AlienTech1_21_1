package net.nicotfpn.alientech.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.block.entity.PyramidCoreBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Pyramid Core block - the main energy generator for the pyramid structure.
 * Generates FE when placed on a valid pyramid multiblock and activated with an
 * Ankh.
 */
public class PyramidCoreBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    // Hitbox shapes for each direction
    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public PyramidCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    // ==================== Shape/Hitbox ====================

    private static Map<Direction, VoxelShape> createShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

        // Pyramid Core Model (NORTH facing):
        // Main body: 0,0,0 to 16,10,15.9
        // Middle layer: 0,10,0 to 16,12,16
        // Top layer: 0.9,12,1 to 15.1,13,15
        // Horus eyes on top: 3.5-12.5,13,2-14 (decorative, too thin for collision)
        VoxelShape northShape = Shapes.or(
                Block.box(0, 0, 0, 16, 10, 15.9), // Main body
                Block.box(0, 10, 0, 16, 12, 16), // Middle layer
                Block.box(0.9, 12, 1, 15.1, 13, 15) // Top layer
        );

        shapes.put(Direction.NORTH, northShape);
        shapes.put(Direction.SOUTH, rotateShape(Direction.NORTH, Direction.SOUTH, northShape));
        shapes.put(Direction.EAST, rotateShape(Direction.NORTH, Direction.EAST, northShape));
        shapes.put(Direction.WEST, rotateShape(Direction.NORTH, Direction.WEST, northShape));

        return shapes;
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = { shape, Shapes.empty() };
        int rotations = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;

        for (int i = 0; i < rotations; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1],
                    Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.NORTH));
    }

    // ==================== Block State ====================

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    // ==================== Block Entity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PyramidCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PYRAMID_CORE_BE.get(), PyramidCoreBlockEntity::tick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> serverType,
            BlockEntityType<E> clientType,
            BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }

    // ==================== Interaction ====================

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PyramidCoreBlockEntity blockEntity) {
            player.openMenu(blockEntity, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PyramidCoreBlockEntity blockEntity) {
            blockEntity.drops();
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
        if (level.getBlockEntity(pos) instanceof PyramidCoreBlockEntity be) {
            int stored = be.getEnergyStorage().getEnergyStored();
            int max = be.getEnergyStorage().getMaxEnergyStored();
            return max > 0 ? (int) (15.0 * stored / max) : 0;
        }
        return 0;
    }
}
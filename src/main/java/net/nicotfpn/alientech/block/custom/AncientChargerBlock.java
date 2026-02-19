package net.nicotfpn.alientech.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.nicotfpn.alientech.block.entity.AncientChargerBlockEntity;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

public class AncientChargerBlock extends BaseEntityBlock {
    public static final MapCodec<AncientChargerBlock> CODEC = simpleCodec(AncientChargerBlock::new);

    // Precise hitbox matching Blockbench model - no gaps!
    // Base layer: 0-3 pixels height (full width)
    // Decorated layer: 2-14 pixels width/depth, 2.7-4.7 pixels height
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16), // Base plate (full block, 3px tall)
            Block.box(2, 2.7, 2, 14, 4.7, 14) // Decorated top (12x12, 2px tall)
    );

    public AncientChargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AncientChargerBlockEntity charger) {
                IItemHandler h = charger.getItemHandler();
                ItemStack stackInSlot = h.getStackInSlot(0);

                if (!stackInSlot.isEmpty()) {
                    // Extract item
                    player.getInventory().placeItemBackInInventory(h.extractItem(0, 64, false));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AncientChargerBlockEntity charger) {
                IItemHandler h = charger.getItemHandler();
                ItemStack stackInSlot = h.getStackInSlot(0);

                if (stackInSlot.isEmpty() && !stack.isEmpty()) {
                    // Insert item
                    ItemStack leftover = h.insertItem(0, stack.copy(), false);
                    stack.setCount(leftover.getCount());
                    return ItemInteractionResult.SUCCESS;
                } else if (!stackInSlot.isEmpty()) {
                    // Extract item
                    player.getInventory().placeItemBackInInventory(h.extractItem(0, 64, false));
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        // Save energy to item when block is cloned/broken
        if (level.getBlockEntity(pos) instanceof AncientChargerBlockEntity charger) {
            int energy = charger.getEnergyStorage().getEnergyStored();
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
        return new AncientChargerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ANCIENT_CHARGER_BE.get(),
                level.isClientSide()
                        ? AncientChargerBlockEntity::clientTick
                        : AncientChargerBlockEntity::serverTick);
    }

    // ==================== Comparator Output ====================

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AncientChargerBlockEntity be) {
            int stored = be.getEnergyStorage().getEnergyStored();
            int max = be.getEnergyStorage().getMaxEnergyStored();
            return max > 0 ? (int) (15.0 * stored / max) : 0;
        }
        return 0;
    }
}

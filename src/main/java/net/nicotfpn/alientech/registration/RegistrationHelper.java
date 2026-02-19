package net.nicotfpn.alientech.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.item.ModItems;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helper class for easy item and block registration.
 * Usage:
 * 
 * <pre>
 * // Simple item
 * public static final DeferredItem<Item> MY_ITEM = RegistrationHelper.simpleItem("my_item");
 * 
 * // Item with properties
 * public static final DeferredItem<Item> MY_RARE_ITEM = RegistrationHelper.item("my_rare_item",
 *         props -> props.rarity(Rarity.RARE).stacksTo(1));
 * 
 * // Energy item
 * public static final DeferredItem<Item> MY_POWER_ITEM = RegistrationHelper.energyItem("my_power_item", Rarity.EPIC);
 * 
 * // Simple block with item
 * public static final DeferredBlock<Block> MY_BLOCK = RegistrationHelper.simpleBlock("my_block", MapColor.GOLD, 3f,
 *         SoundType.STONE);
 * </pre>
 */
public final class RegistrationHelper {

    private RegistrationHelper() {
    }

    // ==================== ITEM REGISTRATION ====================

    /**
     * Register a simple item with default properties.
     */
    public static DeferredItem<Item> simpleItem(String name) {
        return ModItems.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /**
     * Register an item with custom properties.
     */
    public static DeferredItem<Item> item(String name, Function<Item.Properties, Item.Properties> propsModifier) {
        return ModItems.ITEMS.register(name, () -> new Item(propsModifier.apply(new Item.Properties())));
    }

    /**
     * Register a custom item with a supplier.
     */
    public static <T extends Item> DeferredItem<T> customItem(String name, Supplier<T> itemSupplier) {
        return ModItems.ITEMS.register(name, itemSupplier);
    }

    /**
     * Register an item with rarity and stack size.
     */
    public static DeferredItem<Item> item(String name, Rarity rarity, int stackSize) {
        return ModItems.ITEMS.register(name, () -> new Item(new Item.Properties()
                .rarity(rarity)
                .stacksTo(stackSize)));
    }

    /**
     * Register a rare unstackable item.
     */
    public static DeferredItem<Item> rareItem(String name, Rarity rarity) {
        return item(name, rarity, 1);
    }

    /**
     * Register an item that should be an energy storage item.
     * Note: The item class must implement energy capability separately.
     */
    public static DeferredItem<Item> energyItem(String name, Rarity rarity) {
        return ModItems.ITEMS.register(name, () -> new Item(new Item.Properties()
                .rarity(rarity)
                .stacksTo(1)
                .setNoRepair()));
    }

    // ==================== BLOCK REGISTRATION ====================

    /**
     * Register a simple block with auto-generated BlockItem.
     */
    public static DeferredBlock<Block> simpleBlock(String name, MapColor color, float strength, SoundType sound) {
        DeferredBlock<Block> block = ModBlocks.BLOCKS.register(name, () -> new Block(
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .strength(strength)
                        .sound(sound)
                        .requiresCorrectToolForDrops()));
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /**
     * Register a custom block with auto-generated BlockItem.
     */
    public static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /**
     * Register a custom block with custom BlockItem.
     */
    public static <T extends Block, I extends BlockItem> DeferredBlock<T> blockWithItem(
            String name,
            Supplier<T> blockSupplier,
            Function<T, I> itemFactory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(block.get()));
        return block;
    }

    /**
     * Register a decorative block (no tool requirement).
     */
    public static DeferredBlock<Block> decorativeBlock(String name, MapColor color, float strength, SoundType sound) {
        DeferredBlock<Block> block = ModBlocks.BLOCKS.register(name, () -> new Block(
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .strength(strength)
                        .sound(sound)));
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /**
     * Register a glowing block.
     */
    public static DeferredBlock<Block> glowingBlock(String name, MapColor color, float strength, SoundType sound,
            int lightLevel) {
        DeferredBlock<Block> block = ModBlocks.BLOCKS.register(name, () -> new Block(
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .strength(strength)
                        .sound(sound)
                        .lightLevel(state -> lightLevel)
                        .requiresCorrectToolForDrops()));
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    // ==================== BLOCK PROPERTIES BUILDER ====================

    /**
     * Create common block properties with standard settings.
     */
    public static BlockBehaviour.Properties machineProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3f)
                .sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops();
    }

    /**
     * Create ore block properties.
     */
    public static BlockBehaviour.Properties oreProps(float strength, SoundType sound) {
        return BlockBehaviour.Properties.of()
                .strength(strength)
                .sound(sound)
                .requiresCorrectToolForDrops();
    }
}

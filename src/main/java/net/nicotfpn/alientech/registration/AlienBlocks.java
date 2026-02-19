package net.nicotfpn.alientech.registration;

import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.custom.AncientBatteryBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.BlockItem;

public class AlienBlocks {

        public static final AlienDeferredRegister BLOCKS = new AlienDeferredRegister(AlienTech.MOD_ID);

        public static final BlockRegistryObject<AncientBatteryBlock, BlockItem> ANCIENT_BATTERY = BLOCKS.register(
                        "ancient_battery",
                        () -> new AncientBatteryBlock(BlockBehaviour.Properties.of()
                                        .strength(3.5f)
                                        .requiresCorrectToolForDrops()
                                        .sound(SoundType.METAL)),
                        block -> new net.nicotfpn.alientech.item.custom.AncientBatteryBlockItem(block,
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)));

        public static void register(net.neoforged.bus.api.IEventBus eventBus) {
                BLOCKS.register(eventBus);
        }

}

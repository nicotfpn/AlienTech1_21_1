package net.nicotfpn.alientech.screen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;

public class ModMenuTypes {
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU,
                        AlienTech.MOD_ID);

        public static final DeferredHolder<MenuType<?>, MenuType<PrimalCatalystMenu>> PRIMAL_CATALYST_MENU = MENUS
                        .register("primal_catalyst_menu", () -> IMenuTypeExtension.create(PrimalCatalystMenu::new));

        public static final DeferredHolder<MenuType<?>, MenuType<PyramidCoreMenu>> PYRAMID_CORE_MENU = MENUS
                        .register("pyramid_core_menu", () -> IMenuTypeExtension.create(PyramidCoreMenu::new));

        public static final DeferredHolder<MenuType<?>, MenuType<AncientBatteryMenu>> ANCIENT_BATTERY_MENU = MENUS
                        .register("ancient_battery_menu", () -> IMenuTypeExtension.create(AncientBatteryMenu::new));

        public static final DeferredHolder<MenuType<?>, MenuType<AncientChargerMenu>> ANCIENT_CHARGER_MENU = MENUS
                        .register("ancient_charger_menu", () -> IMenuTypeExtension.create(AncientChargerMenu::new));

        public static void register(IEventBus eventBus) {
                MENUS.register(eventBus);
        }
}

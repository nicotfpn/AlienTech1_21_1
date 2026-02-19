package net.nicotfpn.alientech.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.client.EnergyHudOverlay;
import net.nicotfpn.alientech.screen.AncientBatteryScreen;
import net.nicotfpn.alientech.screen.AncientChargerScreen;
import net.nicotfpn.alientech.screen.ModMenuTypes;
import net.nicotfpn.alientech.screen.PrimalCatalystScreen;
import net.nicotfpn.alientech.screen.PyramidCoreScreen;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.client.renderer.AncientChargerRenderer;

@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ANCIENT_CHARGER_BE.get(), AncientChargerRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "energy_hud"),
                EnergyHudOverlay.HUD_ENERGY);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.PRIMAL_CATALYST_MENU.get(), PrimalCatalystScreen::new);
        event.register(ModMenuTypes.PYRAMID_CORE_MENU.get(), PyramidCoreScreen::new);
        event.register(ModMenuTypes.ANCIENT_BATTERY_MENU.get(), AncientBatteryScreen::new);
        event.register(ModMenuTypes.ANCIENT_CHARGER_MENU.get(), AncientChargerScreen::new);
    }
}

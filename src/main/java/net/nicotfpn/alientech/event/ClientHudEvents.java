package net.nicotfpn.alientech.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.client.PyramidCoreHud;

@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientHudEvents {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiEvent.Post event) {
        PyramidCoreHud.renderHud(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}

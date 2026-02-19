package assets.alientech.client;

public package net.nicotfpn.alientech.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.client.PyramidCoreHud;

@EventBusSubscriber(modid = AlienTech.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    
    /**
     * Renderiza a HUD do Pyramid Core quando o jogador olha para ele
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiEvent.Post event) {
        PyramidCoreHud.renderHud(event.getGuiGraphics(), event.getPartialTick());
    }
    
    // Adicione outros eventos de cliente aqui se necessário
} {
    
}

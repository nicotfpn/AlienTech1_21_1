package net.nicotfpn.alientech.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;

public class EntropyReservoirScreen extends AbstractContainerScreen<EntropyReservoirMenu> {

    // Using a generic background because there are no physical slots designed for
    // this buffer block
    private static final ResourceLocation TEXTURE = ResourceLocation
            .withDefaultNamespace("textures/gui/container/dispenser.png");

    public EntropyReservoirScreen(EntropyReservoirMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        NumberFormat format = NumberFormat.getInstance();
        guiGraphics.drawString(this.font, "Alien Energy Network", x + 10, y + 25, 0x55FFFF, false);
        guiGraphics.drawString(this.font, "Entropy: " + format.format(menu.getEntropyStored()), x + 10, y + 40,
                0xFF5555, false);
        guiGraphics.drawString(this.font, "Instability: " + format.format(menu.getInstabilityLevel()), x + 10, y + 55,
                0xFFAA00, false);
    }
}

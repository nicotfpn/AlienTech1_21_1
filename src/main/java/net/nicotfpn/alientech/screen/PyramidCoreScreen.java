package net.nicotfpn.alientech.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nicotfpn.alientech.AlienTech;

import java.text.NumberFormat;
import java.util.List;

public class PyramidCoreScreen extends AbstractContainerScreen<PyramidCoreMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID,
            "textures/gui/pyramid_core_gui.png");

    // ==================== GUI Element Positions (from alientech_gui_gen.py V9)
    // ====================
    private static final int BAR_TOP = 17;
    private static final int BAR_HEIGHT = 52;
    private static final int BAR_WIDTH = 8;

    private static final int ENTROPY_X = 8;

    // Zone 2 UVs
    private static final int UV_ENT_U = 176;

    public PyramidCoreScreen(PyramidCoreMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Layer 1: Background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Layer 2: Entropy bar fill (Zone 2)
        renderEntropyBar(guiGraphics, x, y);
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        long entropy = menu.getEntropy();
        long maxEntropy = menu.getMaxEntropy();
        if (maxEntropy > 0) {
            int fill = (int) (entropy * BAR_HEIGHT / maxEntropy);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE,
                        x + ENTROPY_X, y + BAR_TOP + (BAR_HEIGHT - fill),
                        UV_ENT_U, BAR_HEIGHT - fill,
                        BAR_WIDTH, fill);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderEntropyTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderEntropyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int relX = mouseX - x;
        int relY = mouseY - y;

        // Entropy tooltip (8-16, 17-69)
        if (relX >= ENTROPY_X && relX < ENTROPY_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            NumberFormat format = NumberFormat.getInstance();
            String entropyText = format.format(menu.getEntropy()) + " / " + format.format(menu.getMaxEntropy()) + " EN";
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§cEntropy Buffer"),
                    Component.literal("§4" + entropyText)), mouseX, mouseY);
        }

        // Note: Pyramid Core Energy tooltip removed as per V9 sidebar spec (Entropy
        // only)
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}

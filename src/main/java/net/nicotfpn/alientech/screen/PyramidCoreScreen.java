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
import java.util.Locale;

public class PyramidCoreScreen extends AbstractContainerScreen<PyramidCoreMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID,
            "textures/gui/pyramid_core_gui.png");

    // Vertical energy bar on the left (standardized)
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 14;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 60;

    public PyramidCoreScreen(PyramidCoreMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        // Render vertical energy bar (standardized with other blocks)
        renderEnergyBar(guiGraphics, x, y);
    }

    /**
     * Renders vertical energy bar that fills from bottom to top (STANDARDIZED).
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int barX = x + ENERGY_BAR_X;
        int barY = y + ENERGY_BAR_Y;

        long energy = menu.getEnergyStored();
        long maxEnergy = menu.getMaxEnergy();

        int scaledEnergy = (int) (ENERGY_BAR_HEIGHT * energy / Math.max(1, maxEnergy));
        if (scaledEnergy > 0) {
            // Draw from bottom up
            int fillY = barY + ENERGY_BAR_HEIGHT - scaledEnergy;
            guiGraphics.blit(TEXTURE, barX, fillY, 176, 14 + (ENERGY_BAR_HEIGHT - scaledEnergy),
                    ENERGY_BAR_WIDTH, scaledEnergy, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Renders tooltip when hovering over energy bar.
     */
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int barX = x + ENERGY_BAR_X;
        int barY = y + ENERGY_BAR_Y;

        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH &&
                mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {

            long stored = menu.getEnergyStored();
            long max = menu.getMaxEnergy();

            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
            String energyText = formatter.format(stored) + " / " + formatter.format(max) + " FE";
            float percentage = max > 0 ? (float) stored / max * 100 : 0;

            String statusText = menu.isActive() ? "§aActive" : "§cInactive";
            // Assuming generation rate is constant in config, or we could sync it.
            // For now, let's show the potential max generation if active.
            String generationText = menu.isActive()
                    ? "§a+" + formatter.format(net.nicotfpn.alientech.Config.PYRAMID_CORE_GENERATION.get()) + " FE/t"
                    : "§70 FE/t";

            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6§lPyramid Core"),
                    Component.literal("§7Stored: §e" + energyText),
                    Component.literal("§7Capacity: §b" + String.format("%.1f%%", percentage)),
                    Component.literal(""),
                    Component.literal("§7Status: " + statusText),
                    Component.literal("§7Generation: " + generationText)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title in gold
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, 5, 0xD4AF37, false);
    }
}

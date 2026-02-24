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

/**
 * GUI Screen for the Primal Catalyst machine.
 *
 * Renders:
 * - Energy bar (vertical, fills bottom-to-top)
 * - Progress bar (horizontal, fills left-to-right)
 * - Energy tooltip on hover
 *
 * Coordinates sourced from mc_gui_generator.py CONFIG.
 */
public class PrimalCatalystScreen extends AbstractContainerScreen<PrimalCatalystMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienTech.MOD_ID, "textures/gui/primal_catalyst_gui.png");

    // ==================== GUI Element Positions (from mc_gui_generator.py)
    // ====================
    // Energy bar (vertical, left side)
    private static final int ENERGY_X = 9;
    private static final int ENERGY_Y = 8;
    private static final int ENERGY_W = 16;
    private static final int ENERGY_H = 44;
    // Entropy bar (vertical, right side of energy)
    private static final int ENTROPY_X = 28;
    private static final int ENTROPY_Y = 8;
    private static final int ENTROPY_W = 16;
    private static final int ENTROPY_H = 44;
    // energy atlas UV → (176, 0) 16×44

    // Progress bar (horizontal)
    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 36;
    private static final int PROGRESS_H = 10;
    // progress atlas UV → (176, 46) 34×10

    public PrimalCatalystScreen(PrimalCatalystMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 176;
        this.imageHeight = 166;
        // Hide default labels
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Main GUI background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        // Energy bar (fills from bottom to top)
        renderEnergyBar(guiGraphics, x, y);

        // Entropy bar (fills from bottom to top)
        renderEntropyBar(guiGraphics, x, y);

        // Progress bar (fills from left to right)
        renderProgressBar(guiGraphics, x, y);
    }

    /**
     * Renders the energy bar. Fills from bottom to top based on stored energy.
     * Source texture: (176, 0) in the atlas.
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int scaled = menu.getScaledEnergy();
        if (scaled > 0) {
            // Draw from bottom up
            guiGraphics.blit(TEXTURE,
                    x + ENERGY_X,
                    y + ENERGY_Y + (ENERGY_H - scaled),
                    176,
                    0 + (ENERGY_H - scaled),
                    ENERGY_W,
                    scaled,
                    256, 256);
        }
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        int entropy = menu.getEntropy();
        int max = menu.getMaxEntropy();
        int scaled = max != 0 && entropy != 0 ? (int) ((long) entropy * ENTROPY_H / max) : 0;
        if (scaled > 0) {
            guiGraphics.blit(TEXTURE,
                    x + ENTROPY_X,
                    y + ENTROPY_Y + (ENTROPY_H - scaled),
                    176,
                    0 + (ENTROPY_H - scaled),
                    ENTROPY_W,
                    scaled,
                    256, 256);
        }
    }

    /**
     * Renders the progress bar. Fills from left to right.
     * Source texture: (176, 46) in the atlas.
     */
    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            int progress = menu.getScaledProgress();
            guiGraphics.blit(TEXTURE,
                    x + PROGRESS_X, y + PROGRESS_Y,
                    176, 46,
                    progress, PROGRESS_H,
                    256, 256);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Energy tooltip
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        // Entropy tooltip
        renderEntropyTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Renders tooltip when hovering over the energy bar.
     */
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int barX = x + ENERGY_X;
        int barY = y + ENERGY_Y;

        if (mouseX >= barX && mouseX < barX + ENERGY_W &&
                mouseY >= barY && mouseY < barY + ENERGY_H) {

            NumberFormat format = NumberFormat.getInstance();
            String energyText = format.format(menu.getEnergy()) + " / " + format.format(menu.getMaxEnergy()) + " FE";

            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Energy"),
                    Component.literal("§e" + energyText)), mouseX, mouseY);
        }
    }

    /**
     * Renders tooltip when hovering over the entropy bar.
     */
    private void renderEntropyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int barX = x + ENTROPY_X;
        int barY = y + ENTROPY_Y;

        if (mouseX >= barX && mouseX < barX + ENTROPY_W &&
                mouseY >= barY && mouseY < barY + ENTROPY_H) {

            NumberFormat format = NumberFormat.getInstance();
            String entropyText = format.format(menu.getEntropy()) + " / " + format.format(menu.getMaxEntropy()) + " EN";

            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Entropy"),
                    Component.literal("§e" + entropyText)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title in gold color at top center
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, 5, 0xD4AF37, false);
    }
}

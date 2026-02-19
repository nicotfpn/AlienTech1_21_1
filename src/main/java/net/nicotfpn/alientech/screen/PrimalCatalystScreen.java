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
 * - Progress arrow (animated left-to-right)
 * - Fuel burn indicator (flame icon, fills top-to-bottom)
 * - Energy bar (vertical, fills bottom-to-top)
 * - Energy tooltip on hover
 */
public class PrimalCatalystScreen extends AbstractContainerScreen<PrimalCatalystMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienTech.MOD_ID, "textures/gui/primal_catalyst_gui.png");

    // ==================== GUI Element Positions ====================
    // Energy bar (vertical, left side)
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 14;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 52;

    // Progress arrow
    private static final int ARROW_X = 85;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 26;
    private static final int ARROW_HEIGHT = 17;

    // Fuel flame
    private static final int FLAME_X = 33;
    private static final int FLAME_Y = 37;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;

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

        // Progress arrow (fills from left to right)
        renderProgressArrow(guiGraphics, x, y);

        // Fuel flame (fills from bottom to top)
        renderFuelFlame(guiGraphics, x, y);
    }

    /**
     * Renders the energy bar. Fills from bottom to top based on stored energy.
     * Source texture: (176, 14) in the atlas.
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int scaled = menu.getScaledEnergy();
        if (scaled > 0) {
            // Draw from bottom up
            guiGraphics.blit(TEXTURE,
                    x + ENERGY_BAR_X,
                    y + ENERGY_BAR_Y + (ENERGY_BAR_HEIGHT - scaled),
                    176,
                    14 + (ENERGY_BAR_HEIGHT - scaled),
                    ENERGY_BAR_WIDTH,
                    scaled,
                    256, 256);
        }
    }

    /**
     * Renders the progress arrow. Fills from left to right.
     * Source texture: (176, 70) in the atlas.
     */
    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            int progress = menu.getScaledProgress();
            guiGraphics.blit(TEXTURE,
                    x + ARROW_X, y + ARROW_Y,
                    176, 70,
                    progress, ARROW_HEIGHT,
                    256, 256);
        }
    }

    /**
     * Renders the fuel flame indicator. Fills from top to bottom when burning.
     * Source texture: (176, 90) in the atlas.
     */
    private void renderFuelFlame(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isBurning()) {
            int scaled = menu.getScaledBurnTime();
            // Flame fills from bottom up (like furnace)
            guiGraphics.blit(TEXTURE,
                    x + FLAME_X,
                    y + FLAME_Y + (FLAME_HEIGHT - scaled),
                    176,
                    90 + (FLAME_HEIGHT - scaled),
                    FLAME_WIDTH,
                    scaled,
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
    }

    /**
     * Renders tooltip when hovering over the energy bar.
     */
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int barX = x + ENERGY_BAR_X;
        int barY = y + ENERGY_BAR_Y;

        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH &&
                mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {

            NumberFormat format = NumberFormat.getInstance();
            String energyText = format.format(menu.getEnergy()) + " / " + format.format(menu.getMaxEnergy()) + " FE";

            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Energy"),
                    Component.literal("§e" + energyText)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title in gold color at top center
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, 5, 0xD4AF37, false);
    }
}

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
 * GUI Screen for the Decay Chamber Controller.
 * <p>
 * Renders:
 * - Energy bar (vertical, fills bottom-to-top)
 * - Progress bar (horizontal, fills left-to-right)
 * - Energy/entropy tooltip on hover
 *
 * Coordinates sourced from create_decay_chamber_gui.py CONFIG.
 */
public class DecayChamberScreen extends AbstractContainerScreen<DecayChamberMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienTech.MOD_ID, "textures/gui/decay_chamber_gui.png");

    // ==================== GUI Element Positions (from alientech_gui_gen.py V9)
    // ====================
    private static final int BAR_TOP = 17;
    private static final int BAR_HEIGHT = 52;
    private static final int BAR_WIDTH = 8;

    private static final int ENTROPY_X = 8;
    private static final int FE_X = 19;

    private static final int PROGRESS_X = 88;
    private static final int PROGRESS_Y = 42;
    private static final int PROGRESS_H = 16;

    // Zone 2 UVs
    private static final int UV_ENT_U = 176;
    private static final int UV_FE_U = 186;
    private static final int UV_PROG_U = 176;
    private static final int UV_PROG_V = 54;

    public DecayChamberScreen(DecayChamberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Layer 1: Main GUI background (Zone 1)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Layer 2: Entropy bar fill (Zone 2)
        renderEntropyBar(guiGraphics, x, y);

        // Layer 3: FE bar fill (Zone 2)
        renderEnergyBar(guiGraphics, x, y);

        // Layer 4: Progress arrow (Zone 2)
        renderProgressBar(guiGraphics, x, y);
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        int entropy = (int) menu.getEntropy();
        int maxEntropy = (int) menu.getMaxEntropy();
        if (maxEntropy > 0) {
            int fill = (int) ((float) entropy / maxEntropy * BAR_HEIGHT);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE,
                        x + ENTROPY_X, y + BAR_TOP + (BAR_HEIGHT - fill),
                        UV_ENT_U, BAR_HEIGHT - fill,
                        BAR_WIDTH, fill);
            }
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy > 0) {
            int fill = (int) ((float) energy / maxEnergy * BAR_HEIGHT);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE,
                        x + FE_X, y + BAR_TOP + (BAR_HEIGHT - fill),
                        UV_FE_U, BAR_HEIGHT - fill,
                        BAR_WIDTH, fill);
            }
        }
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            int progress = menu.getScaledProgress(); // Assumed to return 0-24
            if (progress > 0) {
                guiGraphics.blit(TEXTURE,
                        x + PROGRESS_X, y + PROGRESS_Y,
                        UV_PROG_U, UV_PROG_V,
                        progress, PROGRESS_H);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int relX = mouseX - x;
        int relY = mouseY - y;

        // Entropy tooltip (Entropy bar area: 8-16, 17-69)
        if (relX >= ENTROPY_X && relX < ENTROPY_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            NumberFormat format = NumberFormat.getInstance();
            String entropyText = format.format(menu.getEntropy()) + " / " + format.format(menu.getMaxEntropy()) + " EN";
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§cEntropy"),
                    Component.literal("§4" + entropyText)), mouseX, mouseY);
        }

        // Energy tooltip (FE bar area: 19-27, 17-69)
        if (relX >= FE_X && relX < FE_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            NumberFormat format = NumberFormat.getInstance();
            String energyText = format.format(menu.getEnergy()) + " / " + format.format(menu.getMaxEnergy()) + " FE";
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Energy"),
                    Component.literal("§e" + energyText)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}

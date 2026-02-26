package net.nicotfpn.alientech.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nicotfpn.alientech.client.gui.AlienScreen;
import net.nicotfpn.alientech.AlienTech;

/**
 * GUI Screen for Ancient Battery.
 * Features:
 * - Top Slot: Output (Charge Item)
 * - Bottom Slot: Input (Discharge Item)
 * - Center: Giant Energy Bar
 */
public class AncientBatteryScreen extends AlienScreen<AncientBatteryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienTech.MOD_ID, "textures/gui/ancient_battery_gui.png");

    // ==================== GUI Element Positions (from alientech_gui_gen.py V9)
    // ====================
    private static final int BAR_TOP = 17;
    private static final int BAR_HEIGHT = 52;
    private static final int BAR_WIDTH = 8;

    private static final int ENTROPY_X = 8;
    private static final int FE_X = 19;

    private static final int BATTERY_X = 86;
    private static final int BATTERY_Y = 21;
    private static final int BATTERY_W = 28;
    private static final int BATTERY_H = 36;

    // Zone 2 UVs
    private static final int UV_ENT_U = 176;
    private static final int UV_FE_U = 186;
    private static final int UV_BATT_U = 196;

    public AncientBatteryScreen(AncientBatteryMenu menu, Inventory playerInventory, Component title) {
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
    protected void addGuiElements() {
    }

    @Override
    protected void drawBackgroundTexture(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Layer 1: Background (Zone 1)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Layer 2: Entropy bar fill (Zone 2)
        renderEntropyBar(guiGraphics, x, y);

        // Layer 3: FE bar fill (Zone 2)
        renderEnergyBar(guiGraphics, x, y);

        // Layer 4: Battery Indicator Fill (Zone 2)
        renderBatteryIndicator(guiGraphics, x, y);
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        int entropy = menu.getEntropy();
        int max = menu.getMaxEntropy();
        if (max > 0) {
            int fill = (int) ((float) entropy / max * BAR_HEIGHT);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE, x + ENTROPY_X, y + BAR_TOP + (BAR_HEIGHT - fill), UV_ENT_U, BAR_HEIGHT - fill,
                        BAR_WIDTH, fill);
            }
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int energy = menu.getEnergyStored();
        int max = menu.getMaxEnergy();
        if (max > 0) {
            int fill = (int) ((float) energy / max * BAR_HEIGHT);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE, x + FE_X, y + BAR_TOP + (BAR_HEIGHT - fill), UV_FE_U, BAR_HEIGHT - fill,
                        BAR_WIDTH, fill);
            }
        }
    }

    private void renderBatteryIndicator(GuiGraphics guiGraphics, int x, int y) {
        int energy = menu.getEnergyStored();
        int max = menu.getMaxEnergy();
        if (max > 0) {
            int fill = (int) ((float) energy / max * BATTERY_H);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE, x + BATTERY_X, y + BATTERY_Y + (BATTERY_H - fill), UV_BATT_U,
                        BATTERY_H - fill, BATTERY_W, fill);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, this.playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int relX = mouseX - x;
        int relY = mouseY - y;

        // Entropy bar tooltip
        if (relX >= ENTROPY_X && relX < ENTROPY_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            String entropyText = String.format("Entropy: %,d / %,d EN", menu.getEntropy(), menu.getMaxEntropy());
            guiGraphics.renderTooltip(font, Component.literal(entropyText), mouseX, mouseY);
        }

        // Energy bar tooltip (Sidebar)
        if (relX >= FE_X && relX < FE_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            String energyText = String.format("Energy: %,d / %,d FE", menu.getEnergyStored(), menu.getMaxEnergy());
            guiGraphics.renderTooltip(font, Component.literal(energyText), mouseX, mouseY);
        }

        // Battery Indicator tooltip
        if (relX >= BATTERY_X && relX < BATTERY_X + BATTERY_W && relY >= BATTERY_Y && relY < BATTERY_Y + BATTERY_H) {
            String energyText = String.format("Total Stored: %,d / %,d FE", menu.getEnergyStored(),
                    menu.getMaxEnergy());
            guiGraphics.renderTooltip(font, Component.literal(energyText), mouseX, mouseY);
        }
    }
}

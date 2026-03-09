package net.nicotfpn.alientech.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.client.gui.element.AlienEnergyGauge;
import net.nicotfpn.alientech.client.gui.element.AlienGuiTab;
import net.nicotfpn.alientech.client.gui.element.AlienSideConfigElement;
import net.nicotfpn.alientech.client.gui.AlienInfo;

/**
 * GUI Screen for the Decay Chamber Controller.
 */
public class DecayChamberScreen extends net.nicotfpn.alientech.client.gui.AlienScreen<DecayChamberMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienTech.MOD_ID, "textures/gui/decay_chamber_gui.png");

    private static final int ENTROPY_X = 8;
    private static final int FE_X = 19;
    private static final int PROGRESS_X = 88;
    private static final int PROGRESS_Y = 42;
    private static final int PROGRESS_H = 16;
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
    protected void addGuiElements() {
        // 1. Energy & Entropy Gauges
        // FE Gauge at 19, 17
        addElement(new AlienEnergyGauge(this, FE_X, 17, 8, 52,
                () -> (long) menu.getEnergy(), () -> (long) menu.getMaxEnergy()));

        // Entropy Gauge at 8, 17
        addElement(new AlienEnergyGauge(this, ENTROPY_X, 17, 8, 52,
                () -> (long) menu.getEntropy(), () -> (long) menu.getMaxEntropy()));

        // 2. Side Config Tab
        int tabX = imageWidth;
        int tabY = 20;
        addElement(new AlienGuiTab(
                this, tabX, tabY, 24, 24,
                AlienInfo.TAB_SIDE_CONFIG,
                Component.literal("Side Configuration"),
                tab -> {
                    boolean next = !isConfigMode();
                    setConfigMode(next);
                    tab.setActive(next);
                }));

        // 3. Side Config Panel
        this.sideConfigElement = new AlienSideConfigElement(
                this, 13, 15, menu.blockEntity);
        this.sideConfigElement.visible = false;
    }

    @Override
    protected void drawBackgroundTexture(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (!configMode) {
            // Layer 2 & 3 are now handled by Gauges
            // Layer 4: Progress arrow remains manual
            renderProgressBar(guiGraphics, x, y);
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
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!configMode) {
            guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
            guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
        } else {
            guiGraphics.drawString(font, Component.literal("Side Configuration"), 8, 6, 0xFFD4AF37, false);
        }
    }
}

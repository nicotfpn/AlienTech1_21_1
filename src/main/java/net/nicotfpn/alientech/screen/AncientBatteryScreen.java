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
 * GUI Screen for Ancient Battery.
 * Features:
 * - Top Slot: Output (Charge Item)
 * - Bottom Slot: Input (Discharge Item)
 * - Center: Giant Energy Bar
 */
public class AncientBatteryScreen extends net.nicotfpn.alientech.client.gui.AlienScreen<AncientBatteryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienTech.MOD_ID, "textures/gui/ancient_battery_gui.png");

    private static final int BATTERY_X = 86;
    private static final int BATTERY_Y = 21;
    private static final int BATTERY_W = 28;
    private static final int BATTERY_H = 36;
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
        // 1. Energy Gauge
        addElement(new AlienEnergyGauge(this, 19, 17, 8, 52,
                menu::getEnergyStored, menu::getMaxEnergy));

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
            // Layer 3: Battery Indicator Fill (Still manual since it's unique)
            renderBatteryIndicator(guiGraphics, x, y);
        }
    }

    private void renderBatteryIndicator(GuiGraphics guiGraphics, int x, int y) {
        long energy = menu.getEnergyStored();
        long max = menu.getMaxEnergy();
        if (max > 0) {
            int fill = (int) ((float) energy / max * BATTERY_H);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE, x + BATTERY_X, y + BATTERY_Y + (BATTERY_H - fill), UV_BATT_U,
                        BATTERY_H - fill, BATTERY_W, fill);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!configMode) {
            guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
            guiGraphics.drawString(font, this.playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
        } else {
            guiGraphics.drawString(font, Component.literal("Side Configuration"), 8, 6, 0xFFD4AF37, false);
        }
    }
}

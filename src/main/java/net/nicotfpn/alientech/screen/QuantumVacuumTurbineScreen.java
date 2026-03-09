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
import org.jetbrains.annotations.NotNull;

public class QuantumVacuumTurbineScreen
        extends net.nicotfpn.alientech.client.gui.AlienScreen<QuantumVacuumTurbineMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID,
            "textures/gui/quantum_vacuum_turbine_gui.png");

    private static final int SLOT_X = 91;
    private static final int SLOT_Y = 26;

    public QuantumVacuumTurbineScreen(QuantumVacuumTurbineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
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
    protected void addGuiElements() {
        // 1. Energy Gauge
        addElement(new AlienEnergyGauge(this, 19, 17, 8, 52,
                menu::getEnergyStored, menu::getMaxEnergy));

        // 2. Side Config Tab (Right side)
        int tabX = imageWidth;
        int tabY = 20;
        AlienGuiTab configTab = new AlienGuiTab(
                this, tabX, tabY, 24, 24,
                AlienInfo.TAB_SIDE_CONFIG,
                Component.literal("Side Configuration"),
                tab -> {
                    boolean next = !isConfigMode();
                    setConfigMode(next);
                    tab.setActive(next);
                });
        addElement(configTab);

        // 3. Side Config Panel (Overlay)
        this.sideConfigElement = new AlienSideConfigElement(
                this, 13, 15, menu.blockEntity);
        this.sideConfigElement.visible = false; // Controlled by configMode
    }

    @Override
    protected void drawBackgroundTexture(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        super.renderBg(guiGraphics, pPartialTick, pMouseX, pMouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (!configMode) {
            // Layer 2: Entropy bar fill (Zone 2) - Custom for QVT
            renderEntropyBar(guiGraphics, x, y);

            // --- Burning Effect ---
            if (menu.isBurning()) {
                guiGraphics.fill(x + SLOT_X + 1, y + SLOT_Y + 18, x + SLOT_X + 17, y + SLOT_Y + 20, 0xFFFF6A00);
            }
        }
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        // QVT does not store local entropy — bar is purely cosmetic/disabled.
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
            guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
            guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
        } else {
            guiGraphics.drawString(this.font, Component.literal("Side Configuration"), 8, 6, 0xFFD4AF37, false);
        }
    }
}

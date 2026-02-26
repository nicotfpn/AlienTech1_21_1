package net.nicotfpn.alientech.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;

public class QuantumVacuumTurbineScreen extends AbstractContainerScreen<QuantumVacuumTurbineMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID,
            "textures/gui/quantum_vacuum_turbine_gui.png");

    // ==================== GUI Element Positions (from alientech_gui_gen.py V9)
    // ====================
    private static final int BAR_TOP = 17;
    private static final int BAR_HEIGHT = 52;
    private static final int BAR_WIDTH = 8;

    private static final int ENTROPY_X = 8;
    private static final int FE_X = 19;

    private static final int SLOT_X = 91;
    private static final int SLOT_Y = 26;

    // Zone 2 UVs
    private static final int UV_ENT_U = 176;
    private static final int UV_FE_U = 186;

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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Layer 1: Background (Zone 1)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Layer 2: Entropy bar fill (Zone 2)
        renderEntropyBar(guiGraphics, x, y);

        // Layer 3: FE bar fill (Zone 2)
        renderEnergyBar(guiGraphics, x, y);

        // --- Burning Effect (Optional Indicator) ---
        if (menu.isBurning()) {
            // We can add a small orange glow or indicator if needed,
            // but for now, we follow the texture-pure approach.
            // If we want a simple indicator:
            guiGraphics.fill(x + SLOT_X + 1, y + SLOT_Y + 18, x + SLOT_X + 17, y + SLOT_Y + 20, 0xFFFF6A00);
        }
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        // QVT does not store local entropy — bar is purely cosmetic/disabled.
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        long energy = menu.getEnergyStored();
        long max = menu.getMaxEnergy();
        if (max > 0) {
            int fill = (int) (energy * BAR_HEIGHT / max);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE,
                        x + FE_X, y + BAR_TOP + (BAR_HEIGHT - fill),
                        UV_FE_U, BAR_HEIGHT - fill,
                        BAR_WIDTH, fill);
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

        // Entropy tooltip (8-16, 17-69)
        if (relX >= ENTROPY_X && relX < ENTROPY_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            guiGraphics.renderTooltip(font, Component.literal("Entropy: N/A (Generator)"), mouseX, mouseY);
        }

        // Energy Bar tooltip (19-27, 17-69)
        if (relX >= FE_X && relX < FE_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            long currentEnergy = menu.getEnergyStored();
            long maxEnergy = menu.getMaxEnergy();
            guiGraphics.renderTooltip(font, Component.literal("Energy: " + EnergyUtils.formatCompact(currentEnergy)
                    + " / " + EnergyUtils.formatCompact(maxEnergy) + " FE"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}

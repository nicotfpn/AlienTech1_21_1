package net.nicotfpn.alientech.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nicotfpn.alientech.AlienTech;

public class AncientChargerScreen extends AbstractContainerScreen<AncientChargerMenu> {

    // ==================== GUI Element Positions (from alientech_gui_gen.py V9)
    // ====================
    private static final int BAR_TOP = 17;
    private static final int BAR_HEIGHT = 52;
    private static final int BAR_WIDTH = 8;

    private static final int ENTROPY_X = 8;
    private static final int FE_X = 19;

    // Zone 2 UVs
    private static final int UV_ENT_U = 176;
    private static final int UV_FE_U = 186;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID,
            "textures/gui/ancient_charger_gui.png");

    public AncientChargerScreen(AncientChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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

        // Layer 1: Background (Zone 1)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Layer 2: Entropy bar fill (Zone 2)
        renderEntropyBar(guiGraphics, x, y);

        // Layer 3: FE bar fill (Zone 2)
        renderEnergyBar(guiGraphics, x, y);
    }

    private void renderEntropyBar(GuiGraphics guiGraphics, int x, int y) {
        int entropy = menu.getEntropy();
        int max = menu.getMaxEntropy();
        if (max > 0) {
            int fill = (int) ((float) entropy / max * BAR_HEIGHT);
            if (fill > 0) {
                guiGraphics.blit(TEXTURE,
                        x + ENTROPY_X, y + BAR_TOP + (BAR_HEIGHT - fill),
                        UV_ENT_U, BAR_HEIGHT - fill,
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
            guiGraphics.renderTooltip(font,
                    Component.literal("Entropy: " + menu.getEntropy() + " / " + menu.getMaxEntropy() + " EN"), mouseX,
                    mouseY);
        }

        // Energy Bar tooltip
        if (relX >= FE_X && relX < FE_X + BAR_WIDTH && relY >= BAR_TOP && relY < BAR_TOP + BAR_HEIGHT) {
            guiGraphics.renderTooltip(font,
                    Component.literal("Energy: " + menu.getEnergyStored() + " / " + menu.getMaxEnergy() + " FE"),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);

        // Render Item Charge Status under the slot
        if (!menu.getSlot(0).getItem().isEmpty()) {
            net.minecraft.world.item.ItemStack stack = menu.getSlot(0).getItem();
            var energyCap = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (energyCap != null && energyCap.getMaxEnergyStored() > 0) {
                int max = energyCap.getMaxEnergyStored();
                int stored = energyCap.getEnergyStored();
                int percentage = (int) (100f * stored / max);

                String percentText = percentage + "%";
                int textWidth = font.width(percentText);
                int textX = 80 + (18 - textWidth) / 2; // Centered under slot (x=80, w=18)
                int textY = 35 + 20; // Below slot (y=35, h=18)

                guiGraphics.drawString(font, percentText, textX, textY, 0x00FF00, false);
            }
        }
    }
}

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

    // Reuse specific texture or generic background
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID,
            "textures/gui/ancient_battery.png");

    public AncientChargerScreen(AncientChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 1000;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderEnergy(guiGraphics, x, y);
    }

    private void renderEnergy(GuiGraphics guiGraphics, int x, int y) {
        int scaledHeight = menu.getScaledEnergy(52);
        guiGraphics.blit(TEXTURE, x + 8, y + 20 + 52 - scaledHeight, 176, 52 - scaledHeight, 16, scaledHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for energy
        if (isHovering(8, 20, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }

        // Tooltip for Item Charge Bar
        if (isHovering(80, 55, 16, 4, mouseX, mouseY) && !menu.getSlot(0).getItem().isEmpty()) {
            guiGraphics.renderTooltip(font, Component.literal("Item Charge"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render "Ancient Charger" title centered
        guiGraphics.drawString(font, Component.translatable("block.alientech.ancient_charger"),
                (imageWidth - font.width(Component.translatable("block.alientech.ancient_charger"))) / 2, 6, 0xD4AF37,
                false);

        // Render Item Charge Bar under the slot
        if (!menu.getSlot(0).getItem().isEmpty()) {
            net.minecraft.world.item.ItemStack stack = menu.getSlot(0).getItem();
            var energyCap = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (energyCap != null && energyCap.getMaxEnergyStored() > 0) {
                int max = energyCap.getMaxEnergyStored();
                int stored = energyCap.getEnergyStored();
                int barWidth = 30; // Wider bar
                int filled = (int) (barWidth * ((float) stored / max));
                int percentage = (int) (100f * stored / max);

                int barX = 65; // Centered under slot
                int barY = 35 + 20; // Below slot

                // Background (dark gray)
                guiGraphics.fill(barX, barY, barX + barWidth, barY + 4, 0xFF333333);

                // Gradient fill based on charge level
                int color;
                if (percentage >= 100) {
                    color = 0xFF00FF00; // Green - fully charged
                } else if (percentage >= 50) {
                    color = 0xFF00AAFF; // Blue - charging well
                } else {
                    color = 0xFFFFAA00; // Orange - low charge
                }
                guiGraphics.fill(barX, barY, barX + filled, barY + 4, color);

                // Border
                guiGraphics.renderOutline(barX - 1, barY - 1, barWidth + 2, 6, 0xFF555555);

                // Percentage text
                String percentText = percentage + "%";
                int textX = barX + barWidth + 4;
                guiGraphics.drawString(font, percentText, textX, barY - 2, 0xFFFFFF, false);
            }
        }
    }
}

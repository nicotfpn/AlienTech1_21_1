package net.nicotfpn.alientech.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.nicotfpn.alientech.client.gui.element.AlienGuiElement;

import java.util.ArrayList;
import java.util.List;

public abstract class AlienScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected final List<AlienGuiElement> leftElements = new ArrayList<>();

    public AlienScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.leftElements.clear(); // Clear old elements on resize/init
        addGuiElements();
    }

    /**
     * Override to add custom elements.
     */
    protected void addGuiElements() {
    }

    protected <E extends AlienGuiElement> E addElement(E element) {
        leftElements.add(element);
        return element;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        drawBackgroundTexture(guiGraphics, x, y);

        // Render elements
        for (AlienGuiElement element : leftElements) {
            element.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    protected void drawBackgroundTexture(GuiGraphics guiGraphics, int x, int y) {
        // Default implementation using standard GUI base
        guiGraphics.blit(AlienInfo.GUI_BASE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render element tooltips/foregrounds here if needed, but AlienGuiElement
        // handles its own foreground
        // We override this to PREVENT default title rendering if we want, or keep it.
        // For now, let's keep super to render title/inventory labels.
        super.renderLabels(guiGraphics, mouseX, mouseY);

        for (AlienGuiElement element : leftElements) {
            if (element.isMouseOver(mouseX, mouseY)) {
                // Elements handle their own tooltips, but if they had "foreground" drawing
                // dependent on layer, do it here
            }
        }
    }

    // Helper accessors for elements
    public int getGuiLeft() {
        return leftPos;
    }

    public int getGuiTop() {
        return topPos;
    }

    public int getXSize() {
        return imageWidth;
    }

    public int getYSize() {
        return imageHeight;
    }
}

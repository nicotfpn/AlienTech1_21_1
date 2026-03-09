package net.nicotfpn.alientech.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.nicotfpn.alientech.client.gui.element.AlienGuiElement;
import net.nicotfpn.alientech.client.gui.element.AlienGuiTab;
import net.nicotfpn.alientech.client.gui.element.AlienSideConfigElement;

import java.util.ArrayList;
import java.util.List;

public abstract class AlienScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected final List<AlienGuiElement> leftElements = new ArrayList<>();
    protected final List<AlienGuiTab> tabs = new ArrayList<>();
    protected boolean configMode = false;
    protected AlienSideConfigElement sideConfigElement;

    public AlienScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.leftElements.clear();
        this.tabs.clear();
        addGuiElements();
    }

    /**
     * Override to add custom elements.
     */
    protected void addGuiElements() {
    }

    protected <E extends AlienGuiElement> E addElement(E element) {
        if (element instanceof AlienGuiTab tab) {
            tabs.add(tab);
        } else {
            leftElements.add(element);
        }
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

        if (configMode && sideConfigElement != null) {
            sideConfigElement.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            // Render elements
            for (AlienGuiElement element : leftElements) {
                element.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        // Render tabs always
        for (AlienGuiTab tab : tabs) {
            tab.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public void setConfigMode(boolean configMode) {
        this.configMode = configMode;
        if (tabs.size() > 0) {
            // Usually the side config tab is the first or second.
            // We should find it and set it active
            // For now, let's just let the tab handle its own active state via the click
            // consumer
        }
    }

    public boolean isConfigMode() {
        return configMode;
    }

    protected void drawBackgroundTexture(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(AlienInfo.GUI_BASE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Element tooltips are handled in their own drawForeground called by
        // renderWidget
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AlienGuiTab tab : tabs) {
            if (tab.mouseClicked(mouseX, mouseY, button))
                return true;
        }
        for (AlienGuiElement element : leftElements) {
            if (element.mouseClicked(mouseX, mouseY, button))
                return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    public net.minecraft.client.Minecraft getMinecraft() {
        return minecraft;
    }
}

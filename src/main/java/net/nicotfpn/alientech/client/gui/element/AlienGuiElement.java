package net.nicotfpn.alientech.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.nicotfpn.alientech.client.gui.AlienScreen;

public abstract class AlienGuiElement extends AbstractWidget {

    protected final AlienScreen<?> screen;
    protected final int relativeX;
    protected final int relativeY;

    public AlienGuiElement(AlienScreen<?> screen, int x, int y, int width, int height) {
        super(screen.getGuiLeft() + x, screen.getGuiTop() + y, width, height, Component.empty());
        this.screen = screen;
        this.relativeX = x;
        this.relativeY = y;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        // No-op for now
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Update position in case the window moved
        this.setX(screen.getGuiLeft() + relativeX);
        this.setY(screen.getGuiTop() + relativeY);

        drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        drawForeground(guiGraphics, mouseX, mouseY);
    }

    public abstract void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);

    public void drawForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            renderToolTip(guiGraphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

}

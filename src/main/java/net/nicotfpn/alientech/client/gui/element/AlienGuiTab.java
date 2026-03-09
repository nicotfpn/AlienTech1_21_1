package net.nicotfpn.alientech.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.client.gui.AlienScreen;

import java.util.function.Consumer;

/**
 * A side tab for AlienTech GUIs, inspired by Mekanism's tabs.
 * Can be placed on either side and supports icons and tooltips.
 */
public class AlienGuiTab extends AlienGuiElement {

    private final ResourceLocation icon;
    private final Component tooltip;
    private final Consumer<AlienGuiTab> onClick;
    private boolean active = false;

    public AlienGuiTab(AlienScreen<?> screen, int x, int y, int width, int height,
            ResourceLocation icon, Component tooltip, Consumer<AlienGuiTab> onClick) {
        super(screen, x, y, width, height);
        this.icon = icon;
        this.tooltip = tooltip;
        this.onClick = onClick;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        // Tab background color (Teal-ish for active, dark for inactive)
        int color = active ? 0xFF00ADB5 : 0xFF222831; // #00ADB5 (Active Teal), #222831 (Dark Gray)

        // Draw the tab "ear" sticking out
        // Assuming it's on the right side of the main GUI (at imageWidth)
        guiGraphics.fill(x, y, x + width, y + height, color);

        // Draw border
        int borderColor = active ? 0xFF00FFF5 : 0xFF393E46; // Lighter border
        guiGraphics.fill(x, y, x + width, y + 1, borderColor); // Top
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor); // Right

        // Icon
        if (icon != null) {
            guiGraphics.blit(icon, x + (width - 16) / 2, y + (height - 16) / 2, 0, 0, 16, 16, 16, 16);
        } else {
            // Placeholder text if no icon
            guiGraphics.drawString(screen.getMinecraft().font, "?", x + (width - 6) / 2, y + (height - 8) / 2,
                    0xFFFFFF);
        }

        // Hover highlight
        if (isMouseOver(mouseX, mouseY)) {
            guiGraphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onClick.accept(this);
    }

    @Override
    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderTooltip(screen.getMinecraft().font, tooltip, mouseX, mouseY);
    }
}

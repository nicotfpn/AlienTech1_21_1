package net.nicotfpn.alientech.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.nicotfpn.alientech.client.gui.AlienScreen;
import net.nicotfpn.alientech.util.EnergyUtils;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * A standard vertical Energy Gauge, inspired by Mekanism's GuiEnergyGauge.
 * Renders a bar that fills up based on energy stored.
 */
public class AlienEnergyGauge extends AlienGuiElement {

    // Textures for the gauge
    // private static final ResourceLocation GLASS_TEXTURE =
    // ResourceLocation.fromNamespaceAndPath("alientech",
    // "textures/gui/gauges/glass.png");
    // private static final ResourceLocation BAR_TEXTURE =
    // ResourceLocation.fromNamespaceAndPath("alientech",
    // "textures/gui/gauges/bar.png");

    private final LongSupplier storedEnergy;
    private final LongSupplier maxEnergy;
    private final int width;
    private final int height;

    // Colors (ARGB)
    // Gold: 0xFFFFD700
    // Teal: 0xFF008080
    // We will use a Cyan/Gold mix for the bar
    private static final int BAR_COLOR = 0xFF00FFFF; // Neon Cyan

    public AlienEnergyGauge(AlienScreen<?> screen, int x, int y, int width, int height,
            LongSupplier storedEnergy, LongSupplier maxEnergy) {
        super(screen, x, y, width, height);
        this.storedEnergy = storedEnergy;
        this.maxEnergy = maxEnergy;
        this.width = width;
        this.height = height;
    }

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = this.getX();
        int y = this.getY();

        // 1. Draw Background (Dark Slot)
        guiGraphics.fill(x, y, x + width, y + height, 0xFF000000); // Black background

        // 2. Calculate Fill Height
        long stored = storedEnergy.getAsLong();
        long max = maxEnergy.getAsLong();
        if (max <= 0)
            max = 1;

        float fillRatio = (float) stored / max;
        int fillHeight = (int) (fillRatio * height);

        // 3. Draw Energy Bar
        if (fillHeight > 0) {
            int fillY = y + height - fillHeight;
            guiGraphics.fill(x + 1, fillY, x + width - 1, y + height, BAR_COLOR);

            // Add a "glint" or lighter color on the left for 3D effect
            guiGraphics.fill(x + 1, fillY, x + 2, y + height, 0x88FFFFFF);
        }

        // 4. Draw Frame/Border (if we had a texture, we'd blit it here. For now, a
        // clean border)
        // Top
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF555555);
        // Bottom
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF555555);
        // Left
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF555555);
        // Right
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF555555);
    }

    @Override
    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        long stored = storedEnergy.getAsLong();
        long max = maxEnergy.getAsLong();

        String storedText = EnergyUtils.formatCompact(stored);
        String maxText = EnergyUtils.formatCompact(max);
        float percentage = (float) stored / max * 100;

        guiGraphics.renderComponentTooltip(screen.getMinecraft().font, List.of(
                Component.literal("§6Energy"),
                Component.literal("§b" + storedText + " / " + maxText + " FE"),
                Component.literal("§7" + String.format("%.1f%%", percentage))), mouseX, mouseY);
    }
}

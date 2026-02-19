package net.nicotfpn.alientech.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicotfpn.alientech.block.entity.ISideConfigurable;
import net.nicotfpn.alientech.block.entity.SideConfiguration.RelativeSide;
import net.nicotfpn.alientech.block.entity.SideMode;
import net.nicotfpn.alientech.network.SideConfigPacket;

/**
 * Screen for configuring I/O sides of a machine.
 * Similar to Mekanism's side configuration GUI.
 */
public class SideConfigScreen extends Screen {

    // GUI dimensions
    private static final int GUI_WIDTH = 140;
    private static final int GUI_HEIGHT = 140;

    // Button size
    private static final int BUTTON_SIZE = 24;

    // The block entity being configured
    private final ISideConfigurable configurable;
    private final BlockPos blockPos;
    private final Screen parentScreen;

    public SideConfigScreen(ISideConfigurable configurable, BlockPos pos, Screen parentScreen) {
        super(Component.translatable("gui.alientech.side_config"));
        this.configurable = configurable;
        this.blockPos = pos;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = (width - GUI_WIDTH) / 2;
        int centerY = (height - GUI_HEIGHT) / 2;

        int row1Y = centerY + 15; // TOP
        int row2Y = centerY + 45; // LEFT, FRONT, RIGHT
        int row3Y = centerY + 75; // BACK
        int row4Y = centerY + 105; // BOTTOM

        int centerBtnX = centerX + (GUI_WIDTH - BUTTON_SIZE) / 2;
        int leftBtnX = centerX + 15;
        int rightBtnX = centerX + GUI_WIDTH - BUTTON_SIZE - 15;

        // Create buttons
        addRenderableWidget(new SideButton(
                centerBtnX, row1Y, BUTTON_SIZE, RelativeSide.TOP));

        addRenderableWidget(new SideButton(
                leftBtnX, row2Y, BUTTON_SIZE, RelativeSide.LEFT));

        addRenderableWidget(new SideButton(
                centerBtnX, row2Y, BUTTON_SIZE, RelativeSide.FRONT));

        addRenderableWidget(new SideButton(
                rightBtnX, row2Y, BUTTON_SIZE, RelativeSide.RIGHT));

        addRenderableWidget(new SideButton(
                centerBtnX, row3Y, BUTTON_SIZE, RelativeSide.BACK));

        addRenderableWidget(new SideButton(
                centerBtnX, row4Y, BUTTON_SIZE, RelativeSide.BOTTOM));

        // Done button
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                .bounds(centerX + 10, centerY + GUI_HEIGHT - 25, GUI_WIDTH - 20, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw darkened background
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;

        // Draw background panel (Egyptian style - sand color with gold border)
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Background fill
        guiGraphics.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFC2B280); // Sand color

        // Gold border
        guiGraphics.fill(x, y, x + GUI_WIDTH, y + 3, 0xFFD4AF37); // Top
        guiGraphics.fill(x, y + GUI_HEIGHT - 3, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B7332); // Bottom
        guiGraphics.fill(x, y, x + 3, y + GUI_HEIGHT, 0xFFD4AF37); // Left
        guiGraphics.fill(x + GUI_WIDTH - 3, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B7332); // Right

        // Title
        String title = "Side Configuration";
        int titleX = x + (GUI_WIDTH - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, y + 5, 0xD4AF37, false);

        // Render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw legend
        int legendY = y + GUI_HEIGHT + 5;
        guiGraphics.drawString(font, "§7Left-click: Cycle mode", x, legendY, 0xFFFFFF, false);
        guiGraphics.drawString(font, "§7Right-click: Reverse", x, legendY + 10, 0xFFFFFF, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null && parentScreen != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Custom button for each side of the block.
     * Shows the current mode with appropriate color.
     */
    private class SideButton extends Button {
        private final RelativeSide side;

        public SideButton(int x, int y, int size, RelativeSide side) {
            super(Button.builder(Component.literal(side.getName().substring(0, 1).toUpperCase()),
                    btn -> {
                    }) // Placeholder - we override onPress
                    .bounds(x, y, size, size));
            this.side = side;
        }

        @Override
        public void onPress() {
            handleClick(false);
        }

        private void handleClick(boolean reverse) {
            // Send packet to server
            PacketDistributor.sendToServer(new SideConfigPacket(
                    blockPos,
                    side.ordinal(),
                    reverse));

            // Optimistic update on client
            if (reverse) {
                configurable.getSideConfiguration().cycleModeReverse(side);
            } else {
                configurable.getSideConfiguration().cycleMode(side);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.isMouseOver(mouseX, mouseY)) {
                if (button == 1) { // Right click
                    handleClick(true);
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
            return false;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            SideMode mode = configurable.getSideConfiguration().getMode(side);
            int color = mode.getColor();

            // Button background with mode color
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);

            // 3D border effect
            int lightColor = brighten(color);
            int darkColor = darken(color);
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + 2, lightColor);
            guiGraphics.fill(getX(), getY(), getX() + 2, getY() + getHeight(), lightColor);
            guiGraphics.fill(getX(), getY() + getHeight() - 2, getX() + getWidth(), getY() + getHeight(), darkColor);
            guiGraphics.fill(getX() + getWidth() - 2, getY(), getX() + getWidth(), getY() + getHeight(), darkColor);

            // Mode text (abbreviation)
            String modeText = switch (mode) {
                case NONE -> "X";
                case INPUT -> "I";
                case OUTPUT -> "O";
                case BOTH -> "B";
            };

            int textX = getX() + (getWidth() - font.width(modeText)) / 2;
            int textY = getY() + (getHeight() - 8) / 2;
            guiGraphics.drawString(font, modeText, textX, textY, 0xFFFFFF, true);

            // Side label below
            String label = side.getName().substring(0, 1).toUpperCase();
            int labelX = getX() + (getWidth() - font.width(label)) / 2;
            guiGraphics.drawString(font, label, labelX, getY() + getHeight() + 2, 0x555555, false);

            // Hover effect
            if (isHovered) {
                guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);
            }
        }

        private int brighten(int color) {
            int r = Math.min(255, ((color >> 16) & 0xFF) + 40);
            int g = Math.min(255, ((color >> 8) & 0xFF) + 40);
            int b = Math.min(255, (color & 0xFF) + 40);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        private int darken(int color) {
            int r = Math.max(0, ((color >> 16) & 0xFF) - 40);
            int g = Math.max(0, ((color >> 8) & 0xFF) - 40);
            int b = Math.max(0, (color & 0xFF) - 40);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int width,
                int color) {
            // Don't render default text - we handle it ourselves
        }
    }
}

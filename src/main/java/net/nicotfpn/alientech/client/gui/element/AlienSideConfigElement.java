package net.nicotfpn.alientech.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicotfpn.alientech.client.gui.AlienScreen;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;
import net.nicotfpn.alientech.network.packet.ServerboundSideConfigPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Technical Schematic Side Configuration Panel.
 * Shows all 6 faces of the machine for a specific CapabilityType.
 */
public class AlienSideConfigElement extends AlienGuiElement {

    private final AlienMachineBlockEntity machine;
    private CapabilityType currentType = CapabilityType.ITEM;
    private final List<FaceButton> faceButtons = new ArrayList<>();

    public AlienSideConfigElement(AlienScreen<?> screen, int x, int y, AlienMachineBlockEntity machine) {
        super(screen, x, y, 150, 130);
        this.machine = machine;

        // Initialize face buttons in an unfolded layout
        // Center of the panel is roughly (75, 65)
        int bSize = 24;
        int cx = 75 - bSize / 2;
        int cy = 65 - bSize / 2;

        faceButtons.add(new FaceButton(cx, cy - bSize - 2, Direction.UP)); // TOP
        faceButtons.add(new FaceButton(cx - bSize - 2, cy, Direction.WEST)); // LEFT
        faceButtons.add(new FaceButton(cx, cy, Direction.NORTH)); // FRONT
        faceButtons.add(new FaceButton(cx + bSize + 2, cy, Direction.EAST)); // RIGHT
        faceButtons.add(new FaceButton(cx, cy + bSize + 2, Direction.DOWN)); // BOTTOM
        faceButtons.add(new FaceButton(cx + 2 * (bSize + 2), cy, Direction.SOUTH)); // BACK
    }

    public void setType(CapabilityType type) {
        this.currentType = type;
    }

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        // 1. Draw Panel Background (Egyptian Slate)
        guiGraphics.fill(x, y, x + width, y + height, 0xEE1A1C20); // Darker gray

        // 2. Draw Borders (Gold highlights)
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFD4AF37);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFFD4AF37);

        // 3. Draw Type Selectors (Simplified for now)
        drawTypeButton(guiGraphics, x + 5, y + 5, "ITEMS", currentType == CapabilityType.ITEM);
        drawTypeButton(guiGraphics, x + 45, y + 5, "ENERGY", currentType == CapabilityType.ENERGY);
        drawTypeButton(guiGraphics, x + 95, y + 5, "ENTROPY", currentType == CapabilityType.ENTROPY);

        // 4. Draw Face Buttons
        for (FaceButton btn : faceButtons) {
            btn.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    private void drawTypeButton(GuiGraphics guiGraphics, int x, int y, String label, boolean active) {
        int color = active ? 0xFF00ADB5 : 0xFF393E46;
        guiGraphics.fill(x, y, x + 35, y + 12, color);
        guiGraphics.drawString(screen.getMinecraft().font, label, x + 2, y + 2, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible)
            return false;

        // Check Type Selectors
        int x = getX();
        int y = getY();
        if (isWithin(mouseX, mouseY, x + 5, y + 5, 35, 12)) {
            currentType = CapabilityType.ITEM;
            return true;
        }
        if (isWithin(mouseX, mouseY, x + 45, y + 5, 35, 12)) {
            currentType = CapabilityType.ENERGY;
            return true;
        }
        if (isWithin(mouseX, mouseY, x + 95, y + 5, 35, 12)) {
            currentType = CapabilityType.ENTROPY;
            return true;
        }

        // Check Face Buttons
        for (FaceButton btn : faceButtons) {
            if (btn.mouseClicked(mouseX, mouseY, button))
                return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isWithin(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Inner class for the face buttons to keep logic contained. */
    private class FaceButton {
        private final int relX;
        private final int relY;
        private final Direction face;

        public FaceButton(int relX, int relY, Direction face) {
            this.relX = relX;
            this.relY = relY;
            this.face = face;
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            int x = getX() + relX;
            int y = getY() + relY;
            int size = 24;

            SideConfigComponent config = machine.getComponent(SideConfigComponent.class);
            IOSideMode mode = config.getMode(face, currentType);
            int color = mode.getColor();

            // Draw button base
            guiGraphics.fill(x, y, x + size, y + size, color);

            // Highlight/Shadow
            guiGraphics.fill(x, y, x + size, y + 1, 0x80FFFFFF); // Top
            guiGraphics.fill(x, y, x + 1, y + size, 0x80FFFFFF); // Left
            guiGraphics.fill(x, y + size - 1, x + size, y + size, 0x80000000); // Bottom
            guiGraphics.fill(x + size - 1, y, x + size, y + size, 0x80000000); // Right

            // Face Label
            String label = face.name().substring(0, 1);
            guiGraphics.drawString(screen.getMinecraft().font, label, x + 2, y + 2, 0xFFFFFF, true);

            // Mode Indicator (Dot)
            if (mode != IOSideMode.NONE) {
                guiGraphics.fill(x + size - 5, y + size - 5, x + size - 2, y + size - 2, 0xFFFFFFFF);
            }

            if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
                guiGraphics.fill(x, y, x + size, y + size, 0x40FFFFFF);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int x = getX() + relX;
            int y = getY() + relY;
            int size = 24;

            if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
                SideConfigComponent config = machine.getComponent(SideConfigComponent.class);
                IOSideMode currentMode = config.getMode(face, currentType);
                IOSideMode nextMode;

                if (button == 1) { // Right Click - Reverse
                    nextMode = cycleReverse(currentMode);
                } else { // Left Click - Forward
                    nextMode = cycleForward(currentMode);
                }

                // Send Packet
                PacketDistributor.sendToServer(new ServerboundSideConfigPacket(
                        machine.getBlockPos(), face, currentType, nextMode));

                // Optimistic Client Update
                config.setMode(face, currentType, nextMode);
                return true;
            }
            return false;
        }

        private IOSideMode cycleForward(IOSideMode mode) {
            IOSideMode[] values = IOSideMode.values();
            return values[(mode.ordinal() + 1) % values.length];
        }

        private IOSideMode cycleReverse(IOSideMode mode) {
            IOSideMode[] values = IOSideMode.values();
            return values[(mode.ordinal() - 1 + values.length) % values.length];
        }
    }
}

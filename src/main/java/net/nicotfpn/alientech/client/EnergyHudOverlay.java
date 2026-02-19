package net.nicotfpn.alientech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * EnergyHudOverlay — Modular HUD that renders energy info when looking at
 * blocks.
 *
 * Architecture:
 * 1. If the BlockEntity implements IHudProvider, it delegates ALL rendering to
 * it.
 * 2. If NOT an IHudProvider but still has energy, shows a basic "Energy: X / Y
 * FE" line.
 * 3. Position: Rendered above the hotbar at a safe Y offset that doesn't
 * overlap.
 */
public class EnergyHudOverlay {

    /** Base Y offset from the bottom of the screen (above armor/hotbar). */
    private static final int BASE_Y_OFFSET = 59;
    /** Line spacing in pixels. */
    private static final int LINE_HEIGHT = 12;

    public static final LayeredDraw.Layer HUD_ENERGY = (guiGraphics, partialTick) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
            return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        HitResult hitResult = minecraft.hitResult;

        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK)
            return;

        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos pos = blockHitResult.getBlockPos();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        if (blockEntity == null)
            return;

        // Collect lines to render
        List<Component> lines = new ArrayList<>();

        // Title: Block name (always shown, white)
        Component blockName = blockEntity.getBlockState().getBlock().getName();

        if (blockEntity instanceof IHudProvider hudProvider) {
            // Delegate rendering to the BlockEntity itself
            hudProvider.addHudLines(lines);
        } else {
            // Fallback: Generic energy display for any energy block
            IEnergyStorage energy = minecraft.level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, pos, blockHitResult.getDirection());
            if (energy != null) {
                String stored = formatNumber(energy.getEnergyStored());
                String max = formatNumber(energy.getMaxEnergyStored());
                lines.add(Component.literal("⚡ " + stored + " / " + max + " FE").withColor(0xD4AF37));
            }
        }

        // Only render if we have something to show
        if (lines.isEmpty())
            return;

        // Render block name
        int totalLines = lines.size() + 1; // +1 for block name
        int startY = screenHeight - BASE_Y_OFFSET - (totalLines * LINE_HEIGHT);

        int nameWidth = minecraft.font.width(blockName);
        guiGraphics.drawString(minecraft.font, blockName,
                (screenWidth - nameWidth) / 2, startY, 0xFFFFFF, true);

        // Render info lines
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lineWidth = minecraft.font.width(line);
            int lineY = startY + ((i + 1) * LINE_HEIGHT);
            guiGraphics.drawString(minecraft.font, line,
                    (screenWidth - lineWidth) / 2, lineY, 0xFFFFFF, true);
        }
    };

    private static String formatNumber(int val) {
        if (val >= 1_000_000) {
            return String.format("%.1fM", val / 1_000_000.0);
        } else if (val >= 1_000) {
            return String.format("%.1fk", val / 1_000.0);
        }
        return String.valueOf(val);
    }
}

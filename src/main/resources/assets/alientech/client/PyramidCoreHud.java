package net.nicotfpn.alientech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.nicotfpn.alientech.block.custom.PyramidCoreBlock;
import net.nicotfpn.alientech.block.entity.PyramidCoreBlockEntity;

public class PyramidCoreHud {
    private static final int HUD_WIDTH = 180;
    private static final int HUD_HEIGHT = 60;
    private static final int ENERGY_BAR_WIDTH = 150;
    private static final int ENERGY_BAR_HEIGHT = 8;
    
    public static void renderHud(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Verifica se o jogador está olhando para um Pyramid Core
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;
        
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        Level level = mc.level;
        
        if (!(level.getBlockState(pos).getBlock() instanceof PyramidCoreBlock)) return;
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof PyramidCoreBlockEntity pyramidCore)) return;
        
        // Obter dados
        int energy = pyramidCore.getEnergyStorage().getEnergyStored();
        int maxEnergy = pyramidCore.getEnergyStorage().getMaxEnergyStored();
        ItemStack eyeStack = pyramidCore.getItemHandler().getStackInSlot(0);
        boolean isActive = pyramidCore.isActive();
        boolean isStructureValid = pyramidCore.isStructureValid();
        
        // Renderizar HUD
        renderPyramidCoreHud(guiGraphics, mc, energy, maxEnergy, eyeStack, isActive, isStructureValid);
    }
    
    private static void renderPyramidCoreHud(GuiGraphics guiGraphics, Minecraft mc, 
                                            int energy, int maxEnergy, ItemStack eyeStack,
                                            boolean isActive, boolean isStructureValid) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Posição centralizada no topo da tela
        int x = (screenWidth - HUD_WIDTH) / 2;
        int y = 10;
        
        // Fundo semi-transparente
        guiGraphics.fill(x - 5, y - 5, x + HUD_WIDTH + 5, y + HUD_HEIGHT + 5, 0x90000000);
        
        // Borda decorativa
        int borderColor = isActive ? 0xFF00FF00 : (isStructureValid ? 0xFF0088FF : 0xFFFF0000);
        guiGraphics.fill(x - 5, y - 5, x + HUD_WIDTH + 5, y - 3, borderColor); // Top
        guiGraphics.fill(x - 5, y + HUD_HEIGHT + 3, x + HUD_WIDTH + 5, y + HUD_HEIGHT + 5, borderColor); // Bottom
        guiGraphics.fill(x - 5, y - 3, x - 3, y + HUD_HEIGHT + 3, borderColor); // Left
        guiGraphics.fill(x + HUD_WIDTH + 3, y - 3, x + HUD_WIDTH + 5, y + HUD_HEIGHT + 3, borderColor); // Right
        
        // Título
        Component title = Component.literal("⚡ Pyramid Core ⚡").withStyle(style -> style.withBold(true));
        guiGraphics.drawString(mc.font, title, x + (HUD_WIDTH - mc.font.width(title)) / 2, y, 0xFFFFFF, true);
        
        // Status
        int statusY = y + 12;
        String statusText = isActive ? "§aActive" : (isStructureValid ? "§7Inactive" : "§cInvalid Structure");
        Component statusComponent = Component.literal("Status: " + statusText);
        guiGraphics.drawString(mc.font, statusComponent, x + 5, statusY, 0xFFFFFF, false);
        
        // Energia - Texto
        int energyTextY = statusY + 12;
        String energyText = String.format("Energy: §b%,d§r / §6%,d§r FE", energy, maxEnergy);
        guiGraphics.drawString(mc.font, Component.literal(energyText), x + 5, energyTextY, 0xFFFFFF, false);
        
        // Barra de energia
        int energyBarY = energyTextY + 12;
        int energyBarX = x + 15;
        
        // Fundo da barra
        guiGraphics.fill(energyBarX - 1, energyBarY - 1, 
                        energyBarX + ENERGY_BAR_WIDTH + 1, energyBarY + ENERGY_BAR_HEIGHT + 1, 
                        0xFF333333);
        
        // Barra de energia preenchida
        float energyPercent = maxEnergy > 0 ? (float) energy / maxEnergy : 0;
        int filledWidth = (int) (ENERGY_BAR_WIDTH * energyPercent);
        
        // Gradiente de cor baseado na porcentagem
        int barColor = getEnergyBarColor(energyPercent);
        guiGraphics.fill(energyBarX, energyBarY, 
                        energyBarX + filledWidth, energyBarY + ENERGY_BAR_HEIGHT, 
                        barColor);
        
        // Porcentagem na barra
        String percentText = String.format("%.1f%%", energyPercent * 100);
        int percentX = energyBarX + (ENERGY_BAR_WIDTH - mc.font.width(percentText)) / 2;
        guiGraphics.drawString(mc.font, Component.literal(percentText), percentX, energyBarY, 0xFFFFFF, true);
        
        // Item no slot (Eye of Horus)
        int itemY = energyBarY + 12;
        if (!eyeStack.isEmpty()) {
            // Renderizar ícone do item
            guiGraphics.renderItem(eyeStack, x + 5, itemY);
            
            // Nome do item
            Component itemName = eyeStack.getHoverName();
            guiGraphics.drawString(mc.font, itemName, x + 25, itemY + 1, 0xFFFFFF, false);
            
            // Durabilidade se for danificável
            if (eyeStack.isDamageableItem()) {
                int damage = eyeStack.getDamageValue();
                int maxDamage = eyeStack.getMaxDamage();
                int durability = maxDamage - damage;
                String durabilityText = String.format("§e%d§r / §6%d", durability, maxDamage);
                guiGraphics.drawString(mc.font, Component.literal(durabilityText), 
                                      x + 25, itemY + 10, 0xFFFFFF, false);
            }
        } else {
            Component emptyText = Component.literal("§7No Eye of Horus");
            guiGraphics.drawString(mc.font, emptyText, x + 5, itemY + 4, 0xFFFFFF, false);
        }
    }
    
    private static int getEnergyBarColor(float percent) {
        if (percent >= 0.75f) {
            // Verde (75-100%)
            return 0xFF00FF00;
        } else if (percent >= 0.50f) {
            // Amarelo (50-75%)
            return 0xFFFFFF00;
        } else if (percent >= 0.25f) {
            // Laranja (25-50%)
            return 0xFFFF8800;
        } else {
            // Vermelho (0-25%)
            return 0xFFFF0000;
        }
    }
}
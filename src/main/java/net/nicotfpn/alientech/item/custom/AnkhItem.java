package net.nicotfpn.alientech.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nicotfpn.alientech.block.entity.PyramidCoreBlockEntity;
import org.jetbrains.annotations.NotNull;

public class AnkhItem extends Item {

    private static final int COOLDOWN_TICKS = 40; // 2 segundos (20 ticks = 1s)

    public AnkhItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Só funciona no servidor
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            // Verifica se clicou no Pyramid Core
            if (blockEntity instanceof PyramidCoreBlockEntity pyramidCore) {
                // Toggle: se está ativo, desativa. Se está inativo, tenta ativar
                if (pyramidCore.isActive()) {
                    // Desativar a pirâmide
                    pyramidCore.deactivatePyramid();

                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);

                    player.sendSystemMessage(Component.translatable("message.alientech.pyramid_deactivated"));
                    return InteractionResult.SUCCESS;
                }

                // Tenta ativar a pirâmide
                if (pyramidCore.activatePyramid()) {
                    // Sucesso! Tocar som e partículas
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);

                    ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            20, 0.5, 0.5, 0.5, 0.05);

                    // NÃO consome o Ankh - ele serve apenas para toggle
                    return InteractionResult.SUCCESS;
                } else {
                    // Falha na ativação - verificar o motivo
                    if (pyramidCore.getItemHandler().getStackInSlot(0).isEmpty()) {
                        // Falta o Alloy
                        player.sendSystemMessage(Component.translatable("message.alientech.missing_graviton"));
                    } else {
                        // Estrutura inválida
                        player.sendSystemMessage(Component.translatable("message.alientech.invalid_structure"));
                    }
                    return InteractionResult.FAIL;
                }
            }

            Block clickedBlock = level.getBlockState(pos).getBlock();

            // Efeito original em Sandstone
            if (clickedBlock == Blocks.SANDSTONE ||
                    clickedBlock == Blocks.CHISELED_SANDSTONE ||
                    clickedBlock == Blocks.CUT_SANDSTONE ||
                    clickedBlock == Blocks.SMOOTH_SANDSTONE) {

                if (!player.getCooldowns().isOnCooldown(this)) {
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);

                    ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            10, 0.3, 0.3, 0.3, 0.02);

                    player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                }

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
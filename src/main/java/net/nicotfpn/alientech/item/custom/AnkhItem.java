package net.nicotfpn.alientech.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.nicotfpn.alientech.block.ModBlocks;

import static net.minecraft.world.level.block.SculkSensorBlock.canActivate;

public class AnkhItem extends Item {

    public AnkhItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Block clickedBlock = level.getBlockState(pos).getBlock();

        // Só funciona no servidor
    //    if (!level.isClientSide && player != null) {

            // Verifica se clicou em um bloco que pode ser ativado
            //  if (canActivate(clickedBlock)) {
            // ATIVA O BLOCO!
            //   activateBlock(level, pos, player, clickedBlock);
            //   return InteractionResult.SUCCESS;
            // } else {
            //      // Mensagem de erro
            //      player.displayClientMessage(
            //             Component.literal("§c✗ Este bloco não pode ser ativado com o Ankh!"),
            //          true
            //  );
//return InteractionResult.FAIL;
            //      }
            //   }

           // return InteractionResult.sidedSuccess(level.isClientSide);
        }

        //   private boolean canActivate(Block block) {
        // Lista de blocos que podem ser ativados
        //    return block == ModBlocks.PYRAMID_CAPSTONE.get()
        // Adicione aqui outros blocos quando criar:
        // || block == ModBlocks.ANCIENT_FABRICATOR.get()
        // || block == ModBlocks.GRAVITON_INFUSER.get()
        ;
   }

    private void activateBlock(Level level, BlockPos pos, Player player, Block block) {
        // Efeitos visuais e sonoros
        level.playSound(null, pos,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1.0f, 1.0f);

        // Mensagem de sucesso
        player.displayClientMessage(
                Component.literal("§6✓ Bloco ativado com o poder do Ankh!"),
                true
        );

        // TODO: Aqui você vai adicionar a lógica específica de cada bloco
        // Por exemplo:
     //   if (block == ModBlocks.PYRAMID_CAPSTONE.get()) {
            // Verifica e ativa pirâmide
          //  activatePyramid(level, pos, player);
    //
        // Se quiser que o Ankh tenha durabilidade:
        // context.getItemInHand().hurtAndBreak(1, player, ...);
    }

    private void activatePyramid(Level level, BlockPos pos, Player player) {
        // Aqui você pode chamar a verificação da pirâmide
        player.displayClientMessage(
                Component.literal("§d§l⚡ VERIFICANDO ESTRUTURA DA PIRÂMIDE... ⚡"),
                false
        );

        // TODO: Adicionar verificação da estrutura completa
    }


void main() {
}
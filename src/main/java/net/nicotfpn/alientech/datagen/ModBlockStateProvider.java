package net.nicotfpn.alientech.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.ModBlocks;

public class ModBlockStateProvider extends BlockStateProvider {
        public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
                super(output, AlienTech.MOD_ID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
                // Blocos básicos
                simpleBlockWithItem(ModBlocks.NEUTRION_ORE.get(),
                                models().cubeAll("neutrion_ore", modLoc("block/neutrion_ore")));

                simpleBlockWithItem(ModBlocks.NEUTRION_BLOCK.get(),
                                models().cubeAll("neutrion_block", modLoc("block/neutrion_block")));

                // ❌ Primal Catalyst e Pyramid Core NÃO devem ser registrados aqui via datagen
                // Pois eles possuem modelos complexos manuais em src/main/resources
                // Se registrados aqui, o datagen cria cubos simples que sobrescrevem os
                // manuais.
        }
}
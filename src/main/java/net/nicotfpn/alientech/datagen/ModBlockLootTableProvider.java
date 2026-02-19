package net.nicotfpn.alientech.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.nicotfpn.alientech.block.ModBlocks;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    protected ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        // Blocos que dropam eles mesmos
        dropSelf(ModBlocks.NEUTRION_BLOCK.get());
        dropSelf(ModBlocks.PRIMAL_CATALYST.get());
        dropSelf(ModBlocks.PYRAMID_CORE.get());

        // Bloco de minério - dropa o próprio bloco (você pode mudar depois para dropar
        // raw_neutrion)
        dropSelf(ModBlocks.NEUTRION_ORE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(entry -> (Block) entry.value())
                .toList();
    }
}

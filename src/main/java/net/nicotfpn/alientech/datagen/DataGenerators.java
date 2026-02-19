package net.nicotfpn.alientech.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.nicotfpn.alientech.AlienTech;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
                DataGenerator generator = event.getGenerator();
                PackOutput output = generator.getPackOutput();
                ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
                CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

                // Client providers
                // ⚠️ ORDEM IMPORTANTE: BlockStateProvider ANTES de ItemModelProvider
                generator.addProvider(event.includeClient(),
                                new ModBlockStateProvider(output, existingFileHelper)); // 1º - Cria modelos de blocos

                generator.addProvider(event.includeClient(),
                                new ModItemModelProvider(output, existingFileHelper)); // 2º - Referencia modelos de
                                                                                       // blocos

                // Server providers - Loot Tables
                generator.addProvider(event.includeServer(),
                                new LootTableProvider(output, Collections.emptySet(),
                                                List.of(new LootTableProvider.SubProviderEntry(
                                                                ModBlockLootTableProvider::new,
                                                                LootContextParamSets.BLOCK)),
                                                lookupProvider));
        }
}
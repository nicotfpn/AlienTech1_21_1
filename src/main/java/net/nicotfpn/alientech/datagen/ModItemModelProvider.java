package net.nicotfpn.alientech.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.nicotfpn.alientech.AlienTech;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AlienTech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Itens básicos
        simpleItem("graviton");
        simpleItem("gravion_disk");
        simpleItem("neutrion_ingot");
        simpleItem("raw_neutrion");
        simpleItem("ancient_ankh");

        // Novos itens baseados nas suas texturas
        simpleItem("concentrated_substrate");
        simpleItem("exotic_substrate");
        simpleItem("graviton_contained");
        simpleItem("horus_eye");
        simpleItem("horus_eye_activated");
        simpleItem("inertial_stability_alloy");
        simpleItem("rainbow_captured");

        // ❌ REMOVIDO: Block items - NÃO registre aqui
        // O ModBlockStateProvider vai cuidar disso
        // withExistingParent("primal_catalyst", modLoc("block/primal_catalyst"));
        // withExistingParent("pyramid_core", modLoc("block/pyramid_core"));
    }

    private void simpleItem(String name) {
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
    }
}
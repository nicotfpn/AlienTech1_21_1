package net.nicotfpn.alientech.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.item.custom.ActivatedEyeOfHorusItem;
import net.nicotfpn.alientech.item.custom.AnkhItem;
import net.nicotfpn.alientech.item.custom.EnergyEyeOfHorusItem;
import net.nicotfpn.alientech.item.custom.GravitonItem;
import net.nicotfpn.alientech.item.custom.DecayingGravitonItem;
import net.nicotfpn.alientech.item.custom.PocketDimensionalPrisonItem;

import net.nicotfpn.alientech.item.custom.PharaohSwordItem;

public class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AlienTech.MOD_ID);

        // Itens existentes
        public static final DeferredItem<Item> GRAVITON = ITEMS.register("graviton",
                        () -> new GravitonItem(new Item.Properties()
                                        .stacksTo(16)
                                        .rarity(Rarity.UNCOMMON)));

        public static final DeferredItem<Item> GRAVION_DISK = ITEMS.register("gravion_disk",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> NEUTRION_INGOT = ITEMS.register("neutrion_ingot",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> RAW_NEUTRION = ITEMS.register("raw_neutrion",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> ANCIENT_ANKH = ITEMS.register("ancient_ankh",
                        () -> new AnkhItem(new Item.Properties().stacksTo(1)));

        // Novos itens baseados nas suas texturas
        public static final DeferredItem<Item> CONCENTRATED_SUBSTRATE = ITEMS.register("concentrated_substrate",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> EXOTIC_SUBSTRATE = ITEMS.register("exotic_substrate",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> GRAVITON_CONTAINED = ITEMS.register("graviton_contained",
                        () -> new Item(new Item.Properties()
                                        .stacksTo(1)
                                        .rarity(Rarity.RARE)));

        public static final DeferredItem<Item> HORUS_EYE = ITEMS.register("horus_eye",
                        () -> new EnergyEyeOfHorusItem(new Item.Properties()
                                        .stacksTo(1)
                                        .rarity(Rarity.EPIC)
                                        .setNoRepair()));

        public static final DeferredItem<Item> HORUS_EYE_ACTIVATED = ITEMS.register("horus_eye_activated",
                        () -> new ActivatedEyeOfHorusItem(new Item.Properties()
                                        .stacksTo(1)
                                        .rarity(Rarity.EPIC)
                                        .fireResistant()));

        public static final DeferredItem<Item> INERTIAL_STABILITY_ALLOY = ITEMS.register("inertial_stability_alloy",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> RAINBOW_CAPTURED = ITEMS.register("rainbow_captured",
                        () -> new Item(new Item.Properties()
                                        .stacksTo(1)
                                        .rarity(Rarity.UNCOMMON)));

        public static final DeferredItem<Item> PHARAOH_SWORD = ITEMS.register("pharaoh_sword",
                        () -> new PharaohSwordItem(new Item.Properties()
                                        .stacksTo(1)
                                        .rarity(Rarity.EPIC)
                                        .fireResistant()));

        // === Phase 2: Entropy Ecosystem Items ===

        public static final DeferredItem<Item> POCKET_DIMENSIONAL_PRISON = ITEMS.register("pocket_dimensional_prison",
                        () -> new PocketDimensionalPrisonItem(new Item.Properties()
                                        .stacksTo(1)
                                        .rarity(Rarity.EPIC)));

        public static final DeferredItem<Item> ENTROPY_BIOMASS = ITEMS.register("entropy_biomass",
                        () -> new Item(new Item.Properties()
                                        .stacksTo(64)));

        public static final DeferredItem<Item> DECAYING_GRAVITON = ITEMS.register("decaying_graviton",
                        () -> new DecayingGravitonItem(new Item.Properties()
                                        .stacksTo(16)
                                        .rarity(Rarity.RARE)));

        public static void register(IEventBus eventBus) {
                ITEMS.register(eventBus);
        }
}
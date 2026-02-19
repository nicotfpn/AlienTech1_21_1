package net.nicotfpn.alientech;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
                        .comment("Whether to log the dirt block on common setup")
                        .define("logDirtBlock", true);

        // Client configs...
        public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
                        .comment("A magic number")
                        .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

        public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
                        .comment("What you want the introduction message to be for the magic number")
                        .define("magicNumberIntroduction", "The magic number is... ");

        public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
                        .comment("A list of items to log on common setup.")
                        .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "",
                                        Config::validateItemName);

        // SERVER CONFIG (Synced to client)
        public static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

        // FIX: Renamed and increased capacity for professional standards
        public static final ModConfigSpec.IntValue ANCIENT_BATTERY_CAPACITY = SERVER_BUILDER
                        .comment("Maximum energy capacity for the Ancient Battery")
                        .defineInRange("ancientBatteryCapacity", 10_000_000, 10_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue CHARGER_CAPACITY = SERVER_BUILDER
                        .comment("Maximum energy capacity for the Ancient Charger")
                        .defineInRange("chargerCapacity", 10_000_000, 10_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PHARAOH_SWORD_COST = SERVER_BUILDER
                        .comment("Energy cost per use of the Pharaoh Sword ability")
                        .defineInRange("pharaohSwordCost", 500, 0, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PHARAOH_SWORD_CAPACITY = SERVER_BUILDER
                        .comment("Maximum energy capacity for the Pharaoh Sword")
                        .defineInRange("pharaohSwordCapacity", 1_000_000, 1_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PYRAMID_CORE_CAPACITY = SERVER_BUILDER
                        .comment("Maximum energy capacity for the Pyramid Core")
                        .defineInRange("pyramidCoreCapacity", 100_000_000, 1_000_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PYRAMID_CORE_GENERATION = SERVER_BUILDER
                        .comment("Energy generation per tick for the Pyramid Core")
                        .defineInRange("pyramidCoreGeneration", 50_000, 100, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PRIMAL_CATALYST_MAX_PROGRESS = SERVER_BUILDER
                        .comment("[LEGACY] Ticks required to craft an Inertial Stability Alloy")
                        .defineInRange("primalCatalystMaxProgress", 72, 1, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PRIMAL_CATALYST_CAPACITY = SERVER_BUILDER
                        .comment("Maximum energy capacity for the Primal Catalyst (FE)")
                        .defineInRange("primalCatalystCapacity", 100_000, 1_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PRIMAL_CATALYST_ENERGY_PER_TICK = SERVER_BUILDER
                        .comment("Energy consumed per tick while the Primal Catalyst is processing (FE/t)")
                        .defineInRange("primalCatalystEnergyPerTick", 40, 1, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue PRIMAL_CATALYST_PROCESS_TIME = SERVER_BUILDER
                        .comment("Ticks required for the Primal Catalyst to complete one recipe")
                        .defineInRange("primalCatalystProcessTime", 200, 1, Integer.MAX_VALUE);

        // === Phase 3: Decay Chamber ===

        public static final ModConfigSpec.IntValue DECAY_CHAMBER_TICKS_PER_HP = SERVER_BUILDER
                        .comment("Ticks of decay processing per point of mob max health")
                        .defineInRange("decayChamberTicksPerHp", 10, 1, 1000);

        public static final ModConfigSpec.DoubleValue DECAY_CHAMBER_BIOMASS_PER_HP = SERVER_BUILDER
                        .comment("Entropy biomass items generated per point of mob max health")
                        .defineInRange("decayChamberBiomassPerHp", 0.5, 0.01, 10.0);

        public static final ModConfigSpec.IntValue DECAY_CHAMBER_ENTROPY_CAPACITY = SERVER_BUILDER
                        .comment("Maximum entropy storage for the Decay Chamber Controller")
                        .defineInRange("decayChamberEntropyCapacity", 10000, 100, 1_000_000);

        // === Phase 4: Entropy Reservoir ===

        public static final ModConfigSpec.IntValue ENTROPY_RESERVOIR_CAPACITY = SERVER_BUILDER
                        .comment("Maximum FE energy capacity for the Entropy Reservoir")
                        .defineInRange("entropyReservoirCapacity", 50_000, 1_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue ENTROPY_RESERVOIR_PROCESS_TIME = SERVER_BUILDER
                        .comment("Ticks required for the Entropy Reservoir to convert biomass to graviton")
                        .defineInRange("entropyReservoirProcessTime", 100, 1, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue ENTROPY_RESERVOIR_ENERGY_PER_TICK = SERVER_BUILDER
                        .comment("FE consumed per tick while the Entropy Reservoir is processing")
                        .defineInRange("entropyReservoirEnergyPerTick", 20, 1, Integer.MAX_VALUE);

        // === Phase 5: Quantum Vacuum Turbine ===

        public static final ModConfigSpec.IntValue QVT_ENERGY_CAPACITY = SERVER_BUILDER
                        .comment("Maximum FE energy capacity for the Quantum Vacuum Turbine")
                        .defineInRange("qvtEnergyCapacity", 500_000, 1_000, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue QVT_FE_PER_TICK = SERVER_BUILDER
                        .comment("FE generated per tick while the Quantum Vacuum Turbine is burning")
                        .defineInRange("qvtFePerTick", 256, 1, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue QVT_BURN_TIME_PER_GRAVITON = SERVER_BUILDER
                        .comment("Ticks of burn time per decaying graviton consumed")
                        .defineInRange("qvtBurnTimePerGraviton", 400, 1, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue QVT_MAX_PUSH_PER_TICK = SERVER_BUILDER
                        .comment("Maximum FE pushed to each adjacent block per tick")
                        .defineInRange("qvtMaxPushPerTick", 10000, 1, Integer.MAX_VALUE);

        // === Phase 6: Pyramid Boost ===

        public static final ModConfigSpec.IntValue PYRAMID_SCAN_INTERVAL = SERVER_BUILDER
                        .comment("Ticks between pyramid structure validation and turbine scanning")
                        .defineInRange("pyramidScanInterval", 200, 20, 10000);

        public static final ModConfigSpec.IntValue PYRAMID_BOOST_RANGE = SERVER_BUILDER
                        .comment("Block radius to search for turbines around a Pyramid Core")
                        .defineInRange("pyramidBoostRange", 32, 1, 128);

        public static final ModConfigSpec.DoubleValue PYRAMID_TIER1_MULTIPLIER = SERVER_BUILDER
                        .comment("Energy generation multiplier for Pyramid Tier 1 (higher tiers scale from this)")
                        .defineInRange("pyramidTier1Multiplier", 4.0, 1.0, 100.0);

        // === Phase 7: Entropy Transport ===

        public static final ModConfigSpec.IntValue ENTROPY_CABLE_TRANSFER_RATE = SERVER_BUILDER
                        .comment("Maximum entropy transferred per tick per cable connection")
                        .defineInRange("entropyCableTransferRate", 500, 1, 100_000);

        // === Phase 8: Player Evolution Abilities ===

        // Entropy Shield Ability
        public static final ModConfigSpec.IntValue ABILITY_ENTROPY_SHIELD_COST = SERVER_BUILDER
                        .comment("Entropy cost to activate Entropy Shield ability")
                        .defineInRange("abilityEntropyShieldCost", 200, 1, 10_000);
        
        public static final ModConfigSpec.IntValue ABILITY_ENTROPY_SHIELD_COOLDOWN = SERVER_BUILDER
                        .comment("Cooldown in ticks for Entropy Shield ability")
                        .defineInRange("abilityEntropyShieldCooldown", 400, 0, 10_000);
        
        public static final ModConfigSpec.IntValue ABILITY_ENTROPY_SHIELD_DURATION = SERVER_BUILDER
                        .comment("Duration in ticks for Entropy Shield effect")
                        .defineInRange("abilityEntropyShieldDuration", 200, 1, 10_000);

        // Decay Vision Ability
        public static final ModConfigSpec.IntValue ABILITY_DECAY_VISION_COST = SERVER_BUILDER
                        .comment("Entropy cost to activate Decay Vision ability")
                        .defineInRange("abilityDecayVisionCost", 150, 1, 10_000);
        
        public static final ModConfigSpec.IntValue ABILITY_DECAY_VISION_COOLDOWN = SERVER_BUILDER
                        .comment("Cooldown in ticks for Decay Vision ability")
                        .defineInRange("abilityDecayVisionCooldown", 300, 0, 10_000);
        
        public static final ModConfigSpec.IntValue ABILITY_DECAY_VISION_DURATION = SERVER_BUILDER
                        .comment("Duration in ticks for Decay Vision effect")
                        .defineInRange("abilityDecayVisionDuration", 600, 1, 10_000);
        
        public static final ModConfigSpec.DoubleValue ABILITY_DECAY_VISION_RANGE = SERVER_BUILDER
                        .comment("Range in blocks for Decay Vision entity detection")
                        .defineInRange("abilityDecayVisionRange", 32.0, 1.0, 128.0);

        // Gravitational Pull Ability
        public static final ModConfigSpec.IntValue ABILITY_GRAVITATIONAL_PULL_COST = SERVER_BUILDER
                        .comment("Entropy cost to activate Gravitational Pull ability")
                        .defineInRange("abilityGravitationalPullCost", 300, 1, 10_000);
        
        public static final ModConfigSpec.IntValue ABILITY_GRAVITATIONAL_PULL_COOLDOWN = SERVER_BUILDER
                        .comment("Cooldown in ticks for Gravitational Pull ability")
                        .defineInRange("abilityGravitationalPullCooldown", 200, 0, 10_000);
        
        public static final ModConfigSpec.DoubleValue ABILITY_GRAVITATIONAL_PULL_RANGE = SERVER_BUILDER
                        .comment("Range in blocks for Gravitational Pull")
                        .defineInRange("abilityGravitationalPullRange", 16.0, 1.0, 64.0);
        
        public static final ModConfigSpec.DoubleValue ABILITY_GRAVITATIONAL_PULL_STRENGTH = SERVER_BUILDER
                        .comment("Pull strength multiplier for Gravitational Pull")
                        .defineInRange("abilityGravitationalPullStrength", 0.5, 0.1, 5.0);

        // Entropy Burst Ability
        public static final ModConfigSpec.IntValue ABILITY_ENTROPY_BURST_COST = SERVER_BUILDER
                        .comment("Entropy cost to activate Entropy Burst ability")
                        .defineInRange("abilityEntropyBurstCost", 500, 1, 10_000);
        
        public static final ModConfigSpec.IntValue ABILITY_ENTROPY_BURST_COOLDOWN = SERVER_BUILDER
                        .comment("Cooldown in ticks for Entropy Burst ability")
                        .defineInRange("abilityEntropyBurstCooldown", 600, 0, 10_000);
        
        public static final ModConfigSpec.DoubleValue ABILITY_ENTROPY_BURST_POWER = SERVER_BUILDER
                        .comment("Explosion power for Entropy Burst")
                        .defineInRange("abilityEntropyBurstPower", 2.0, 0.5, 10.0);
        
        public static final ModConfigSpec.DoubleValue ABILITY_ENTROPY_BURST_RANGE = SERVER_BUILDER
                        .comment("Range in blocks for Entropy Burst damage")
                        .defineInRange("abilityEntropyBurstRange", 8.0, 1.0, 32.0);
        
        public static final ModConfigSpec.DoubleValue ABILITY_ENTROPY_BURST_DAMAGE = SERVER_BUILDER
                        .comment("Damage dealt by Entropy Burst")
                        .defineInRange("abilityEntropyBurstDamage", 8.0, 1.0, 50.0);

        public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

        static final ModConfigSpec SPEC = BUILDER.build();

        private static boolean validateItemName(final Object obj) {
                return obj instanceof String itemName
                                && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
        }
}

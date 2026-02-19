package net.nicotfpn.alientech;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.event.CommonModEvents;
import net.nicotfpn.alientech.item.ModCreativeModeTabs;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.screen.ModMenuTypes;
import org.slf4j.Logger;

/**
 * Main mod class for AlienTech - Ancient Egyptian Tech Mod
 * Registers all mod components and handles lifecycle events.
 */
@Mod(AlienTech.MOD_ID)
public class AlienTech {

    public static final String MOD_ID = "alientech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AlienTech(IEventBus modBus, ModContainer modContainer) {
        // Register all mod components
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, Config.SERVER_SPEC);

        ModItems.register(modBus);

        ModBlocks.register(modBus);
        net.nicotfpn.alientech.registration.AlienBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeModeTabs.register(modBus);
        net.nicotfpn.alientech.recipe.ModRecipes.register(modBus);
        net.nicotfpn.alientech.evolution.ModAttachments.register(modBus);

        // Register event listeners
        modBus.addListener(this::onCommonSetup);

        // Register capability events (energy, etc) via CommonModEvents
        modBus.register(CommonModEvents.class);

        // Register for server events
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("AlienTech mod initialized!");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Register evolution abilities
        net.nicotfpn.alientech.evolution.ability.ModAbilities.register();
        
        LOGGER.info("AlienTech common setup complete!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("AlienTech loaded on server!");
    }
}
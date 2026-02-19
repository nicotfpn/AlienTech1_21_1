package net.nicotfpn.alientech.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.evolution.PlayerEvolutionHelper;
import net.nicotfpn.alientech.evolution.ability.AbilityRegistry;
import net.nicotfpn.alientech.evolution.ability.IEvolutionAbility;
import net.nicotfpn.alientech.network.AbilityActivationPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles client-side keybinds for evolution abilities.
 * <p>
 * Registers keybinds and sends activation packets to the server when keys are pressed.
 */
@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityKeyHandler {

    private static final List<KeyMapping> KEYBINDS = new ArrayList<>();
    
    // Ability keybinds - one per ability slot
    public static final KeyMapping ABILITY_1 = createKeybind("ability_1", GLFW.GLFW_KEY_V, "key.categories.alientech");
    public static final KeyMapping ABILITY_2 = createKeybind("ability_2", GLFW.GLFW_KEY_B, "key.categories.alientech");
    public static final KeyMapping ABILITY_3 = createKeybind("ability_3", GLFW.GLFW_KEY_N, "key.categories.alientech");
    public static final KeyMapping ABILITY_4 = createKeybind("ability_4", GLFW.GLFW_KEY_M, "key.categories.alientech");

    private static KeyMapping createKeybind(String name, int keyCode, String category) {
        KeyMapping keybind = new KeyMapping(
                "key.alientech." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
                category
        );
        KEYBINDS.add(keybind);
        return keybind;
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KEYBINDS.forEach(event::register);
    }

    // Register for client tick events to check key presses
    @EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientTickHandler {
        
        private static int ability1Cooldown = 0;
        private static int ability2Cooldown = 0;
        private static int ability3Cooldown = 0;
        private static int ability4Cooldown = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }

            // Decrement cooldowns
            if (ability1Cooldown > 0) ability1Cooldown--;
            if (ability2Cooldown > 0) ability2Cooldown--;
            if (ability3Cooldown > 0) ability3Cooldown--;
            if (ability4Cooldown > 0) ability4Cooldown--;

            // Check for key presses
            if (ABILITY_1.consumeClick() && ability1Cooldown == 0) {
                activateAbility(0);
                ability1Cooldown = 5; // Small client-side cooldown to prevent spam
            }
            if (ABILITY_2.consumeClick() && ability2Cooldown == 0) {
                activateAbility(1);
                ability2Cooldown = 5;
            }
            if (ABILITY_3.consumeClick() && ability3Cooldown == 0) {
                activateAbility(2);
                ability3Cooldown = 5;
            }
            if (ABILITY_4.consumeClick() && ability4Cooldown == 0) {
                activateAbility(3);
                ability4Cooldown = 5;
            }
        }

        private static void activateAbility(int slot) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            // Get available abilities for the player
            int evolutionStage = PlayerEvolutionHelper.get(mc.player).getEvolutionStage();
            List<IEvolutionAbility> available = AbilityRegistry.getAvailableAbilities(evolutionStage);

            if (slot < available.size()) {
                IEvolutionAbility ability = available.get(slot);
                ResourceLocation abilityId = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, ability.getId());
                
                // Send packet to server
                AbilityActivationPacket packet = new AbilityActivationPacket(abilityId);
                PacketDistributor.sendToServer(packet);
            }
        }
    }
}

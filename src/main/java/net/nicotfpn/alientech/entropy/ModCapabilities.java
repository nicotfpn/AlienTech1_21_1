package net.nicotfpn.alientech.entropy;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.nicotfpn.alientech.AlienTech;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the Entropy capability using the NeoForge 1.21.1 capability system.
 * <p>
 * Usage: reference {@link #ENTROPY} when registering block entity providers
 * via
 * {@code RegisterCapabilitiesEvent.registerBlockEntity(ModCapabilities.ENTROPY, ...)}.
 * <p>
 * This is a sided capability — providers receive a {@link Direction} parameter
 * indicating which face is being queried (null = internal/unknown).
 */
public final class ModCapabilities {

    private ModCapabilities() {
        // Static holder class
    }

    /**
     * The Entropy block capability.
     * Sided: providers receive the queried Direction (nullable).
     */
    public static final BlockCapability<IEntropyHandler, @Nullable Direction> ENTROPY = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "entropy"),
            IEntropyHandler.class);
}

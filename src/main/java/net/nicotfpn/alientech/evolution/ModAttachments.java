package net.nicotfpn.alientech.evolution;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.nicotfpn.alientech.AlienTech;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Registers the player evolution data attachment using NeoForge 1.21.1
 * AttachmentType system.
 * <p>
 * Data persists across saves and death (copyOnDeath).
 */
public final class ModAttachments {

    private ModAttachments() {
        // Static holder class
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, AlienTech.MOD_ID);

    /**
     * Player evolution data attachment — persists across saves and death.
     */
    public static final Supplier<AttachmentType<PlayerEvolutionData>> PLAYER_EVOLUTION = ATTACHMENT_TYPES.register(
            "player_evolution",
            () -> AttachmentType.builder(PlayerEvolutionData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PlayerEvolutionData>() {
                        @Override
                        public PlayerEvolutionData read(IAttachmentHolder holder, CompoundTag tag,
                                HolderLookup.Provider provider) {
                            PlayerEvolutionData data = new PlayerEvolutionData();
                            data.deserializeNBT(tag);
                            return data;
                        }

                        @Override
                        public CompoundTag write(PlayerEvolutionData data, HolderLookup.Provider provider) {
                            return data.serializeNBT();
                        }
                    }).copyOnDeath()
                    .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}

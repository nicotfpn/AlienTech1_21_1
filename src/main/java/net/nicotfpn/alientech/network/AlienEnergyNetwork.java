package net.nicotfpn.alientech.network;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Metaphysical player-bound energy network tracking Entropy on a scalable long
 * tier.
 */
public class AlienEnergyNetwork {
    private static final long MAX_ENTROPY = 9_000_000_000_000_000_000L;
    private static final long INSTABILITY_THRESHOLD = 8_000_000_000_000_000_000L;

    private final UUID ownerId;
    private long entropyStored;
    private long instabilityLevel;
    private long lastActiveTick;

    public AlienEnergyNetwork(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public long getEntropyStored() {
        return entropyStored;
    }

    public long getInstabilityLevel() {
        return instabilityLevel;
    }

    public long getLastActiveTick() {
        return lastActiveTick;
    }

    public void insertEntropy(long amount, long currentTick) {
        this.lastActiveTick = currentTick;
        this.entropyStored = Math.min(this.entropyStored + amount, MAX_ENTROPY);

        long overflow = this.entropyStored - INSTABILITY_THRESHOLD;
        if (overflow > 0) {
            // Regulated scale growth mapping to produce systemic risk when overloaded
            this.instabilityLevel += overflow / 1_000_000L;
        }
    }

    public boolean extractEntropy(long amount, long currentTick) {
        this.lastActiveTick = currentTick;
        if (this.entropyStored >= amount) {
            this.entropyStored -= amount;
            return true;
        }
        return false;
    }

    public void processInstabilitySlowdown() {
        if (instabilityLevel > 0) {
            // Decay over time or penalize player machinery
            instabilityLevel--;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", ownerId);
        tag.putLong("Entropy", entropyStored);
        tag.putLong("Instability", instabilityLevel);
        tag.putLong("LastActiveTick", lastActiveTick);
        return tag;
    }

    public static AlienEnergyNetwork load(CompoundTag tag) {
        UUID ownerId = tag.getUUID("Owner");
        AlienEnergyNetwork network = new AlienEnergyNetwork(ownerId);
        network.entropyStored = tag.getLong("Entropy");
        network.instabilityLevel = tag.getLong("Instability");
        network.lastActiveTick = tag.getLong("LastActiveTick");
        return network;
    }
}

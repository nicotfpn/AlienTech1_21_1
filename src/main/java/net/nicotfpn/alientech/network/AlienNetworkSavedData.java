package net.nicotfpn.alientech.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists metaphysical AlienEnergyNetworks spanning globally for all players.
 */
public class AlienNetworkSavedData extends SavedData {

    // Threshold: networks inactive for more than ~30 in-game days (~720,000 ticks)
    // are removed
    private static final long GC_TICK_THRESHOLD = 720_000L;

    private final Map<UUID, AlienEnergyNetwork> networks = new HashMap<>();

    public AlienEnergyNetwork getNetwork(UUID ownerId) {
        if (!networks.containsKey(ownerId)) {
            networks.put(ownerId, new AlienEnergyNetwork(ownerId));
            setDirty();
        }
        return networks.get(ownerId);
    }

    public void tickNetworkGarbageCollection(long currentTick) {
        List<UUID> toRemove = new ArrayList<>();
        for (AlienEnergyNetwork network : networks.values()) {
            if (currentTick - network.getLastActiveTick() > GC_TICK_THRESHOLD) {
                toRemove.add(network.getOwnerId());
            }
        }
        if (!toRemove.isEmpty()) {
            for (UUID id : toRemove) {
                networks.remove(id);
            }
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (AlienEnergyNetwork network : networks.values()) {
            list.add(network.save());
        }
        tag.put("Networks", list);
        return tag;
    }

    public static AlienNetworkSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AlienNetworkSavedData data = new AlienNetworkSavedData();
        if (tag.contains("Networks", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Networks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag networkTag = list.getCompound(i);
                AlienEnergyNetwork network = AlienEnergyNetwork.load(networkTag);
                data.networks.put(network.getOwnerId(), network);
            }
        }
        return data; // Dirty flagged on getNetwork anyways.
    }

    public static AlienNetworkSavedData get(ServerLevel level) {
        // Typically attached to Overworld for true global existence
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        AlienNetworkSavedData::new,
                        AlienNetworkSavedData::load),
                "alientech_networks");
    }
}

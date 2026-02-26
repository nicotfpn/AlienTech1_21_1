package net.nicotfpn.alientech.multiblock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the global state of all multiblocks linked to a specific dimension.
 * Ensures formations persist safely across server restarts.
 */
public class AlienMultiblockSavedData extends SavedData {

    private final Map<UUID, AlienMultiblockInstance> activeMultiblocks = new HashMap<>();

    public void register(AlienMultiblockInstance instance) {
        activeMultiblocks.put(instance.getStructureId(), instance);
        setDirty();
    }

    public void remove(UUID structureId) {
        if (activeMultiblocks.remove(structureId) != null) {
            setDirty();
        }
    }

    public AlienMultiblockInstance get(UUID structureId) {
        return activeMultiblocks.get(structureId);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (AlienMultiblockInstance inst : activeMultiblocks.values()) {
            list.add(inst.save(provider));
        }
        tag.put("Instances", list);
        return tag;
    }

    public static AlienMultiblockSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AlienMultiblockSavedData data = new AlienMultiblockSavedData();
        if (tag.contains("Instances", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Instances", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag instanceTag = list.getCompound(i);
                AlienMultiblockInstance instance = AlienMultiblockInstance.load(instanceTag, provider);
                data.register(instance);
            }
        }
        return data; // Registration automatically sets it dirty but whatever.
    }

    public static AlienMultiblockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        AlienMultiblockSavedData::new,
                        AlienMultiblockSavedData::load),
                "alientech_multiblocks");
    }
}

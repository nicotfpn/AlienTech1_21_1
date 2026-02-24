package net.nicotfpn.alientech.pyramid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.nicotfpn.alientech.util.AlienSavedData;

/**
 * SavedData wrapper delegating to {@link PyramidNetwork} save/load helpers.
 */
public class PyramidSavedData extends AlienSavedData {

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        try {
            MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
            if (srv != null) {
                ServerLevel overworld = srv.overworld();
                if (overworld != null) {
                    PyramidNetwork.get(overworld).loadFromTag(nbt);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        try {
            MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
            if (srv != null) {
                ServerLevel overworld = srv.overworld();
                if (overworld != null) {
                    PyramidNetwork.get(overworld).saveToTag(tag);
                }
            }
        } catch (Exception ignored) {
        }
        return tag;
    }

    public static PyramidSavedData loadOrCreate() {
        return AlienSavedData.createSavedData(PyramidSavedData::new, "pyramid_network");
    }
}

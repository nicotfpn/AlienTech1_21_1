package net.nicotfpn.alientech.util;

import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

/**
 * Small helper to create SavedData instances similar to Mekanism's utility.
 */
public abstract class AlienSavedData extends SavedData {

    public abstract void load(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider);

    /**
     * Note: This should only be called from the server side
     */
    public static <DATA extends AlienSavedData> DATA createSavedData(Supplier<DATA> createFunction, String name) {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        if (currentServer == null) {
            throw new IllegalStateException("Current server is null");
        }
        DimensionDataStorage dataStorage = currentServer.overworld().getDataStorage();
        return createSavedData(dataStorage, new SavedData.Factory<>(createFunction, (tag, provider) -> {
            DATA handler = createFunction.get();
            handler.load(tag, provider);
            return handler;
        }), name);
    }

    public static <DATA extends AlienSavedData> DATA createSavedData(DimensionDataStorage dataStorage, SavedData.Factory<DATA> factory, String name) {
        return dataStorage.computeIfAbsent(factory, "alientech_" + name);
    }
}

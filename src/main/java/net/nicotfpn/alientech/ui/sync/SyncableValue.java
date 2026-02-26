package net.nicotfpn.alientech.ui.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Interface determining how discrete data can be compacted and synced over the
 * network,
 * avoiding the standard 32-bit DataSlot limitations.
 */
public interface SyncableValue<T> {

    T get();

    void set(T value);

    boolean isDirty();

    void markClean();

    void encode(RegistryFriendlyByteBuf buffer);

    void decode(RegistryFriendlyByteBuf buffer);
}

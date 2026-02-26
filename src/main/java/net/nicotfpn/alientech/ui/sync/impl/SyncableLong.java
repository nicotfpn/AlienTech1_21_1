package net.nicotfpn.alientech.ui.sync.impl;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.nicotfpn.alientech.ui.sync.SyncableValue;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * A variable-byte long syncer capable of compressing payloads dynamically.
 * Instead of holding the value, it takes a supplier and consumer to pull from
 * BlockEntities directly.
 */
public class SyncableLong implements SyncableValue<Long> {
    private final LongSupplier getter;
    private final LongConsumer setter;
    private long lastSyncedValue;

    public static SyncableLong create(LongSupplier getter, LongConsumer setter) {
        return new SyncableLong(getter, setter);
    }

    private SyncableLong(LongSupplier getter, LongConsumer setter) {
        this.getter = getter;
        this.setter = setter;
        // Seed initial sync tracking to the tile's current payload directly on
        // construction
        this.lastSyncedValue = getter.getAsLong();
    }

    @Override
    public Long get() {
        return getter.getAsLong();
    }

    @Override
    public void set(Long val) {
        setter.accept(val);
    }

    @Override
    public boolean isDirty() {
        return get() != lastSyncedValue;
    }

    @Override
    public void markClean() {
        lastSyncedValue = get();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarLong(get());
    }

    @Override
    public void decode(RegistryFriendlyByteBuf buffer) {
        set(buffer.readVarLong());
    }
}

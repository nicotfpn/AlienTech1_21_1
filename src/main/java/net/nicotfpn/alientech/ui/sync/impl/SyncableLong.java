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

    // Tracks the last value sent from the server. If the value is equal to this,
    // the sync system assumes nothing changed.
    // NOTE: We intentionally start as "dirty" so the client receives an initial
    // state packet when the GUI is first opened.
    private long lastSyncedValue;
    private boolean needsInitialSync = true;

    private boolean isClientSide = false;
    private long clientValue;

    public static SyncableLong create(LongSupplier getter, LongConsumer setter) {
        return new SyncableLong(getter, setter);
    }

    public SyncableLong(LongSupplier getter, LongConsumer setter) {
        this.getter = getter;
        this.setter = setter;
        this.lastSyncedValue = getter.getAsLong();
    }

    @Override
    public Long get() {
        return isClientSide ? clientValue : getter.getAsLong();
    }

    @Override
    public void set(Long val) {
        this.isClientSide = true;
        this.clientValue = val;
        if (setter != null) {
            setter.accept(val);
        }
    }

    @Override
    public boolean isDirty() {
        if (isClientSide) {
            // Client-side trackables are not synchronized from the client; only the
            // server pushes updates.
            return false;
        }
        if (needsInitialSync) {
            return true;
        }
        return getter.getAsLong() != lastSyncedValue;
    }

    @Override
    public void markClean() {
        needsInitialSync = false;
        lastSyncedValue = getter.getAsLong();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarLong(getter.getAsLong());
    }

    @Override
    public void decode(RegistryFriendlyByteBuf buffer) {
        set(buffer.readVarLong());
    }
}

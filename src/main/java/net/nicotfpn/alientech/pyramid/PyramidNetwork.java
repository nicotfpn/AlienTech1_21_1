package net.nicotfpn.alientech.pyramid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.nicotfpn.alientech.entropy.EntropyStorage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * In-memory Pyramid network manager.
 * <p>
 * Responsibilities:
 * - Keep a set of registered Pyramid Core positions (cores register/unregister
 * in their onLoad()/setRemoved()).
 * - Maintain a shared entropy buffer that machines can pull from using
 * {@link #extractEntropy(long, boolean)}.
 * - Compute current tier dynamically by querying
 * {@link PyramidStructureValidator} for
 * each registered core (highest tier wins).
 *
 * Note: persistence to world save is intentionally left out for now (the
 * network provides API hooks `saveToTag`/`loadFromTag` for future integration
 * with SavedData). This class avoids chunk scanning entirely.
 */
public final class PyramidNetwork {

    private static final WeakHashMap<Level, PyramidNetwork> INSTANCES = new WeakHashMap<>();

    private final Level level;
    private final Set<BlockPos> cores = new HashSet<>();

    // Shared network buffer for entropy (machines pull into their local buffers)
    private final EntropyStorage networkBuffer;

    private PyramidNetwork(Level level) {
        this.level = Objects.requireNonNull(level);
        // Default capacity -- reasonable default; can be tuned via Config later
        this.networkBuffer = new EntropyStorage(100000, this::onNetworkChanged);
    }

    private void onNetworkChanged() {
        // For now, light debug; callers must handle sync/persistence as needed.
        // network buffer changed
        // Mark saved data dirty so it will be written to disk on next save
        try {
            PyramidSavedData sd = PyramidSavedData.loadOrCreate();
            if (sd != null)
                sd.setDirty();
        } catch (Exception ignored) {
        }
    }

    /**
     * Obtain the PyramidNetwork instance for a given level.
     * Creates a single in-memory instance per-level.
     */
    public static PyramidNetwork get(Level level) {
        synchronized (INSTANCES) {
            PyramidNetwork net = INSTANCES.get(level);
            if (net == null) {
                net = new PyramidNetwork(level);
                INSTANCES.put(level, net);

                // If running on server, apply the SavedData content to this network.
                // This MUST be called after placing it in INSTANCES to break the recursive load
                // loop.
                try {
                    PyramidSavedData.loadOrCreate();
                } catch (Exception ignored) {
                }
            }
            return net;
        }
    }

    // ========== Core registration (no chunk scanning) ==========

    public void registerCore(BlockPos pos) {
        if (pos == null)
            return;
        synchronized (cores) {
            cores.add(pos.immutable());
        }
    }

    public void unregisterCore(BlockPos pos) {
        if (pos == null)
            return;
        synchronized (cores) {
            cores.remove(pos);
        }
    }

    /**
     * Returns an unmodifiable snapshot of registered cores.
     */
    public Set<BlockPos> getRegisteredCores() {
        synchronized (cores) {
            return Collections.unmodifiableSet(new HashSet<>(cores));
        }
    }

    // ========== Tier calculation (dynamic, highest wins) ==========

    /**
     * Computes the current PyramidTier for the level by querying every registered
     * core and returning the highest validated tier. If no cores are registered
     * or validation fails, returns {@link PyramidTier#NONE}.
     */
    public PyramidTier getTier() {
        PyramidTier highest = PyramidTier.NONE;
        // Copy to avoid holding lock during validation
        Set<BlockPos> snapshot;
        synchronized (cores) {
            snapshot = new HashSet<>(cores);
        }

        for (BlockPos pos : snapshot) {
            try {
                PyramidTier t = PyramidStructureValidator.validate(level, pos);
                if (t.ordinal() > highest.ordinal()) {
                    highest = t;
                    if (highest == PyramidTier.TIER_3) {
                        // early exit - can't get higher
                        break;
                    }
                }
            } catch (Exception e) {
                // validation failed for core: log omitted in clean build
            }
        }
        return highest;
    }

    // ========== Network entropy buffer API ==========

    /**
     * Insert entropy into the network buffer. Returns amount actually inserted.
     */
    public long insertEntropy(long amount, boolean simulate) {
        if (amount <= 0L)
            return 0L;
        return networkBuffer.insertEntropy(amount, simulate);
    }

    /**
     * Extract entropy from the network buffer. Returns amount actually
     * extracted.
     * <p>
     * Machines must call this and then insert the returned amount into their
     * local buffer; machines must never consume directly from the network.
     */
    public long extractEntropy(long amount, boolean simulate) {
        if (amount <= 0L)
            return 0L;
        return networkBuffer.extractEntropy(amount, simulate);
    }

    /**
     * Query available entropy in the network buffer.
     */
    public long getEntropyAvailable() {
        return networkBuffer.getEntropy();
    }

    /**
     * Return network buffer capacity.
     */
    public long getNetworkCapacity() {
        return networkBuffer.getMaxEntropy();
    }

    // ========== Persistence helpers (for future SavedData integration) ==========

    /**
     * Populate a tag with the network state. Caller is responsible for writing
     * this tag to world saved data.
     */
    public void saveToTag(CompoundTag tag) {
        networkBuffer.save(tag);
        // Save cores as long array (BlockPos -> long)
        long[] longs;
        synchronized (cores) {
            longs = cores.stream().mapToLong(BlockPos::asLong).toArray();
        }
        tag.putLongArray("Cores", longs);
    }

    /**
     * Load network state from a tag (if present). Replaces the current cores
     * set and buffer value.
     */
    public void loadFromTag(CompoundTag tag) {
        if (tag == null)
            return;
        networkBuffer.load(tag);
        if (tag.contains("Cores")) {
            long[] longs = tag.getLongArray("Cores");
            synchronized (cores) {
                cores.clear();
                for (long l : longs) {
                    cores.add(BlockPos.of(l));
                }
            }
        }
    }
}

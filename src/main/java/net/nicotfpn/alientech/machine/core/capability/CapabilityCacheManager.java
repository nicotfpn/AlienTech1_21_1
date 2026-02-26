package net.nicotfpn.alientech.machine.core.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages capability caches to optimize lookups per tick without iteration
 * overhead.
 */
public class CapabilityCacheManager<T, C> {

    private final BlockCapability<T, C> capability;
    private final ServerLevel level;
    private final BlockPos pos;
    private final Map<Direction, BlockCapabilityCache<T, C>> caches = new EnumMap<>(Direction.class);

    public CapabilityCacheManager(BlockCapability<T, C> capability, ServerLevel level, BlockPos pos) {
        this.capability = capability;
        this.level = level;
        this.pos = pos;
    }

    /**
     * Resolves the capability at the specified direction, utilizing memory caches
     * and lazy rebuilding.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getCapability(Direction dir) {
        if (dir == null)
            return null; // Unhandled internal directions for now

        BlockCapabilityCache<T, C> cache = caches.computeIfAbsent(dir,
                d -> BlockCapabilityCache.create(capability, level, pos.relative(d), (C) d.getOpposite()));
        return cache.getCapability();
    }

    /**
     * Called when the tile entity is invalidated, dropping references to cleanly GC
     * the caches.
     */
    public void invalidateAll() {
        caches.clear();
    }
}

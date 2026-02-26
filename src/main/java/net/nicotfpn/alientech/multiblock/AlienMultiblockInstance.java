package net.nicotfpn.alientech.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Transient representation of a formed Multiblock, calculating its block
 * footprints
 * dynamically to save disk space and validating lazily via events.
 */
public class AlienMultiblockInstance {

    private final UUID structureId;
    private final UUID ownerId;
    private final BlockPos corePos;
    private final int tier;
    private final Direction facing;

    private boolean isFormed;

    public AlienMultiblockInstance(UUID structureId, UUID ownerId, BlockPos corePos, int tier, Direction facing) {
        this.structureId = structureId;
        this.ownerId = ownerId;
        this.corePos = corePos;
        this.tier = tier;
        this.facing = facing;
        this.isFormed = true; // Assumes validated upon instantiation
    }

    public UUID getStructureId() {
        return structureId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    public int getTier() {
        return tier;
    }

    public Direction getFacing() {
        return facing;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public void markBroken() {
        this.isFormed = false;
    }

    /**
     * Hook triggered exclusively when a Neighbor blocks updates (BlockEvent or
     * Native).
     * Re-validates the structure without constantly scanning empty chunks.
     */
    public void triggerNeighborUpdate(BlockPos updatedPos) {
        // Validation algorithm goes here. E.g.: buildFootprint().contains(updatedPos)
        // If broken, markBroken().
    }

    /**
     * Dynamically renders the expected set of block positions for tracking
     * boundaries,
     * avoiding saving gigantic Set<BlockPos> in NBT.
     */
    public Set<BlockPos> rebuildFootprint() {
        Set<BlockPos> parts = new HashSet<>();
        // Example: 3x3x3 around core for Tier 1
        int radius = tier + 1;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    parts.add(corePos.offset(x, y, z));
                }
            }
        }
        return parts;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("StructureId", this.structureId);
        tag.putUUID("OwnerId", this.ownerId);
        tag.putLong("CorePos", this.corePos.asLong());
        tag.putInt("Tier", this.tier);
        tag.putString("Facing", this.facing.getName());
        return tag;
    }

    public static AlienMultiblockInstance load(CompoundTag tag, HolderLookup.Provider provider) {
        UUID structureId = tag.getUUID("StructureId");
        UUID ownerId = tag.getUUID("OwnerId");
        BlockPos corePos = BlockPos.of(tag.getLong("CorePos"));
        int tier = tag.getInt("Tier");
        Direction facing = Direction.byName(tag.getString("Facing"));

        // When loaded from memory, it might be suspended/unloaded until the chunk loads
        return new AlienMultiblockInstance(structureId, ownerId, corePos, tier, facing);
    }
}

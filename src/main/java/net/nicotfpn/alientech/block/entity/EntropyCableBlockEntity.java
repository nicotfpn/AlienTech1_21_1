package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.entropy.EntropyTransaction;
import net.nicotfpn.alientech.entropy.IEntropyHandler;
import net.nicotfpn.alientech.util.AlienTechDebug;
import net.nicotfpn.alientech.util.CapabilityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entropy Cable Block Entity — transports entropy between adjacent blocks.
 * <p>
 * Does NOT store entropy internally. Acts purely as a conduit.
 * <p>
 * Transfer algorithm (per tick):
 * <ol>
 * <li>For each direction, check if neighbor has IEntropyHandler capability</li>
 * <li>Identify sources (canExtract) and destinations (canInsert)</li>
 * <li>Pull from sources and push to destinations, respecting transfer rate</li>
 * <li>Balance entropy flow between connected handlers</li>
 * </ol>
 * <p>
 * Capability-only interaction — no hardcoded machine references.
 */
public class EntropyCableBlockEntity extends BlockEntity {

    public EntropyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_CABLE_BE.get(), pos, state);
    }

    // ==================== Tick Logic ====================

    /**
     * Called every server tick by the block's ticker.
     * Transfers entropy between adjacent IEntropyHandler blocks using
     * transaction-safe logic.
     * <p>
     * Deterministic behavior: processes directions in enum order, prevents
     * double-transfer.
     * Throttled to run every TICK_INTERVAL ticks for performance. Transfer rate is
     * scaled proportionally to maintain the same throughput per second.
     */
    private static final int TICK_INTERVAL = 5;
    private int tickCount = 0;

    public void serverTick() {
        tickCount++;

        if (!CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        // Throttle: only process every TICK_INTERVAL ticks
        if (tickCount % TICK_INTERVAL != 0) {
            return;
        }

        // Scale transfer rate proportionally to maintain throughput
        int baseRate = Config.ENTROPY_CABLE_TRANSFER_RATE.get();
        int transferRate = baseRate * TICK_INTERVAL;
        if (transferRate <= 0) {
            return; // Invalid config
        }

        // Process each source-destination pair deterministically
        // Order: iterate directions in enum order (deterministic)
        for (Direction sourceDir : Direction.values()) {
            IEntropyHandler source = safeGetNeighborHandler(sourceDir);
            if (source == null || !source.canExtract()) {
                continue;
            }

            // Try to push to each destination
            for (Direction destDir : Direction.values()) {
                if (destDir == sourceDir) {
                    continue; // Skip same direction
                }

                IEntropyHandler dest = safeGetNeighborHandler(destDir);
                if (dest == null || !dest.canInsert()) {
                    continue;
                }

                // Use transaction-safe transfer
                EntropyTransaction transaction = EntropyTransaction.transfer(source, dest, transferRate);
                if (transaction.isCommitted()) {
                    long amount = transaction.getAmount();
                    AlienTechDebug.ENTROPY.log("Cable transferred {} entropy from {} to {}",
                            amount, sourceDir, destDir);
                    // Transfer succeeded - continue to next destination
                    // Note: source may now be empty, but we continue checking other destinations
                    // This allows one source to feed multiple destinations in one tick
                }
                // If transaction failed, continue to next destination
            }
        }
    }

    /**
     * Validate cable state (stateless, so this is a no-op but kept for
     * consistency).
     */
    public void validateState() {
        // Entropy cables are stateless - nothing to validate
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clean up any cached references (none currently, but good practice)
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Validate state on load
        validateState();
    }

    /**
     * Safely get the IEntropyHandler capability from a neighbor in the given
     * direction.
     *
     * @param dir direction to check (must not be null)
     * @return the handler, or null if not present or invalid
     */
    @Nullable
    private IEntropyHandler safeGetNeighborHandler(Direction dir) {
        if (dir == null) {
            return null;
        }
        return CapabilityUtils.safeGetNeighborEntropyHandler(level, worldPosition, dir);
    }

    // ==================== Persistence (no-op, stateless) ====================

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}

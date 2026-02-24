package net.nicotfpn.alientech.machine.evolution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.block.entity.base.AlienBlockEntity;
import net.nicotfpn.alientech.entropy.EntropyStorage;
import net.nicotfpn.alientech.entropy.IEntropyHandler;
import net.nicotfpn.alientech.evolution.PlayerEvolutionData;
import net.nicotfpn.alientech.util.CapabilityUtils;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.StateValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evolution Chamber Block Entity — evolves players to higher evolution stages.
 * <p>
 * Detects players standing directly on top of the block and consumes entropy
 * to increase their evolution stage over time.
 * <p>
 * Architecture:
 * - Extends AlienBlockEntity (tick hooks, sync, MenuProvider)
 * - Uses entropy capability for input
 * - Server-authoritative only
 * - Transaction-safe entropy consumption
 */
public class EvolutionChamberBlockEntity extends AlienBlockEntity {

    // ==================== NBT Keys ====================
    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_TARGET_STAGE = "TargetStage";
    private static final String KEY_IS_PROCESSING = "IsProcessing";

    // ==================== State ====================
    private int progress = 0;
    private int targetStage = 0;
    private boolean isProcessing = false;

    // Internal entropy buffer — cables push entropy here via capability
    private final EntropyStorage entropyStorage;

    // Scan throttle — player detection only every N ticks
    private static final int PLAYER_SCAN_INTERVAL = 10;

    // Cached player reference (cleared on removal)
    @Nullable
    private ServerPlayer cachedPlayer = null;

    // ==================== Constructor ====================

    public EvolutionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EVOLUTION_CHAMBER_BE.get(), pos, state);
        this.entropyStorage = new EntropyStorage(
                Config.EVOLUTION_CHAMBER_ENTROPY_CAPACITY.get(),
                1000, // maxInsert (transfer rate in)
                0, // maxExtract (chamber consumes internally, not externally)
                true, // canInsert — cables push entropy in
                false, // canExtract — nothing should pull entropy out
                this::setChanged);
    }

    // ==================== Entropy Access ====================

    /**
     * Returns the entropy handler for capability registration.
     * Cables push entropy into this storage.
     */
    public IEntropyHandler getEntropyHandler() {
        return entropyStorage;
    }

    /**
     * Returns the internal entropy storage for direct access.
     */
    public EntropyStorage getEntropyStorage() {
        return entropyStorage;
    }

    // ==================== Tick Logic ====================

    @Override
    protected void onUpdateServer() {
        // Validate level is valid
        if (!CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        // Throttle player detection — only scan every PLAYER_SCAN_INTERVAL ticks
        if (ticker % PLAYER_SCAN_INTERVAL != 0 && !isProcessing) {
            return;
        }

        // Detect player standing on top
        ServerPlayer player = detectPlayerOnTop();

        if (player == null) {
            // No player present - pause processing
            if (isProcessing) {
                isProcessing = false;
                cachedPlayer = null;
                setChanged();
                // player left, processing paused
            }
            return;
        }

        // Player present - get evolution data
        PlayerEvolutionData data = CapabilityUtils.safeGetEvolutionData(player);
        if (data == null) {
            return;
        }

        int currentStage = data.getEvolutionStage();
        int maxStage = Config.MAX_EVOLUTION_STAGE.get();

        // Check if player can evolve further
        if (currentStage >= maxStage) {
            // Already at max stage - do nothing
            if (isProcessing) {
                isProcessing = false;
                cachedPlayer = null;
                setChanged();
            }
            return;
        }

        // Determine target stage
        int newTargetStage = currentStage + 1;

        // Validate target stage
        if (newTargetStage > maxStage) {
            newTargetStage = maxStage;
        }

        // If target changed, reset progress
        if (targetStage != newTargetStage) {
            targetStage = newTargetStage;
            progress = 0;
            isProcessing = false;
            setChanged();
        }

        // Get required progress and entropy cost
        java.util.List<? extends Integer> ticksList = Config.EVOLUTION_CHAMBER_TICKS_PER_STAGE.get();
        java.util.List<? extends Integer> costList = Config.EVOLUTION_CHAMBER_ENTROPY_COST.get();

        if (targetStage < 0 || targetStage >= ticksList.size() || targetStage >= costList.size()) {
            // Invalid stage - reset
            targetStage = 0;
            progress = 0;
            isProcessing = false;
            setChanged();
            return;
        }

        int requiredProgress = ticksList.get(targetStage);
        int totalEntropyCost = costList.get(targetStage);

        if (requiredProgress <= 0 || totalEntropyCost <= 0) {
            // Invalid config - reset
            targetStage = 0;
            progress = 0;
            isProcessing = false;
            setChanged();
            return;
        }

        // Calculate entropy cost per tick (rounded up to ensure full cost is consumed)
        int entropyPerTick = (totalEntropyCost + requiredProgress - 1) / requiredProgress; // Ceiling division

        // Use internal storage as source (cables push into it)
        IEntropyHandler entropySource = this.entropyStorage;

        if (entropySource == null) {
            // No entropy source - pause processing
            if (isProcessing) {
                isProcessing = false;
                setChanged();
            }
            return;
        }

        // Check if we can extract entropy for this tick
        int availableEntropy = entropySource.extractEntropy(entropyPerTick, true);
        if (availableEntropy < entropyPerTick) {
            // Not enough entropy - pause processing
            if (isProcessing) {
                isProcessing = false;
                setChanged();
            }
            return;
        }

        // Consume entropy atomically (extract from source - entropy is consumed)
        int extracted = entropySource.extractEntropy(entropyPerTick, false);
        if (extracted < entropyPerTick) {
            // Extraction failed - pause processing
            if (isProcessing) {
                isProcessing = false;
                setChanged();
            }
            return;
        }

        // consumed entropy

        // Entropy consumed - process
        isProcessing = true;
        cachedPlayer = player;
        progress++;

        // evolution progress updated

        // Check if evolution complete
        if (progress >= requiredProgress) {
            // Evolution complete!
            data.setEvolutionStage(targetStage);

            // Play sound
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);

            // player evolved to target stage

            // Reset for next stage
            progress = 0;
            targetStage = 0;
            isProcessing = false;
            cachedPlayer = null;
        }

        setChanged();
    }

    /**
     * Detect if a player is standing directly on top of this block.
     * 
     * @return the player if detected, null otherwise
     */
    @Nullable
    private ServerPlayer detectPlayerOnTop() {
        if (!CapabilityUtils.isValidServerLevel(level)) {
            return null;
        }

        // Detection AABB: X/Z center ±0.5, Y from block top to block top + 2
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.0;
        double z = worldPosition.getZ() + 0.5;

        AABB detectionBox = new AABB(
                x - 0.5, y, z - 0.5,
                x + 0.5, y + 2.0, z + 0.5);

        // Find players in detection box
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.isAlive() && !player.isSpectator() && detectionBox.contains(player.position())) {
                    return player;
                }
            }
        }

        return null;
    }

    // ==================== State Validation ====================

    /**
     * Internal state validation method.
     * Ensures all values are within valid ranges.
     */
    public void validateState() {
        int maxStage = Config.MAX_EVOLUTION_STAGE.get();

        // Validate target stage
        int oldTarget = targetStage;
        targetStage = StateValidator.clampInt(targetStage, 0, maxStage);
        if (oldTarget != targetStage) {
            // target stage corrected
        }

        // Validate progress
        java.util.List<? extends Integer> ticksList = Config.EVOLUTION_CHAMBER_TICKS_PER_STAGE.get();
        if (targetStage >= 0 && targetStage < ticksList.size()) {
            int maxProgress = ticksList.get(targetStage);
            int oldProgress = progress;
            progress = StateValidator.clampProgress(progress, maxProgress);
            if (oldProgress != progress) {
                // progress corrected
            }
        } else {
            // Invalid target stage - reset progress
            progress = 0;
        }

        // Validate processing state consistency
        if (progress == 0 && isProcessing) {
            isProcessing = false;
        }
    }

    // ==================== Getters ====================

    public int getProgress() {
        return progress;
    }

    public int getTargetStage() {
        return targetStage;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.evolution_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        // No GUI yet
        return null;
    }

    // ==================== Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(KEY_PROGRESS, progress);
        tag.putInt(KEY_TARGET_STAGE, targetStage);
        tag.putBoolean(KEY_IS_PROCESSING, isProcessing);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        progress = StateValidator.ensureNonNegative(SafeNBT.getInt(tag, KEY_PROGRESS, 0));
        targetStage = StateValidator.ensureNonNegative(SafeNBT.getInt(tag, KEY_TARGET_STAGE, 0));
        isProcessing = SafeNBT.getBoolean(tag, KEY_IS_PROCESSING, false);

        // Validate state after load
        validateState();
    }

    // ==================== Static Ticker ====================

    /**
     * Static ticker method for block entity registration.
     */
    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tickServer(
            net.minecraft.world.level.Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (level.isClientSide) {
            return;
        }
        if (blockEntity instanceof EvolutionChamberBlockEntity chamber) {
            chamber.onUpdateServer();
        }
    }

    // ==================== Lifecycle ====================

    @Override
    public void onLoad() {
        super.onLoad();
        // Validate state on load
        validateState();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clear cached player reference
        cachedPlayer = null;
        isProcessing = false;
    }
}

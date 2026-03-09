package net.nicotfpn.alientech.machine.entropy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EntropyComponent;
import net.nicotfpn.alientech.network.AlienEnergyNetwork;
import net.nicotfpn.alientech.network.AlienNetworkSavedData;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Entropy Reservoir — The central metaphysical I/O node.
 * Acts as a local buffer syncing with the player's global AlienEnergyNetwork.
 */
public class EntropyReservoirBlockEntity extends AlienMachineBlockEntity implements MenuProvider {

    // ==================== Components ====================
    public final EntropyComponent entropyComponent;
    public final SideConfigComponent sideConfig;

    // ==================== State ====================
    private UUID ownerId;

    // TPS Optimization: Flush to network every second
    private static final int NETWORK_SYNC_INTERVAL = 20;

    public EntropyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_RESERVOIR_BE.get(), pos, state);

        // The local buffer — now registered via ECS
        this.entropyComponent = new EntropyComponent(this, Config.ENTROPY_RESERVOIR_CAPACITY.get());
        registerComponent(this.entropyComponent);

        this.sideConfig = new SideConfigComponent(this);
        registerComponent(this.sideConfig);

        initSidedWrappers();
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    public UUID getOwner() {
        return ownerId;
    }

    @Override
    public void tickServer() {
        super.tickServer(); // Ticks components

        if (level == null || level.isClientSide)
            return;

        // Phase 2: Local Buffer <-> Network Consolidation
        if (ownerId != null && level.getGameTime() % NETWORK_SYNC_INTERVAL == 0) {
            if (level instanceof ServerLevel serverLevel) {
                AlienEnergyNetwork network = AlienNetworkSavedData.get(serverLevel).getNetwork(ownerId);

                // Flush local entropy to the global network
                long localEntropy = entropyComponent.getEntropyStored();
                if (localEntropy > 0) {
                    // We extract all we can from the local buffer and push to the network
                    network.insertEntropy(localEntropy, level.getGameTime());
                    entropyComponent.setEntropyStored(0); // Emptied local buffer
                    setChanged();
                }
            }
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }

        // Phase 2 Legacy NBT Migration (One-time extraction)
        if (tag.contains("EntropyReservoir_Entropy") && !tag.contains("Components")) {
            CompoundTag oldEntropyTag = tag.getCompound("EntropyReservoir_Entropy");
            long oldEntropy = oldEntropyTag.getInt("Entropy");

            // Push old entropy directly into our new local ECS component buffer.
            // The tickServer() consolidation loop will automatically flush this into the
            // global network.
            this.entropyComponent.setEntropyStored(oldEntropy);
        }
    }

    @Nullable
    public AlienEnergyNetwork getConnectedNetwork() {
        if (level instanceof ServerLevel serverLevel && ownerId != null) {
            return AlienNetworkSavedData.get(serverLevel).getNetwork(ownerId);
        }
        return null;
    }

    private final net.nicotfpn.alientech.entropy.IEntropyHandler capabilityHandler = new net.nicotfpn.alientech.entropy.IEntropyHandler() {
        @Override
        public long getEntropy() {
            AlienEnergyNetwork net = getConnectedNetwork();
            return net != null ? net.getEntropyStored() : entropyComponent.getEntropyStored();
        }

        @Override
        public long getMaxEntropy() {
            return Config.ENTROPY_RESERVOIR_CAPACITY.get();
        }

        @Override
        public long insertEntropy(long amount, boolean simulate) {
            AlienEnergyNetwork net = getConnectedNetwork();
            if (net != null) {
                if (!simulate)
                    net.insertEntropy(amount, level.getGameTime());
                return amount;
            } else {
                long space = entropyComponent.getMaxEntropy() - entropyComponent.getEntropyStored();
                long accepted = Math.min(amount, space);
                if (!simulate && accepted > 0)
                    entropyComponent.addEntropy(accepted);
                return accepted;
            }
        }

        @Override
        public long extractEntropy(long amount, boolean simulate) {
            AlienEnergyNetwork net = getConnectedNetwork();
            if (net != null) {
                long available = Math.min(net.getEntropyStored(), amount);
                if (!simulate && available > 0)
                    net.extractEntropy(available, level.getGameTime());
                return available;
            } else {
                long extracted = Math.min(amount, entropyComponent.getEntropyStored());
                if (!simulate && extracted > 0)
                    entropyComponent.consumeEntropy(extracted);
                return extracted;
            }
        }

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }
    };

    public net.nicotfpn.alientech.entropy.IEntropyHandler getEntropyHandler() {
        return capabilityHandler;
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.entropy_reservoir");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        return new net.nicotfpn.alientech.screen.EntropyReservoirMenu(containerId, playerInventory, this);
    }
}

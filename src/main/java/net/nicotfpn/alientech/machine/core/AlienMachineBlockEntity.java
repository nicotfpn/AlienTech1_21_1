package net.nicotfpn.alientech.machine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.machine.core.component.AlienComponent;

import java.util.ArrayList;
import java.util.List;
import net.nicotfpn.alientech.machine.core.capability.CapabilityCacheManager;

public abstract class AlienMachineBlockEntity extends BlockEntity {

    private final List<AlienComponent> components = new ArrayList<>();
    private final List<AlienComponent> activeComponents = new ArrayList<>(); // TPS Optimization
    private final List<CapabilityCacheManager<?, ?>> capabilityCaches = new ArrayList<>();

    public AlienMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Registers a component to this machine.
     * Should be called in the machine's constructor.
     */
    protected void addComponent(AlienComponent component) {
        this.components.add(component);
        if (component.isActive()) {
            this.activeComponents.add(component);
        }
    }

    /**
     * Updates the active list when a component's idle state changes.
     * Usually called internally by the component itself.
     */
    public void updateActiveState(AlienComponent component) {
        if (component.isActive() && !activeComponents.contains(component)) {
            activeComponents.add(component);
        } else if (!component.isActive()) {
            activeComponents.remove(component);
        }
    }

    public void tickServer() {
        if (activeComponents.isEmpty() || level == null)
            return; // Zero-cost idle tick!

        boolean dirty = false;
        long gameTime = level.getGameTime(); // Schedule synchronization

        for (AlienComponent component : activeComponents) {
            // Tick Interval Scheduling for advanced AAA performance
            if (gameTime % component.getTickInterval() == 0) {
                if (component.tickServer()) {
                    dirty = true;
                }
            }
        }

        if (dirty) {
            setChanged();
        }
    }

    /**
     * Static wrapper required by Block Entity Tickers in Block subclasses.
     */
    public static void tickServer(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            AlienMachineBlockEntity be) {
        be.tickServer();
    }

    /**
     * Drops inventory components when the block is broken.
     */
    public void drops() {
        if (level == null || level.isClientSide())
            return;
        for (AlienComponent component : components) {
            if (component instanceof net.nicotfpn.alientech.machine.core.component.InventoryComponent invComp) {
                for (int i = 0; i < invComp.getHandler().getSlots(); i++) {
                    net.minecraft.world.item.ItemStack stack = invComp.getHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                                worldPosition.getZ(), stack);
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        CompoundTag componentsTag = new CompoundTag();
        for (AlienComponent component : components) {
            CompoundTag sub = new CompoundTag();
            component.save(sub, provider);
            // Namespaced save to prevent overlapping keys across components
            componentsTag.put(component.getId(), sub);
        }
        tag.put("Components", componentsTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        if (tag.contains("Components")) {
            CompoundTag componentsTag = tag.getCompound("Components");
            for (AlienComponent component : components) {
                if (componentsTag.contains(component.getId())) {
                    component.load(componentsTag.getCompound(component.getId()), provider);
                }
            }
        }
    }

    // NeoForge 1.21.1 Capability Lifecycle
    @Override
    public void setRemoved() {
        super.setRemoved();
        for (CapabilityCacheManager<?, ?> cache : capabilityCaches) {
            cache.invalidateAll();
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
    }

    /**
     * Used by child classes to track custom capability caches.
     */
    protected void trackCapabilityCache(CapabilityCacheManager<?, ?> cacheManager) {
        this.capabilityCaches.add(cacheManager);
    }
}

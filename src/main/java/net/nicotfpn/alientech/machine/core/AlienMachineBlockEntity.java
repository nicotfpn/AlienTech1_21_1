package net.nicotfpn.alientech.machine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.machine.core.component.AlienComponent;

import net.nicotfpn.alientech.machine.core.capability.CapabilityCacheManager;
import net.nicotfpn.alientech.machine.core.component.TickableComponent;

import net.minecraft.core.Direction;
import net.nicotfpn.alientech.network.sideconfig.SidedEnergyStorageWrapper;
import net.nicotfpn.alientech.network.sideconfig.SidedItemHandlerWrapper;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AlienMachineBlockEntity extends BlockEntity {

    private final List<AlienComponent> components = new ArrayList<>();
    protected final List<AlienComponent> activeComponents = new ArrayList<>(); // TPS Optimization
    private final List<CapabilityCacheManager<?, ?>> capabilityCaches = new ArrayList<>();
    private final Map<Class<? extends AlienComponent>, AlienComponent> componentRegistry = new HashMap<>();

    // Instanciados uma vez no construtor — nunca recriados
    private final SidedItemHandlerWrapper[] sidedItemHandlers = new SidedItemHandlerWrapper[6];
    private final SidedEnergyStorageWrapper[] sidedEnergyStorages = new SidedEnergyStorageWrapper[6];

    public AlienMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Registers a component to this machine.
     * Should be called in the machine's constructor.
     * 
     * @deprecated Use registerComponent() which also supports fluid chaining and
     *             registry lookups.
     */
    @Deprecated
    protected void addComponent(AlienComponent component) {
        registerComponent(component);
    }

    /**
     * Registra um componente nesta máquina.
     * Deve ser chamado no construtor da subclasse, na ordem correta de execução.
     * Componentes TickableComponent são automaticamente adicionados ao loop de
     * tick.
     *
     * @param component O componente a registrar. Não pode ser null.
     * @param <T>       Tipo do componente (deve estender AlienComponent).
     * @return O próprio componente, para encadeamento fluente no construtor.
     */
    protected <T extends AlienComponent> T registerComponent(T component) {
        this.components.add(component);
        this.componentRegistry.put(component.getClass(), component);
        this.activeComponents.add(component);
        return component;
    }

    /**
     * Recupera um componente registrado por tipo.
     * Lança exceção se o componente não estiver registrado — falha explícita é
     * preferível
     * a NullPointerException silenciosa.
     */
    @SuppressWarnings("unchecked")
    public <T extends AlienComponent> T getComponent(Class<T> type) {
        T component = (T) componentRegistry.get(type);
        if (component == null) {
            throw new IllegalStateException(
                    "AlienTech: Componente " + type.getSimpleName() +
                            " não registrado em " + this.getClass().getSimpleName() +
                            " @ " + worldPosition);
        }
        return component;
    }

    protected void initSidedWrappers() {
        if (!hasComponent(SideConfigComponent.class))
            return;
        SideConfigComponent sideConfig = getComponent(SideConfigComponent.class);

        InventoryComponent inventory = hasComponent(InventoryComponent.class) ? getComponent(InventoryComponent.class)
                : null;
        EnergyComponent energy = hasComponent(EnergyComponent.class) ? getComponent(EnergyComponent.class) : null;

        for (Direction dir : Direction.values()) {
            if (inventory != null) {
                sidedItemHandlers[dir.get3DDataValue()] = new SidedItemHandlerWrapper(inventory, sideConfig, dir);
            }
            if (energy != null) {
                sidedEnergyStorages[dir.get3DDataValue()] = new SidedEnergyStorageWrapper(energy, sideConfig, dir);
            }
        }
    }

    public SidedItemHandlerWrapper getSidedItemHandler(Direction dir) {
        return sidedItemHandlers[dir.get3DDataValue()];
    }

    public SidedEnergyStorageWrapper getSidedEnergyStorage(Direction dir) {
        return sidedEnergyStorages[dir.get3DDataValue()];
    }

    /**
     * Retorna true se a máquina tiver o componente daquele tipo.
     */
    public boolean hasComponent(Class<? extends AlienComponent> type) {
        return componentRegistry.containsKey(type);
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

    /**
     * Loop principal do servidor. Executa apenas os componentes com trabalho
     * pendente.
     *
     * IMPORTANTE: A ordem da lista activeComponents é significativa.
     * Ordem recomendada de registro no construtor da máquina:
     * 1. EnergyComponent (receber FE antes de processar)
     * 2. ProcessingComponent (consumir FE e processar)
     * 3. AutoTransferComponent (ejetar output após processamento)
     * 4. SideConfigComponent NÃO é tickable — apenas consultado pelos wrappers
     */
    public void tickServer() {
        if (activeComponents.isEmpty() || level == null)
            return; // Zero-cost idle tick!

        boolean dirty = false;
        long gameTime = level.getGameTime(); // Schedule synchronization

        for (AlienComponent component : activeComponents) {
            // Se for TickableComponent, honrar o contrato de isActive()
            if (component instanceof TickableComponent tickable) {
                if (tickable.isActive()) {
                    if (gameTime % component.getTickInterval() == 0) {
                        try {
                            tickable.tick(this);
                            dirty = true;
                        } catch (Exception e) {
                            net.nicotfpn.alientech.AlienTech.LOGGER
                                    .error("Error ticking component: " + component.getClass().getSimpleName(), e);
                        }
                    }
                }
            } else {
                // Legacy support code for components that haven't migrated to TickableComponent
                // yet
                if (component.isActive() && gameTime % component.getTickInterval() == 0) {
                    if (component.tickServer()) {
                        dirty = true;
                    }
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

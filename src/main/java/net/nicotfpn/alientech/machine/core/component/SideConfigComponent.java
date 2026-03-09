package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Componente responsável por armazenar e gerenciar a configuração de I/O
 * de cada face de uma máquina.
 *
 * NÃO implementa TickableComponent — é puramente consultado pelos wrappers
 * de capability e pelo AutoTransferComponent.
 */
public class SideConfigComponent extends AlienComponent {

    // Mapa: CapabilityType → (Direction → IOSideMode)
    // EnumMap para acesso O(1) sem boxing overhead
    private final Map<CapabilityType, Map<Direction, IOSideMode>> config = new EnumMap<>(CapabilityType.class);

    // Cache de faces ativas (PUSH ou PULL) para o AutoTransferComponent
    // Invalida quando setMode() é chamado
    private boolean activeTransferCacheDirty = true;
    private List<Direction> cachedActiveFaces = List.of();

    public SideConfigComponent(net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity tile) {
        super(tile);
        // Inicializar todos os modos como NONE por padrão
        for (CapabilityType type : CapabilityType.values()) {
            Map<Direction, IOSideMode> faceMap = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                faceMap.put(direction, IOSideMode.NONE);
            }
            config.put(type, faceMap);
        }
    }

    /**
     * Retorna o modo configurado para uma face e tipo de capability.
     * O(1) — safe para chamar no tick (nos wrappers).
     */
    public IOSideMode getMode(Direction face, CapabilityType type) {
        return config.get(type).get(face);
    }

    /**
     * Define o modo de uma face para um tipo de capability.
     *
     * VALIDAÇÃO: BOTH é proibido para ENERGY e ENTROPY.
     * Esta validação é a linha de defesa contra loops de feedback energético.
     *
     * Após modificação, invalida o cache de faces ativas e notifica a BlockEntity
     * para reregistrar suas capabilities (invalidateCaps).
     *
     * @throws IllegalArgumentException se BOTH for aplicado a ENERGY ou ENTROPY.
     */
    public void setMode(Direction face, CapabilityType type, IOSideMode mode) {
        if (mode == IOSideMode.BOTH && type != CapabilityType.ITEM) {
            throw new IllegalArgumentException(
                    "AlienTech: IOSideMode.BOTH é proibido para " + type +
                            ". Apenas CapabilityType.ITEM aceita modo BOTH.");
        }
        config.get(type).put(face, mode);
        activeTransferCacheDirty = true;
        // A BlockEntity deve sobrescrever este método para chamar invalidateCaps()
        onModeChanged(face, type, mode);
    }

    /**
     * Hook chamado após cada mudança de modo.
     * A BlockEntity que hospedar este componente deve escutar este hook
     * para chamar blockEntity.invalidateCaps() e setChanged().
     */
    protected void onModeChanged(Direction face, CapabilityType type, IOSideMode newMode) {
        // Override na BlockEntity ou via lambda injetado no construtor
    }

    /**
     * Retorna se ao menos uma face tem modo PUSH ou PULL para qualquer tipo.
     * Usado pelo AutoTransferComponent.isActive() em O(1) (resultado cacheado).
     */
    public boolean hasAnyActiveTransfer() {
        if (activeTransferCacheDirty) {
            rebuildActiveCache();
        }
        return !cachedActiveFaces.isEmpty();
    }

    /**
     * Retorna as faces com modo PUSH ou PULL para ITEMS.
     * Resultado cacheado — apenas recalcula quando dirty.
     */
    public List<Direction> getActiveFaces() {
        if (activeTransferCacheDirty) {
            rebuildActiveCache();
        }
        return cachedActiveFaces;
    }

    private void rebuildActiveCache() {
        cachedActiveFaces = Direction.stream()
                .filter(d -> config.get(CapabilityType.ITEM).get(d).isActive())
                .toList();
        activeTransferCacheDirty = false;
    }

    // =========================================================================
    // SERIALIZAÇÃO NBT
    // =========================================================================

    @Override
    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag configTag = new CompoundTag();
        for (CapabilityType capType : CapabilityType.values()) {
            CompoundTag typeTag = new CompoundTag();
            for (Direction dir : Direction.values()) {
                typeTag.putString(dir.getSerializedName(),
                        config.get(capType).get(dir).getSerializedName());
            }
            configTag.put(capType.name(), typeTag);
        }
        tag.put("side_config", configTag);
    }

    @Override
    public void load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        if (!tag.contains("side_config"))
            return;
        CompoundTag configTag = tag.getCompound("side_config");
        for (CapabilityType capType : CapabilityType.values()) {
            if (!configTag.contains(capType.name()))
                continue;
            CompoundTag typeTag = configTag.getCompound(capType.name());
            for (Direction dir : Direction.values()) {
                String modeName = typeTag.getString(dir.getSerializedName());
                IOSideMode mode = IOSideMode.byName(modeName, IOSideMode.NONE);
                config.get(capType).put(dir, mode);
            }
        }
        activeTransferCacheDirty = true;
    }

    @Override
    public String getId() {
        return "SideConfig";
    }
}

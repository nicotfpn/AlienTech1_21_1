package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;

import java.util.List;

/**
 * Componente de transferência automática (AutoEject / AutoPull).
 *
 * Opera APENAS nas faces configuradas como PUSH ou PULL.
 * NUNCA itera sobre Direction.values() cegamente — usa o cache do
 * SideConfigComponent.
 *
 * Cache de neighbors: BlockEntity vizinho é resolvido uma vez e cacheado por
 * face.
 * Cache é invalidado em onNeighborChanged da BlockEntity dona.
 */
public class AutoTransferComponent extends AlienComponent implements TickableComponent {

    // Cache de handlers vizinhos por face (index = Direction.get3DDataValue())
    // null = não resolvido ainda; EMPTY_SENTINEL = vizinho sem ItemHandler
    private final IItemHandler[] neighborCache = new IItemHandler[6];
    private boolean neighborCacheDirty = true;
    private SideConfigComponent sideConfig; // Injetado após registro

    public AutoTransferComponent(AlienMachineBlockEntity tile) {
        super(tile);
    }

    @Override
    public String getId() {
        return "AutoTransfer";
    }

    @Override
    public boolean isActive() {
        // O(1) — consulta cache do SideConfigComponent sem percorrer directions
        return sideConfig != null && sideConfig.hasAnyActiveTransfer();
    }

    @Override
    public void tick(AlienMachineBlockEntity machine) {
        if (sideConfig == null)
            return;
        InventoryComponent inventory = machine.hasComponent(InventoryComponent.class)
                ? machine.getComponent(InventoryComponent.class)
                : null;
        if (inventory == null)
            return;

        // Apenas faces ativas (PUSH ou PULL) — lista pré-computada, não
        // Direction.values()
        List<Direction> activeFaces = sideConfig.getActiveFaces();

        for (Direction face : activeFaces) {
            IOSideMode mode = sideConfig.getMode(face, CapabilityType.ITEM);

            if (mode == IOSideMode.PUSH) {
                performEject(machine, inventory, face);
            } else if (mode == IOSideMode.PULL) {
                performPull(machine, inventory, face);
            }
        }
    }

    /**
     * Empurra itens do inventário desta máquina para o inventário adjacente na
     * face.
     * Resolve o neighbor uma vez por invalidação de cache.
     */
    private void performEject(AlienMachineBlockEntity machine,
            InventoryComponent inventory,
            Direction face) {
        IItemHandler neighbor = resolveNeighbor(machine, face);
        if (neighbor == null)
            return;

        for (int slot = 0; slot < inventory.getHandler().getSlots(); slot++) {
            ItemStack stack = inventory.getHandler().extractItem(slot, 64, true); // simulate
            if (stack.isEmpty())
                continue;

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(neighbor, stack, false);
            if (remainder.getCount() != stack.getCount()) {
                // Algo foi inserido — confirmar extração
                inventory.getHandler().extractItem(slot, stack.getCount() - remainder.getCount(), false);
                break; // Um slot por tick para distribuir a carga de TPS (evitar travamento)
            }
        }
    }

    /**
     * Puxa itens do inventário adjacente na face para o inventário desta máquina.
     */
    private void performPull(AlienMachineBlockEntity machine,
            InventoryComponent inventory,
            Direction face) {
        IItemHandler neighbor = resolveNeighbor(machine, face);
        if (neighbor == null)
            return;

        for (int slot = 0; slot < neighbor.getSlots(); slot++) {
            ItemStack stack = neighbor.extractItem(slot, 64, true); // simulate
            if (stack.isEmpty())
                continue;

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                    inventory.getHandler(), stack, false);
            if (remainder.getCount() != stack.getCount()) {
                neighbor.extractItem(slot, stack.getCount() - remainder.getCount(), false);
                break; // Um slot por tick
            }
        }
    }

    /**
     * Resolve e cacheia o IItemHandler do vizinho em uma direção.
     * Cache é invalidado por neighborCacheDirty (setado em onNeighborChanged).
     *
     * @return O IItemHandler do vizinho, ou null se não houver.
     */
    private IItemHandler resolveNeighbor(AlienMachineBlockEntity machine, Direction face) {
        int idx = face.get3DDataValue();
        if (neighborCacheDirty || neighborCache[idx] == null) {
            BlockPos neighborPos = machine.getBlockPos().relative(face);
            if (machine.getLevel() != null) {
                neighborCache[idx] = machine.getLevel().getCapability(
                        Capabilities.ItemHandler.BLOCK, neighborPos, face.getOpposite());
            }
        }
        return neighborCache[idx];
    }

    /** Chamado pela BlockEntity em onNeighborChanged para invalidar o cache. */
    public void invalidateNeighborCache(BlockPos neighborPos, Block block) {
        neighborCacheDirty = true;
        // Não limpar o array — apenas marcar como dirty para lazy reload
        java.util.Arrays.fill(neighborCache, null);
    }

    public void injectSideConfig(SideConfigComponent sideConfig) {
        this.sideConfig = sideConfig;
    }

    @Override
    public void save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        // Nada de estado persistente aqui. Active faces e Inventory são salvos em seus
        // componentes.
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        // Nada a carregar.
    }
}

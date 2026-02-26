package net.nicotfpn.alientech.ui.sync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicotfpn.alientech.network.SyncDataPacket;

import java.util.ArrayList;
import java.util.List;

public abstract class AlienContainerMenu extends AbstractContainerMenu {

    private final List<SyncableValue<?>> trackables = new ArrayList<>();
    protected final Player player;

    protected AlienContainerMenu(MenuType<?> menuType, int containerId, Player player) {
        super(menuType, containerId);
        this.player = player;
    }

    protected void track(SyncableValue<?> val) {
        trackables.add(val);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (player instanceof ServerPlayer serverPlayer) {
            List<SyncDataPacket.SyncPayload> dirtyPayloads = new ArrayList<>();
            for (int i = 0; i < trackables.size(); i++) {
                SyncableValue<?> val = trackables.get(i);
                if (val.isDirty()) {
                    if (val.get() instanceof Long lVal) {
                        dirtyPayloads.add(new SyncDataPacket.SyncPayload(i, lVal));
                    } else if (val.get() instanceof Integer iVal) {
                        dirtyPayloads.add(new SyncDataPacket.SyncPayload(i, iVal.longValue()));
                    } else if (val.get() instanceof Boolean bVal) {
                        dirtyPayloads.add(new SyncDataPacket.SyncPayload(i, bVal ? 1L : 0L));
                    }
                    val.markClean(); // Resets diff state locally
                }
            }

            if (!dirtyPayloads.isEmpty()) {
                PacketDistributor.sendToPlayer(serverPlayer, new SyncDataPacket(containerId, dirtyPayloads));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void handleSync(List<SyncDataPacket.SyncPayload> payloads) {
        for (SyncDataPacket.SyncPayload payload : payloads) {
            int index = payload.index();
            if (index >= 0 && index < trackables.size()) {
                SyncableValue<?> trackable = trackables.get(index);
                if (trackable.get() instanceof Long) {
                    ((SyncableValue<Long>) trackable).set(payload.value());
                } else if (trackable.get() instanceof Integer) {
                    ((SyncableValue<Integer>) trackable).set((int) payload.value());
                } else if (trackable.get() instanceof Boolean) {
                    ((SyncableValue<Boolean>) trackable).set(payload.value() == 1L);
                }
            }
        }
    }
}

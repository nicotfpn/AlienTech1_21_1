package net.nicotfpn.alientech.machine.core.upgrade;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.AlienComponent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mekanism-grade Upgrade Component.
 * Scaled values: Math.pow(UpgradeType.multiplier, count).
 */
public class UpgradeComponent extends AlienComponent {
    private final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);

    @Override
    public String getId() {
        return "Upgrades";
    }

    public UpgradeComponent(AlienMachineBlockEntity tile) {
        super(tile);
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
            CompoundTag upgradeTag = new CompoundTag();
            upgradeTag.putString("Type", entry.getKey().name());
            upgradeTag.putInt("Count", entry.getValue());
            list.add(upgradeTag);
        }
        tag.put("Upgrades", list);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        upgrades.clear();
        if (tag.contains("Upgrades", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Upgrades", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag upgradeTag = list.getCompound(i);
                try {
                    UpgradeType type = UpgradeType.valueOf(upgradeTag.getString("Type"));
                    int count = upgradeTag.getInt("Count");
                    upgrades.put(type, Math.min(count, type.getMaxStack()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public int getUpgradeCount(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeCount(UpgradeType type, int count) {
        int finalCount = Math.max(0, Math.min(count, type.getMaxStack()));
        if (finalCount == 0) {
            upgrades.remove(type);
        } else {
            upgrades.put(type, finalCount);
        }
    }

    /**
     * Calculates the scaled multiplier: BASE_MULT ^ COUNT.
     */
    public double getMultiplier(UpgradeType type) {
        int count = getUpgradeCount(type);
        if (count <= 0)
            return 1.0;
        return Math.pow(type.getMultiplier(), count);
    }

    /**
     * Helper for energy scaling.
     * Mekanism logic: Math.pow(BaseMult, speed - energy).
     */
    public double getEnergyMultiplier() {
        int speed = getUpgradeCount(UpgradeType.SPEED);
        int energy = getUpgradeCount(UpgradeType.ENERGY);
        // speed makes it more expensive, energy makes it cheaper
        return Math.pow(UpgradeType.SPEED.getMultiplier(), speed - energy);
    }
}

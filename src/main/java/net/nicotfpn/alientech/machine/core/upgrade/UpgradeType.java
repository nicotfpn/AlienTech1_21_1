package net.nicotfpn.alientech.machine.core.upgrade;

import net.minecraft.network.chat.Component;

/**
 * Mekanism-style Upgrade Types for AlienTech machines.
 */
public enum UpgradeType {
    SPEED("Speed", 8, 1.5),
    ENERGY("Energy", 8, 1.5),
    ENTROPY("Entropy", 8, 1.25),
    MUFFLING("Muffling", 4, 1.0),
    OVERCLOCK("Overclock", 1, 4.0);

    private final String name;
    private final int maxStack;
    private final double multiplier;

    UpgradeType(String name, int maxStack, double multiplier) {
        this.name = name;
        this.maxStack = maxStack;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public Component getDescription() {
        return Component.translatable("upgrade.alientech." + name.toLowerCase());
    }
}

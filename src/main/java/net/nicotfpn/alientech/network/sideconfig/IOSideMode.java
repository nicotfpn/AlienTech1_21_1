package net.nicotfpn.alientech.network.sideconfig;

import net.minecraft.util.StringRepresentable;

public enum IOSideMode implements StringRepresentable {

    /** Face completamente bloqueada. Nenhuma capability é exposta. */
    NONE("none", 0xFF393E46),
    INPUT("input", 0xFF00ADB5),
    OUTPUT("output", 0xFFFFD700),
    BOTH("both", 0xFF9B59B6),
    PUSH("push", 0xFFE67E22),
    PULL("pull", 0xFF2ECC71);

    private final String serializedName;
    private final int color;

    IOSideMode(String serializedName, int color) {
        this.serializedName = serializedName;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** Modos que permitem inserção (tanto passiva quanto ativa). */
    public boolean allowsInsertion() {
        return this == INPUT || this == BOTH || this == PULL;
    }

    /** Modos que permitem extração (tanto passiva quanto ativa). */
    public boolean allowsExtraction() {
        return this == OUTPUT || this == BOTH || this == PUSH;
    }

    /** Modos que ativam o AutoTransferComponent (requerem tick ativo). */
    public boolean isActive() {
        return this == PUSH || this == PULL;
    }

    /**
     * Retorna o próximo modo no ciclo de configuração para a GUI (click esquerdo).
     * Ciclo para ITEMS: NONE → INPUT → OUTPUT → BOTH → PUSH → PULL → NONE
     * Ciclo para ENERGY/ENTROPY: NONE → INPUT → OUTPUT → PUSH → NONE (pula BOTH)
     */
    public IOSideMode next(CapabilityType capType) {
        return switch (this) {
            case NONE -> INPUT;
            case INPUT -> OUTPUT;
            case OUTPUT -> (capType == CapabilityType.ITEM) ? BOTH : PUSH;
            case BOTH -> PUSH; // só acessível para ITEM
            case PUSH -> PULL;
            case PULL -> NONE;
        };
    }

    /** Requires StringRepresentable to be accessed statically by string lookup */
    public static IOSideMode byName(String name, IOSideMode fallback) {
        for (IOSideMode mode : values()) {
            if (mode.getSerializedName().equals(name)) {
                return mode;
            }
        }
        return fallback;
    }
}

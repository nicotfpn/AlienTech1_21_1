package net.nicotfpn.alientech.util;

public final class ScalingMath {

    private ScalingMath() {
        throw new UnsupportedOperationException("ScalingMath is a utility class.");
    }

    /**
     * Escala AUMENTANDO um valor base de forma logarítmica.
     * Uso: aumentar custo de FE com OVERCLOCK, aumentar capacidade com upgrades.
     *
     * Fórmula: result = base * (1 + (log2(1 + upgradeCount)) * factor)
     *
     * @param base         Valor base sem upgrades. Deve ser > 0.
     * @param upgradeCount Quantidade de upgrades instalados (0–64). Negativo é
     *                     tratado como 0.
     * @param factor       Intensidade do upgrade (ex: 0.5 para SPEED, 0.7 para
     *                     OVERCLOCK).
     * @return Valor escalado. Nunca menor que {@code base}.
     */
    public static long scale(long base, int upgradeCount, double factor) {
        if (upgradeCount <= 0)
            return base;
        double multiplier = 1.0 + (Math.log1p(upgradeCount) / Math.log(2)) * factor;
        return (long) (base * multiplier);
    }

    /**
     * Escala REDUZINDO um valor base de forma logarítmica.
     * Uso: reduzir custo de FE com ENERGY_EFFICIENCY, reduzir tempo com SPEED.
     *
     * Fórmula: result = base / (1 + (log2(1 + upgradeCount)) * factor)
     *
     * @param base         Valor base sem upgrades. Deve ser > 0.
     * @param upgradeCount Quantidade de upgrades instalados (0–64). Negativo é
     *                     tratado como 0.
     * @param factor       Intensidade do upgrade.
     * @return Valor escalado. Nunca menor que 1.
     */
    public static long scaleDown(long base, int upgradeCount, double factor) {
        if (upgradeCount <= 0)
            return base;
        double divisor = 1.0 + (Math.log1p(upgradeCount) / Math.log(2)) * factor;
        return Math.max(1L, (long) (base / divisor));
    }
}

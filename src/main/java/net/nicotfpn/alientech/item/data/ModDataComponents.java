package net.nicotfpn.alientech.item.data;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;

public final class ModDataComponents {

    // DeferredRegister central para todos os DataComponentTypes do mod
    public static final DeferredRegister<DataComponentType<?>> REGISTRY = DeferredRegister.createDataComponents(
            net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
            AlienTech.MOD_ID);

    // Componentes concretos serão registrados como campos estáticos aqui
    // nas fases seguintes (UpgradeData na Fase 2, PrisonData no futuro, etc.)

    private ModDataComponents() {
    }
}

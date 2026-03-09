package net.nicotfpn.alientech.network.sideconfig;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;

/**
 * Proxy de IEnergyStorage que filtra operações baseado no SideConfigComponent.
 *
 * LEMBRETE: IOSideMode.BOTH é PROIBIDO para ENERGY.
 * Os modos válidos para energia são: NONE, INPUT, OUTPUT, PUSH.
 * Esta classe não precisa validar isso — SideConfigComponent.setMode() já
 * impede BOTH.
 */
public class SidedEnergyStorageWrapper implements IEnergyStorage {

    private final EnergyComponent energy;
    private final SideConfigComponent sideConfig;
    private final Direction face;

    public SidedEnergyStorageWrapper(EnergyComponent energy,
            SideConfigComponent sideConfig,
            Direction face) {
        this.energy = energy;
        this.sideConfig = sideConfig;
        this.face = face;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ENERGY);
        if (!mode.allowsInsertion())
            return 0;
        return energy.getEnergyStorage().receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ENERGY);
        if (!mode.allowsExtraction())
            return 0;
        return energy.getEnergyStorage().extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return energy.getEnergyStorage().getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energy.getEnergyStorage().getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return sideConfig.getMode(face, CapabilityType.ENERGY).allowsExtraction();
    }

    @Override
    public boolean canReceive() {
        return sideConfig.getMode(face, CapabilityType.ENERGY).allowsInsertion();
    }
}

package net.nicotfpn.alientech.machine.core.component;

import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;

/**
 * Contrato obrigatório para qualquer AlienComponent que executa lógica no
 * server tick.
 *
 * REGRA DE TPS SAFETY:
 * isActive() deve executar em O(1) — NUNCA faça I/O de mundo ou lookup de
 * BlockEntity aqui.
 * Apenas verifique estado interno do componente (flags, contagens, etc.).
 */
public interface TickableComponent {

    /**
     * Retorna true se e somente se este componente tem trabalho pendente neste
     * tick.
     *
     * Exemplos de retorno FALSE (idle):
     * - InventoryComponent de input vazio (ProcessingComponent)
     * - Nenhuma face configurada como PUSH ou PULL (AutoTransferComponent)
     * - Energia já no máximo (EnergyReceiveComponent)
     * - Buffer de output cheio e sem destino disponível
     *
     * @return true se o componente deve ser tickado; false para ser pulado (idle).
     */
    boolean isActive();

    /**
     * Executa a lógica do componente para este tick.
     * Só será chamado se isActive() retornou true.
     *
     * @param machine A BlockEntity dona deste componente. Nunca null.
     */
    void tick(AlienMachineBlockEntity machine);
}

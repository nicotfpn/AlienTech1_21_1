# 🪐 AlienTech — Mod Architecture Blueprint (NeoForge 1.21.1)

> **CONTEXTO PARA IA / SYSTEM PROMPT:**
> Se você está lendo este arquivo como referência, assuma o papel de um Arquiteto de Software Java Sênior especializado em modding NeoForge. O AlienTech é construído com padrões de qualidade equivalentes (ou superiores) ao **Mekanism** e **Thermal Expansion**, focando em alta performance de TPS, segurança NBT e arquitetura 100% componentizada (ECS). **NUNCA crie God Classes ou lógicas acopladas.**

---

## 1. 🏗️ Arquitetura Core: Entity Component System (ECS)
A base de todas as máquinas do AlienTech abandonou a herança monolítica antiga. Agora, usamos um sistema puramente baseado em componentes acoplados em tempo de execução à classe base `AlienMachineBlockEntity`.

### A Classe Base (`AlienMachineBlockEntity`)
- **Herança:** Estende `AlienBlockEntity`.
- **Lógica de Tick:** Sobrescreve `tickServer()` e roda um loop `for` sobre uma lista estática de **Componentes Ativos** registrados durante o bloco `init`.
- **TPS Safety:** Se uma máquina não estiver ativamente processando ou transferindo energia, seus componentes entram em estado de `idle` para economizar CPU do servidor. NUNCA utilize `level.getBlockEntity()` em todo tick cegamente.

### Os Componentes Disponíveis (`net.nicotfpn.alientech.machine.core.component.*`)
1. **`InventoryComponent`**: Wrapper escalável para `ItemStackHandler`. Suporta lambdas para regras estritas de inserção/extração por slot (`isItemValid`).
2. **`EnergyComponent`**: Wrapper otimizado para a `IEnergyStorage` do Forge (lidando com FE). Suporta limites estritos de `maxReceive` e `maxExtract`.
3. **`EntropyComponent`**: O motor mágico do mod. Utiliza a abstração customizada baseada em **`long`** (e não `int` para evitar overflows de endgame).
4. **`ProcessingComponent`**: Gerencia o loop mecânico de "Progresso". Depende de lambdas injetadas para `getCalculatedMaxProgress` e `onProcessComplete()`.

---

## 2. ⚡ A Energia "Entropia" (O Sistema Mágico)
A "Entropia" é uma energia biológica e metafísica extraída de entidades vivas. Ela coexiste junto com o FE (Forge Energy).

### A Abstração em `long`
Todo o ecossistema de Entropia (`IEntropyHandler`, `EntropyStorage`, `EntropyTransaction`, e NBT Serializations) suporta **100% a primitividade `long`**.
- Sempre use wrappers em lambdas quando precisar criar blocos lógicos matemáticos seguros para extração.
- Não sofra *"lossy conversion"* castando para `int` em lógicas centrais de rede.

### O Pipeline da Entropia
1. **Pocket Dimensional Prison:** Item (suporta DataComponents Native 1.21) que serializa Mobs vivos (NBT UUID + EntityType) removendo-os do mundo.
2. **Decay Chamber:** Extrai o HP base do Mob armazenado na *Prison* e gera **Entropy Biomass**.
3. **Entropy Reservoir:** Queima *Biomass* e *FE* simultaneamente para condensar tudo em **Decaying Gravitons** (item combustível).
4. **Quantum Vacuum Turbine:** Consome o *Graviton* para gerar fluxos colossais de **FE**.
5. **Evolution Chamber:** Consome Entropia de *Reservoirs* adjacentes para ativar o sistema de Vínculo de Jogador (attachments) e evoluí-lo para novos estágios de poder.
6. **Alien Pyramid:** Multibloco que acelera todas as *Turbines* próximas magicamente e gera entropia gratuita baseada na rede.

---

## 3. 🖥️ Interface de Usuário e Sincronização (UI/UX)
Sincronizar `longs` ou limites massivos de energia causando desync visual é um anti-pattern inaceitável.

### O `AlienContainerMenu` e `SyncableLong`
Todas as GUIs estendem `AlienContainerMenu`, com a seguinte mecânica obrigatória:
1. Em vez de enviar `ContainerData` primitivo truncado (legacy)...
2. Instanciamos `SyncableLong.create()` passando apenas um `Supplier<Long>` (getter do valor real do Componente) e um setter `null`.
3. O Menu registra este SyncableValue e a UI (Screen) o escuta em tempo real usando *Client Caching Proxy*.
4. **Resultado:** Não precisamos empurrar NBT states enormes o tempo todo na rede; a tela rastreia o proxy fluído.

---

## 4. ⚙️ Fase 0 — Foundation Hardening
Esta fundação matemática, de threading e contratos de dados é obrigatória antes de qualquer feature nova.

### 0.1 — Fórmula de Escalonamento Logarítmico (Canonical Formula)
Toda lógica de upgrade no mod deve usar esta fórmula canônica. Não invente variantes.
```java
// Arquivo: net.nicotfpn.alientech.util.ScalingMath
public final class ScalingMath {
    private ScalingMath() {}

    /**
     * Escala logaritmicamente um valor base dado um número de upgrades instalados.
     * Garante que o retorno NUNCA cause overflow em long com até 64 upgrades.
     */
    public static long scale(long base, int upgradeCount, double factor) {
        if (upgradeCount <= 0) return base;
        double multiplier = 1.0 + (Math.log1p(upgradeCount) / Math.log(2)) * factor;
        return (long)(base * multiplier);
    }

    /**
     * Versão inversa: reduz custo (ex: ENERGY_EFFICIENCY reduz consumo de FE).
     */
    public static long scaleDown(long base, int upgradeCount, double factor) {
        if (upgradeCount <= 0) return base;
        double divisor = 1.0 + (Math.log1p(upgradeCount) / Math.log(2)) * factor;
        return Math.max(1L, (long)(base / divisor));
    }
}
```

### 0.2 — Thread Safety Contract
Regra absoluta: Todo acesso de mutação a singletons globais (NetworkManager, caches de grafo) deve acontecer exclusivamente na thread do servidor lógico.
```java
public void invalidateGraph(Level level, BlockPos origin) {
    if (level.isClientSide()) {
        throw new IllegalStateException("AlienTech: NetworkManager mutated on client thread! Origin: " + origin);
    }
}
```

### 0.3 — Contrato de DataComponents para Items Dinâmicos
Em NeoForge 1.21.1, o uso de `.getTag()` / `.setTag()` em ItemStacks é **PROIBIDO**.
Todo item com estado dinâmico (Upgrade, Prison) deve usar DataComponents registrados.
```java
public static final Supplier<DataComponentType<UpgradeData>> UPGRADE_DATA =
    REGISTRY.register("upgrade_data", () ->
        DataComponentType.<UpgradeData>builder()
            .persistent(UpgradeData.CODEC)
            .networkSynchronized(UpgradeData.STREAM_CODEC)
            .build()
    );
```

### 0.4 — Idle Guard (TPS Safety Macro)
Todo componente que opera no `tickServer` deve implementar este contrato:
```java
public interface TickableComponent {
    boolean isActive();
    void tick(AlienMachineBlockEntity machine);
}
```
O loop em `AlienMachineBlockEntity.tickServer()` itera apenas sobre componentes que retornam `isActive() == true`.

---

## 5. 🛠️ Check-list de Mod Standards
Ao escrever qualquer funcionalidade nova, respeite o seguinte check-list de Sanidade Estrutural:

- [ ] **Capacidades Fora da Classe Core:** Use o `CommonModEvents.java` e Registries assíncronos providos pela NeoForge `RegisterCapabilitiesEvent` para atar o `InventoryComponent` do bloco genérico a interface `ItemHandler`.
- [ ] **Evite Variáveis NBT Soltas:** Sempre chame `super.saveAdditional()` pois os componentes serializam automaticamente seu estado isolado, em vez de abarrotar a BlockEntity de métodos de encode.
- [ ] **DataComponents para Items:** No Minecraft 1.21.1+, *NBT Items (Tags)* não devem ser usados. Todo item dinâmico deve instanciar e modificar `DataComponents` imutáveis.
- [ ] **Não usar Hardcode Registry:** Para blocos genéricos, não duplique propriedades nos construtores do Block; puxe Properties através do `.copy(Blocks.IRON_BLOCK)` com métodos chaining de `.strength()` etc.
- [ ] **Use as Abstrações do Mod:** Para logs ou checagens vitais, chame `CapabilityUtils`, `StateValidator` e `SafeNBT` para evitar Crahses com NullPointerException.

---

## 6. 🧠 As Proibições Arquiteturais (Anti-Patterns Definitivos)
Para que o AlienTech atinja e mantenha o nível do Mekanism (em termos de TPS e Escalabilidade), é estritamente proibido:

1. **PROIBIDO:** Herança Monolítica (`extends AbstractMachineBlocoEntityComTudoDentro`). Máquinas **devem** ser compostas por Componentes instanciados nos construtores.
2. **PROIBIDO:** Polling contínuo de vizinhos no server tick (ex: percorrer O(N³) blocos a cada `scannerTick == 20` para achar pilares da Pirâmide ou Cabos adjacentes).
    - *Solução Padrão:* Usar o Evento Vanilla `onNeighborChanged` ou criar um Cache/Manager Baseado em Grafo que recompila rotas apens quando blocos mudam fisicamente de lugar.
3. **PROIBIDO:** Sync de `long` strings ou Integers/Longs gigantes via vanilla `ContainerData`.
    - *Solução Padrão:* Envolver as variáveis do Menu no nosso encapsulamento `SyncableLong` que usa o cache proxy dinâmico, enviando payload apenas em caso de mutação delta `isDirty`.
4. **PROIBIDO:** Hardcodar Custos Mágicos ou Loops Lineares.
    - *Solução Padrão:* Se um jogador colocar 8 upgrades de velocidade na máquina, o custo de energia/entropia deve seguir `ScalingMath.scale` (para nunca quebrar as barreiras numéricas).
5. **PROIBIDO:** Tier em NBT de BlockEntity. Máquinas em `MachineTier` diferentes devem ser instanciadas via Blocos base diferentes (para que dropem corretamente se mineradas com Silk Touch/Pickaxe e evitem desync no mundo).

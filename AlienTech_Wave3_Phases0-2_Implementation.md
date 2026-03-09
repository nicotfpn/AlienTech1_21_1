# 🪐 AlienTech — Wave 3 Implementation Brief
## Fases 0, 1 e 2 — Guia Completo para IA Codificadora

> **LEIA ESTE DOCUMENTO INTEIRO ANTES DE ESCREVER UMA LINHA DE CÓDIGO.**
>
> Você é um **Arquiteto de Software Java Sênior** especializado em NeoForge 1.21.1.
> Padrão de qualidade alvo: **Mekanism 10.x**. Sem God Classes. Sem NBT solto em Items.
> Sem polling de vizinhos no tick. Sem cast de `long` para `int` em lógica central.
> Cada tarefa abaixo é **atômica** — implemente, compile, teste antes de avançar.

---

## 📦 Pacotes de Referência

```
net.nicotfpn.alientech
├── machine/
│   └── core/
│       ├── AlienBlockEntity.java          ← Classe base (NÃO MODIFICAR sem necessidade)
│       ├── AlienMachineBlockEntity.java   ← Loop de tick (REFATORAR na Fase 0)
│       └── component/
│           ├── AlienComponent.java        ← Interface/Classe base de todos os componentes
│           ├── InventoryComponent.java    ← Existente
│           ├── EnergyComponent.java       ← Existente (MODIFICAR na Fase 2)
│           ├── EntropyComponent.java      ← Existente
│           ├── ProcessingComponent.java   ← Existente (MODIFICAR na Fase 2)
│           ├── TickableComponent.java     ← CRIAR na Fase 0
│           ├── SideConfigComponent.java   ← CRIAR na Fase 1
│           ├── AutoTransferComponent.java ← CRIAR na Fase 1
│           └── UpgradeComponent.java      ← CRIAR na Fase 2
├── util/
│   └── ScalingMath.java                  ← CRIAR na Fase 0
├── item/
│   └── data/
│       ├── ModDataComponents.java         ← CRIAR na Fase 0
│       └── UpgradeData.java               ← CRIAR na Fase 2
├── network/
│   ├── sideconfig/
│   │   ├── IOSideMode.java                ← CRIAR na Fase 1
│   │   ├── SidedItemHandlerWrapper.java   ← CRIAR na Fase 1
│   │   └── SidedEnergyStorageWrapper.java ← CRIAR na Fase 1
│   └── packet/
│       ├── ServerboundSideConfigPacket.java  ← CRIAR na Fase 1
│       └── ClientboundSideConfigPacket.java  ← CRIAR na Fase 1
├── upgrade/
│   └── UpgradeType.java                  ← CRIAR na Fase 2
└── events/
    └── CommonModEvents.java              ← MODIFICAR nas Fases 1 e 2
```

---

# ⚙️ FASE 0 — Foundation Hardening

> **Objetivo:** Instalar as fundações matemáticas, de threading e de contratos de dados.
> Nenhuma feature visível ao jogador é criada aqui.
> **Esta fase é pré-requisito absoluto para tudo que vem depois.**

---

## Tarefa 0.1 — Criar `ScalingMath.java`

**Arquivo:** `net.nicotfpn.alientech.util.ScalingMath`

Crie a classe utilitária **final** (sem instanciação) com as duas fórmulas canônicas.
**Esta é a ÚNICA fonte de escalonamento do mod. Não crie variantes em outros arquivos.**

```java
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
     * @param upgradeCount Quantidade de upgrades instalados (0–64). Negativo é tratado como 0.
     * @param factor       Intensidade do upgrade (ex: 0.5 para SPEED, 0.7 para OVERCLOCK).
     * @return             Valor escalado. Nunca menor que {@code base}.
     */
    public static long scale(long base, int upgradeCount, double factor) {
        if (upgradeCount <= 0) return base;
        double multiplier = 1.0 + (Math.log1p(upgradeCount) / Math.log(2)) * factor;
        return (long)(base * multiplier);
    }

    /**
     * Escala REDUZINDO um valor base de forma logarítmica.
     * Uso: reduzir custo de FE com ENERGY_EFFICIENCY, reduzir tempo com SPEED.
     *
     * Fórmula: result = base / (1 + (log2(1 + upgradeCount)) * factor)
     *
     * @param base         Valor base sem upgrades. Deve ser > 0.
     * @param upgradeCount Quantidade de upgrades instalados (0–64). Negativo é tratado como 0.
     * @param factor       Intensidade do upgrade.
     * @return             Valor escalado. Nunca menor que 1.
     */
    public static long scaleDown(long base, int upgradeCount, double factor) {
        if (upgradeCount <= 0) return base;
        double divisor = 1.0 + (Math.log1p(upgradeCount) / Math.log(2)) * factor;
        return Math.max(1L, (long)(base / divisor));
    }
}
```

**Tabela de referência para validação manual (base=1000, factor=0.5):**

| upgradeCount | scale() | scaleDown() |
|:---:|:---:|:---:|
| 0 | 1000 | 1000 |
| 1 | 1500 | 666 |
| 2 | 1793 | 557 |
| 4 | 2160 | 462 |
| 8 | 2500 | 400 |
| 16 | 2930 | 341 |

**Teste:** Escreva um teste unitário que valide os valores da tabela acima. Se qualquer valor diferir em mais de 1 (arredondamento de cast), a implementação está errada.

---

## Tarefa 0.2 — Criar `TickableComponent.java`

**Arquivo:** `net.nicotfpn.alientech.machine.core.component.TickableComponent`

Interface que todos os componentes que operam no `tickServer` devem implementar.

```java
package net.nicotfpn.alientech.machine.core.component;

/**
 * Contrato obrigatório para qualquer AlienComponent que executa lógica no server tick.
 *
 * REGRA DE TPS SAFETY:
 * isActive() deve executar em O(1) — NUNCA faça I/O de mundo ou lookup de BlockEntity aqui.
 * Apenas verifique estado interno do componente (flags, contagens, etc.).
 */
public interface TickableComponent {

    /**
     * Retorna true se e somente se este componente tem trabalho pendente neste tick.
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
```

---

## Tarefa 0.3 — Refatorar `AlienMachineBlockEntity.tickServer()`

**Arquivo:** `net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity`

Localize o método `tickServer()` e substitua o loop existente pelo contrato abaixo.

**ANTES (padrão problemático que pode existir):**
```java
// ❌ Não fazer isso — chama todo componente toda tick sem verificação
protected void tickServer() {
    for (AlienComponent component : components) {
        component.tick(this); // God loop sem guarda
    }
}
```

**DEPOIS (implementação correta):**
```java
/**
 * Loop principal do servidor. Executa apenas os componentes com trabalho pendente.
 *
 * IMPORTANTE: A ordem da lista activeComponents é significativa.
 * Ordem recomendada de registro no construtor da máquina:
 *   1. EnergyComponent (receber FE antes de processar)
 *   2. ProcessingComponent (consumir FE e processar)
 *   3. AutoTransferComponent (ejetar output após processamento)
 *   4. SideConfigComponent NÃO é tickable — apenas consultado pelos wrappers
 */
@Override
protected void tickServer() {
    for (AlienComponent component : activeComponents) {
        if (component instanceof TickableComponent tickable) {
            if (tickable.isActive()) {
                tickable.tick(this);
            }
        }
    }
}
```

**Adicionar método de registro de componentes no construtor:**
```java
/**
 * Registra um componente nesta máquina.
 * Deve ser chamado no construtor da subclasse, na ordem correta de execução.
 * Componentes TickableComponent são automaticamente adicionados ao loop de tick.
 *
 * @param component O componente a registrar. Não pode ser null.
 * @param <T>       Tipo do componente (deve estender AlienComponent).
 * @return O próprio componente, para encadeamento fluente no construtor.
 */
protected <T extends AlienComponent> T registerComponent(T component) {
    componentRegistry.put(component.getClass(), component);
    activeComponents.add(component);
    return component;
}

/**
 * Recupera um componente registrado por tipo.
 * Lança exceção se o componente não estiver registrado — falha explícita é preferível
 * a NullPointerException silenciosa.
 */
@SuppressWarnings("unchecked")
public <T extends AlienComponent> T getComponent(Class<T> type) {
    T component = (T) componentRegistry.get(type);
    if (component == null) {
        throw new IllegalStateException(
            "AlienTech: Componente " + type.getSimpleName() +
            " não registrado em " + this.getClass().getSimpleName() +
            " @ " + worldPosition
        );
    }
    return component;
}
```

---

## Tarefa 0.4 — Thread Safety Audit

**Arquivos a auditar:** Qualquer classe com `static` mutable state ou singletons.

Adicione este guard em **todo** método que muta estado global do servidor:

```java
private static void assertServerThread(Level level, BlockPos pos) {
    if (level.isClientSide()) {
        throw new IllegalStateException(
            "AlienTech: Mutação de estado global chamada na thread client! " +
            "Posição: " + pos + ". Stack: " + Thread.currentThread().getName()
        );
    }
}
```

**Locais onde este guard DEVE ser adicionado:**
- Qualquer método `onPlace` ou `onRemove` de bloco que muta cache global
- Todo método público do futuro `NetworkManager` (Fase 4)
- `SideConfigComponent.setMode()` (Fase 1)

---

## Tarefa 0.5 — Criar `ModDataComponents.java`

**Arquivo:** `net.nicotfpn.alientech.item.data.ModDataComponents`

Registra o `DeferredRegister` de `DataComponentType`. Nenhum componente concreto é registrado aqui ainda — isso ocorre na Fase 2. Mas o registro em si deve existir e ser inicializado no `AlienTech.java` (mod main class).

```java
package net.nicotfpn.alientech.item.data;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.nicotfpn.alientech.AlienTech;

public final class ModDataComponents {

    // DeferredRegister central para todos os DataComponentTypes do mod
    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
        DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
            AlienTech.MODID);

    // Componentes concretos serão registrados como campos estáticos aqui
    // nas fases seguintes (UpgradeData na Fase 2, PrisonData no futuro, etc.)

    private ModDataComponents() {}
}
```

**No `AlienTech.java` (mod main class), adicionar:**
```java
ModDataComponents.REGISTRY.register(modBus);
```

**Validação:** O mod deve carregar sem exceção com este registro vazio. Confirme no log do servidor.

---

# 🔄 FASE 1 — Side Configuration System (I/O)

> **Objetivo:** Permitir que cada face de cada máquina seja configurada independentemente
> para aceitar input, fornecer output, ou ser bloqueada, para Items, FE e Entropia.
> **Pré-requisito:** Fase 0 completamente implementada e compilando.

---

## Tarefa 1.1 — Criar `IOSideMode.java`

**Arquivo:** `net.nicotfpn.alientech.network.sideconfig.IOSideMode`

```java
package net.nicotfpn.alientech.network.sideconfig;

import net.minecraft.util.StringRepresentable;

public enum IOSideMode implements StringRepresentable {

    /** Face completamente bloqueada. Nenhuma capability é exposta. */
    NONE("none"),

    /** Aceita inserção externa (pipes podem empurrar para dentro). */
    INPUT("input"),

    /** Permite extração externa (pipes podem puxar para fora). */
    OUTPUT("output"),

    /**
     * INPUT + OUTPUT simultâneo.
     * ⚠️ RESTRIÇÃO: Válido SOMENTE para ItemHandler.
     * PROIBIDO para IEnergyStorage e IEntropyHandler.
     * Motivo: BOTH em energia cria loops de feedback de FE entre máquinas.
     * Esta restrição é validada em SideConfigComponent.setMode().
     */
    BOTH("both"),

    /**
     * A máquina empurra ATIVAMENTE para fora nesta face no tickServer.
     * Diferente de OUTPUT: OUTPUT é passivo (o pipe puxa).
     * PUSH é ativo (a máquina empurra independentemente de haver pipe).
     */
    PUSH("push"),

    /**
     * A máquina puxa ATIVAMENTE de fora nesta face no tickServer.
     * Diferente de INPUT: INPUT é passivo (o pipe empurra).
     * PULL é ativo (a máquina puxa independentemente de haver pipe).
     */
    PULL("pull");

    private final String serializedName;

    IOSideMode(String serializedName) {
        this.serializedName = serializedName;
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
            case BOTH -> PUSH;   // só acessível para ITEM
            case PUSH -> PULL;
            case PULL -> NONE;
        };
    }
}
```

---

## Tarefa 1.2 — Criar `CapabilityType.java`

**Arquivo:** `net.nicotfpn.alientech.network.sideconfig.CapabilityType`

Enum simples necessário para `IOSideMode.next()` e `SideConfigComponent`.

```java
package net.nicotfpn.alientech.network.sideconfig;

public enum CapabilityType {
    ITEM,
    ENERGY,
    ENTROPY
}
```

---

## Tarefa 1.3 — Criar `SideConfigComponent.java`

**Arquivo:** `net.nicotfpn.alientech.machine.core.component.SideConfigComponent`

Este é o componente central da Fase 1. Armazena e serializa a configuração de I/O de todas as faces.

```java
package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Componente responsável por armazenar e gerenciar a configuração de I/O
 * de cada face de uma máquina.
 *
 * NÃO implementa TickableComponent — é puramente consultado pelos wrappers
 * de capability e pelo AutoTransferComponent.
 */
public class SideConfigComponent extends AlienComponent {

    // Mapa: CapabilityType → (Direction → IOSideMode)
    // EnumMap para acesso O(1) sem boxing overhead
    private final Map<CapabilityType, Map<Direction, IOSideMode>> config = new EnumMap<>(CapabilityType.class);

    // Cache de faces ativas (PUSH ou PULL) para o AutoTransferComponent
    // Invalida quando setMode() é chamado
    private boolean activeTransferCacheDirty = true;
    private List<Direction> cachedActiveFaces = List.of();

    public SideConfigComponent() {
        // Inicializar todos os modos como NONE por padrão
        for (CapabilityType type : CapabilityType.values()) {
            Map<Direction, IOSideMode> faceMap = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                faceMap.put(direction, IOSideMode.NONE);
            }
            config.put(type, faceMap);
        }
    }

    /**
     * Retorna o modo configurado para uma face e tipo de capability.
     * O(1) — safe para chamar no tick (nos wrappers).
     */
    public IOSideMode getMode(Direction face, CapabilityType type) {
        return config.get(type).get(face);
    }

    /**
     * Define o modo de uma face para um tipo de capability.
     *
     * VALIDAÇÃO: BOTH é proibido para ENERGY e ENTROPY.
     * Esta validação é a linha de defesa contra loops de feedback energético.
     *
     * Após modificação, invalida o cache de faces ativas e notifica a BlockEntity
     * para reregistrar suas capabilities (invalidateCaps).
     *
     * @throws IllegalArgumentException se BOTH for aplicado a ENERGY ou ENTROPY.
     */
    public void setMode(Direction face, CapabilityType type, IOSideMode mode) {
        if (mode == IOSideMode.BOTH && type != CapabilityType.ITEM) {
            throw new IllegalArgumentException(
                "AlienTech: IOSideMode.BOTH é proibido para " + type +
                ". Apenas CapabilityType.ITEM aceita modo BOTH."
            );
        }
        config.get(type).put(face, mode);
        activeTransferCacheDirty = true;
        // A BlockEntity deve sobrescrever este método para chamar invalidateCaps()
        onModeChanged(face, type, mode);
    }

    /**
     * Hook chamado após cada mudança de modo.
     * A BlockEntity que hospedar este componente deve escutar este hook
     * para chamar blockEntity.invalidateCaps() e setChanged().
     */
    protected void onModeChanged(Direction face, CapabilityType type, IOSideMode newMode) {
        // Override na BlockEntity ou via lambda injetado no construtor
    }

    /**
     * Retorna se ao menos uma face tem modo PUSH ou PULL para qualquer tipo.
     * Usado pelo AutoTransferComponent.isActive() em O(1) (resultado cacheado).
     */
    public boolean hasAnyActiveTransfer() {
        if (activeTransferCacheDirty) {
            rebuildActiveCache();
        }
        return !cachedActiveFaces.isEmpty();
    }

    /**
     * Retorna as faces com modo PUSH ou PULL para ITEMS.
     * Resultado cacheado — apenas recalcula quando dirty.
     */
    public List<Direction> getActiveFaces() {
        if (activeTransferCacheDirty) {
            rebuildActiveCache();
        }
        return cachedActiveFaces;
    }

    private void rebuildActiveCache() {
        cachedActiveFaces = Direction.stream()
            .filter(d -> config.get(CapabilityType.ITEM).get(d).isActive())
            .toList();
        activeTransferCacheDirty = false;
    }

    // =========================================================================
    // SERIALIZAÇÃO NBT
    // =========================================================================

    /**
     * Estrutura NBT salva:
     * {
     *   "side_config": {
     *     "ITEM":    { "north": "input", "south": "output", ... },
     *     "ENERGY":  { "north": "none", ... },
     *     "ENTROPY": { ... }
     *   }
     * }
     */
    @Override
    public void saveToNBT(CompoundTag tag) {
        CompoundTag configTag = new CompoundTag();
        for (CapabilityType capType : CapabilityType.values()) {
            CompoundTag typeTag = new CompoundTag();
            for (Direction dir : Direction.values()) {
                typeTag.putString(dir.getSerializedName(),
                    config.get(capType).get(dir).getSerializedName());
            }
            configTag.put(capType.name(), typeTag);
        }
        tag.put("side_config", configTag);
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        if (!tag.contains("side_config")) return;
        CompoundTag configTag = tag.getCompound("side_config");
        for (CapabilityType capType : CapabilityType.values()) {
            if (!configTag.contains(capType.name())) continue;
            CompoundTag typeTag = configTag.getCompound(capType.name());
            for (Direction dir : Direction.values()) {
                String modeName = typeTag.getString(dir.getSerializedName());
                IOSideMode mode = IOSideMode.byName(modeName, IOSideMode.NONE);
                config.get(capType).put(dir, mode);
            }
        }
        activeTransferCacheDirty = true;
    }
}
```

> **NOTA PARA IMPLEMENTAÇÃO:** `IOSideMode.byName()` requer que `IOSideMode` implemente `StringRepresentable` com um método estático `byName`. Adicione isso ao enum da Tarefa 1.1 usando `StringRepresentable.fromEnum()`.

---

## Tarefa 1.4 — Criar `SidedItemHandlerWrapper.java`

**Arquivo:** `net.nicotfpn.alientech.network.sideconfig.SidedItemHandlerWrapper`

Wrapper lazy que proxeia operações de item através do `SideConfigComponent`.

```java
package net.nicotfpn.alientech.network.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;

/**
 * Proxy de IItemHandler que filtra operações baseado no SideConfigComponent.
 *
 * Instanciado UMA VEZ por direção no construtor da BlockEntity.
 * Não cria objetos intermediários no tick — todas as operações são O(1) delegações.
 */
public class SidedItemHandlerWrapper implements IItemHandler {

    private final InventoryComponent inventory;
    private final SideConfigComponent sideConfig;
    private final Direction face;

    public SidedItemHandlerWrapper(InventoryComponent inventory,
                                    SideConfigComponent sideConfig,
                                    Direction face) {
        this.inventory = inventory;
        this.sideConfig = sideConfig;
        this.face = face;
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ITEM);
        if (!mode.allowsInsertion()) {
            return stack; // Face bloqueada para inserção — retorna o item sem consumir
        }
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ITEM);
        if (!mode.allowsExtraction()) {
            return ItemStack.EMPTY; // Face bloqueada para extração
        }
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inventory.isItemValid(slot, stack);
    }
}
```

---

## Tarefa 1.5 — Criar `SidedEnergyStorageWrapper.java`

**Arquivo:** `net.nicotfpn.alientech.network.sideconfig.SidedEnergyStorageWrapper`

```java
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
 * Esta classe não precisa validar isso — SideConfigComponent.setMode() já impede BOTH.
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
        if (!mode.allowsInsertion()) return 0;
        return energy.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ENERGY);
        if (!mode.allowsExtraction()) return 0;
        return energy.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
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
```

---

## Tarefa 1.6 — Registrar Wrappers no `CommonModEvents.java`

**Arquivo:** `net.nicotfpn.alientech.events.CommonModEvents`

Localize o `RegisterCapabilitiesEvent` existente e adicione o registro dos wrappers sided.

```java
@SubscribeEvent
public static void registerCapabilities(RegisterCapabilitiesEvent event) {

    // Para cada BlockEntity que usa SideConfigComponent:
    event.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK,        // A capability do NeoForge
        ModBlockEntities.DECAY_CHAMBER.get(),  // O tipo da BlockEntity
        (blockEntity, direction) -> {
            if (!(blockEntity instanceof AlienMachineBlockEntity machine)) return null;
            if (direction == null) {
                // Acesso sem direção (ex: hoppers) → retorna inventory direto
                return machine.getComponent(InventoryComponent.class);
            }
            // Acesso com direção → retorna wrapper que filtra pelo SideConfig
            return machine.getSidedItemHandler(direction);
            // getSidedItemHandler() retorna o wrapper pré-instanciado para essa direção
        }
    );

    event.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK,
        ModBlockEntities.DECAY_CHAMBER.get(),
        (blockEntity, direction) -> {
            if (!(blockEntity instanceof AlienMachineBlockEntity machine)) return null;
            if (direction == null) return machine.getComponent(EnergyComponent.class);
            return machine.getSidedEnergyStorage(direction);
        }
    );

    // Repetir para cada BlockEntity que tem SideConfigComponent
}
```

**Na `AlienMachineBlockEntity`, adicionar os arrays de wrappers:**
```java
// Instanciados uma vez no construtor — nunca recriados
private final SidedItemHandlerWrapper[] sidedItemHandlers = new SidedItemHandlerWrapper[6];
private final SidedEnergyStorageWrapper[] sidedEnergyStorages = new SidedEnergyStorageWrapper[6];

// No construtor, após registrar os componentes:
protected void initSidedWrappers() {
    SideConfigComponent sideConfig = getComponent(SideConfigComponent.class);
    InventoryComponent inventory = getComponent(InventoryComponent.class);
    EnergyComponent energy = getComponent(EnergyComponent.class);
    for (Direction dir : Direction.values()) {
        sidedItemHandlers[dir.get3DDataValue()] =
            new SidedItemHandlerWrapper(inventory, sideConfig, dir);
        sidedEnergyStorages[dir.get3DDataValue()] =
            new SidedEnergyStorageWrapper(energy, sideConfig, dir);
    }
}

public SidedItemHandlerWrapper getSidedItemHandler(Direction dir) {
    return sidedItemHandlers[dir.get3DDataValue()];
}

public SidedEnergyStorageWrapper getSidedEnergyStorage(Direction dir) {
    return sidedEnergyStorages[dir.get3DDataValue()];
}
```

---

## Tarefa 1.7 — Criar `AutoTransferComponent.java`

**Arquivo:** `net.nicotfpn.alientech.machine.core.component.AutoTransferComponent`

```java
package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;

import java.util.List;

/**
 * Componente de transferência automática (AutoEject / AutoPull).
 *
 * Opera APENAS nas faces configuradas como PUSH ou PULL.
 * NUNCA itera sobre Direction.values() cegamente — usa o cache do SideConfigComponent.
 *
 * Cache de neighbors: BlockEntity vizinho é resolvido uma vez e cacheado por face.
 * Cache é invalidado em onNeighborChanged da BlockEntity dona.
 */
public class AutoTransferComponent extends AlienComponent implements TickableComponent {

    // Cache de handlers vizinhos por face (index = Direction.get3DDataValue())
    // null = não resolvido ainda; EMPTY_SENTINEL = vizinho sem ItemHandler
    private final IItemHandler[] neighborCache = new IItemHandler[6];
    private boolean neighborCacheDirty = true;

    @Override
    public boolean isActive() {
        // O(1) — consulta cache do SideConfigComponent sem percorrer directions
        SideConfigComponent sideConfig = null;
        try {
            // Obtido via referência injetada (ver construtor abaixo)
            sideConfig = this.sideConfig;
        } catch (Exception e) {
            return false;
        }
        return sideConfig != null && sideConfig.hasAnyActiveTransfer();
    }

    @Override
    public void tick(AlienMachineBlockEntity machine) {
        SideConfigComponent sideConfig = machine.getComponent(SideConfigComponent.class);
        InventoryComponent inventory = machine.getComponent(InventoryComponent.class);

        // Apenas faces ativas (PUSH ou PULL) — lista pré-computada, não Direction.values()
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
     * Empurra itens do inventário desta máquina para o inventário adjacente na face.
     * Resolve o neighbor uma vez por invalidação de cache.
     */
    private void performEject(AlienMachineBlockEntity machine,
                               InventoryComponent inventory,
                               Direction face) {
        IItemHandler neighbor = resolveNeighbor(machine, face);
        if (neighbor == null) return;

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.extractItem(slot, 64, true); // simulate
            if (stack.isEmpty()) continue;

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(neighbor, stack, false);
            if (remainder.getCount() != stack.getCount()) {
                // Algo foi inserido — confirmar extração
                inventory.extractItem(slot, stack.getCount() - remainder.getCount(), false);
                break; // Um slot por tick para distribuir a carga de TPS
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
        if (neighbor == null) return;

        for (int slot = 0; slot < neighbor.getSlots(); slot++) {
            ItemStack stack = neighbor.extractItem(slot, 64, true); // simulate
            if (stack.isEmpty()) continue;

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                inventory, stack, false);
            if (remainder.getCount() != stack.getCount()) {
                neighbor.extractItem(slot, stack.getCount() - remainder.getCount(), false);
                break;
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
            neighborCache[idx] = machine.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, neighborPos, face.getOpposite()
            );
            // nota: pode ser null se vizinho não tem ItemHandler — isso é válido
        }
        return neighborCache[idx];
    }

    /** Chamado pela BlockEntity em onNeighborChanged para invalidar o cache. */
    public void invalidateNeighborCache() {
        neighborCacheDirty = true;
        // Não limpar o array — apenas marcar como dirty para lazy reload
        java.util.Arrays.fill(neighborCache, null);
    }

    // Referência ao SideConfig injetada para isActive() sem acessar machine
    private SideConfigComponent sideConfig;

    public void injectSideConfig(SideConfigComponent sideConfig) {
        this.sideConfig = sideConfig;
    }
}
```

**Na BlockEntity, após construir os componentes:**
```java
AutoTransferComponent autoTransfer = registerComponent(new AutoTransferComponent());
autoTransfer.injectSideConfig(getComponent(SideConfigComponent.class));
```

**Também na BlockEntity, sobrescrever:**
```java
@Override
public void onNeighborChanged(BlockPos neighborPos, Block block) {
    super.onNeighborChanged(neighborPos, block);
    if (!level.isClientSide()) {
        getComponent(AutoTransferComponent.class).invalidateNeighborCache();
    }
}
```

---

## Tarefa 1.8 — Sincronização GUI (Packets)

### `ServerboundSideConfigPacket.java`

Enviado pelo client quando o jogador clica em uma face na GUI.

```java
// Campos: BlockPos machinePos, Direction face, CapabilityType capType, IOSideMode newMode
// Ao receber no servidor:
//   1. Verificar que o jogador está a <= 8 blocos da máquina
//   2. Verificar que a máquina existe no pos
//   3. Chamar machine.getComponent(SideConfigComponent.class).setMode(face, capType, newMode)
//   4. Chamar machine.setChanged()
//   5. Enviar ClientboundSideConfigPacket de confirmação
```

### `ClientboundSideConfigPacket.java`

Enviado pelo servidor para sincronizar o estado após mudança.

```java
// Campos: BlockPos machinePos, CompoundTag fullSideConfigNBT
// Ao receber no cliente:
//   1. Localizar a BlockEntity no machinePos
//   2. Chamar machine.getComponent(SideConfigComponent.class).loadFromNBT(tag)
//   3. Invalidar o render da tela se aberta
```

> **Registrar ambos os packets** no `AlienTech.java` via `PayloadRegistrar` do NeoForge 1.21.

---

## Tarefa 1.9 — Aba GUI de Side Config

**Regras de implementação da UI:**

1. A aba `SideConfigTab` renderiza um **cubo isométrico 2D simplificado** com 6 botões de face.
2. Cada botão exibe um ícone colorido representando o `IOSideMode` atual:
   - `NONE` → cinza (🔒)
   - `INPUT` → azul (⬇)
   - `OUTPUT` → laranja (⬆)
   - `BOTH` → verde (↕) — só visível para ITEM
   - `PUSH` → vermelho (▶▶)
   - `PULL` → roxo (◀◀)
3. **Click esquerdo** no botão → chama `IOSideMode.next(capType)` e envia `ServerboundSideConfigPacket`.
4. **Click direito** no botão → reseta para `NONE` e envia packet.
5. Um seletor de `CapabilityType` (ITEM / ENERGY / ENTROPY) permite alternar qual capability está sendo configurada.
6. **Sem sync por polling** — a tela atualiza apenas ao receber `ClientboundSideConfigPacket`.

---

# ⬆️ FASE 2 — Universal Upgrade System

> **Objetivo:** Criar um sistema de upgrades modulares onde cada tipo de upgrade
> altera dinamicamente os custos e velocidades dos componentes via `ScalingMath`.
> **Pré-requisito:** Fase 0 e Fase 1 completamente implementadas.

---

## Tarefa 2.1 — Criar `UpgradeType.java`

**Arquivo:** `net.nicotfpn.alientech.upgrade.UpgradeType`

```java
package net.nicotfpn.alientech.upgrade;

import net.minecraft.util.StringRepresentable;

public enum UpgradeType implements StringRepresentable {

    /**
     * Reduz o tempo de processamento.
     * Aplica ScalingMath.scaleDown() no maxProgress do ProcessingComponent.
     * NÃO aumenta custo de FE (use OVERCLOCK para isso).
     */
    SPEED("speed", 0.5, 8),

    /**
     * Reduz o consumo de FE por operação.
     * Aplica ScalingMath.scaleDown() no fePerTick do EnergyComponent.
     * Como bônus, também aumenta a capacidade do buffer de FE em 30%.
     */
    ENERGY_EFFICIENCY("energy_efficiency", 0.4, 8),

    /**
     * Reduz o consumo de Entropia por operação.
     * Aplica ScalingMath.scaleDown() no entropyPerOperation do EntropyComponent.
     */
    ENTROPY_EFFICIENCY("entropy_efficiency", 0.4, 8),

    /**
     * Silencia os sons de operação da máquina. Efeito CLIENT-SIDE ONLY.
     * O servidor apenas armazena a quantidade instalada.
     * A Screen consulta este valor e suprime o SoundEvent de operação.
     * scaleFactor = 0.0 pois não há cálculo matemático envolvido.
     */
    MUFFLING("muffling", 0.0, 4),

    /**
     * Aumenta velocidade E custo de FE simultaneamente.
     * Aplica ScalingMath.scaleDown() no maxProgress (mais rápido).
     * Aplica ScalingMath.scale() no fePerTick (mais caro).
     * Projetado para jogadores que querem throughput máximo e têm FE de sobra.
     */
    OVERCLOCK("overclock", 0.7, 4);

    /** Fator de intensidade para ScalingMath. Ver comentário de cada tipo. */
    public final double scaleFactor;

    /** Quantidade máxima permitida por máquina. */
    public final int maxStack;

    private final String serializedName;

    UpgradeType(String serializedName, double scaleFactor, int maxStack) {
        this.serializedName = serializedName;
        this.scaleFactor = scaleFactor;
        this.maxStack = maxStack;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static final com.mojang.serialization.Codec<UpgradeType> CODEC =
        StringRepresentable.fromEnum(UpgradeType::values);
}
```

---

## Tarefa 2.2 — Criar `UpgradeData.java` e Registrar DataComponent

### `UpgradeData.java`

**Arquivo:** `net.nicotfpn.alientech.item.data.UpgradeData`

Record imutável. Este é o tipo armazenado no `DataComponentType` do item de upgrade.

```java
package net.nicotfpn.alientech.item.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.nicotfpn.alientech.upgrade.UpgradeType;

/**
 * Dado imutável armazenado em um ItemStack de upgrade via DataComponent.
 *
 * PROIBIDO: Usar .getTag() ou .setTag() para ler/escrever UpgradeType em um item.
 * CORRETO: itemStack.get(ModDataComponents.UPGRADE_DATA.get()) retorna este record.
 */
public record UpgradeData(UpgradeType type) {

    public static final Codec<UpgradeData> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            UpgradeType.CODEC.fieldOf("type").forGetter(UpgradeData::type)
        ).apply(inst, UpgradeData::new)
    );

    public static final StreamCodec<ByteBuf, UpgradeData> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(
            s -> new UpgradeData(UpgradeType.CODEC.parse(
                    com.mojang.serialization.JsonOps.INSTANCE,
                    new com.google.gson.JsonPrimitive(s))
                .result().orElseThrow()),
            d -> d.type().getSerializedName()
        );
}
```

### Registrar em `ModDataComponents.java`

Adicionar o campo ao arquivo criado na Fase 0:

```java
public static final Supplier<DataComponentType<UpgradeData>> UPGRADE_DATA =
    REGISTRY.register("upgrade_data", () ->
        DataComponentType.<UpgradeData>builder()
            .persistent(UpgradeData.CODEC)
            .networkSynchronized(UpgradeData.STREAM_CODEC)
            .build()
    );
```

---

## Tarefa 2.3 — Criar `UpgradeComponent.java`

**Arquivo:** `net.nicotfpn.alientech.machine.core.component.UpgradeComponent`

```java
package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.upgrade.UpgradeType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Componente que armazena os upgrades instalados em uma máquina.
 *
 * NÃO implementa TickableComponent — upgrades são consultados por outros
 * componentes como efeito passivo, não processados ativamente no tick.
 *
 * Quando upgrades mudam, dispara onUpgradesChanged() na máquina para
 * invalidar os caches de custo calculado em ProcessingComponent e EnergyComponent.
 */
public class UpgradeComponent extends AlienComponent {

    private final Map<UpgradeType, Integer> installedUpgrades = new EnumMap<>(UpgradeType.class);

    // Listener chamado quando upgrades mudam (injetado pela BlockEntity)
    private Runnable onChangeListener;

    public void setOnChangeListener(Runnable listener) {
        this.onChangeListener = listener;
    }

    /**
     * Retorna a quantidade instalada de um tipo de upgrade.
     * Retorna 0 se nenhum upgrade deste tipo estiver instalado.
     * O(1) — safe para chamar nos métodos de custo calculado.
     */
    public int getCount(UpgradeType type) {
        return installedUpgrades.getOrDefault(type, 0);
    }

    /**
     * Define a quantidade instalada de um tipo de upgrade.
     * Valida contra o maxStack definido no enum.
     *
     * @throws IllegalArgumentException se count exceder UpgradeType.maxStack.
     */
    public void setCount(UpgradeType type, int count) {
        if (count < 0 || count > type.maxStack) {
            throw new IllegalArgumentException(
                "AlienTech: Contagem inválida de upgrade " + type + ": " + count +
                " (máximo: " + type.maxStack + ")"
            );
        }
        if (count == 0) {
            installedUpgrades.remove(type);
        } else {
            installedUpgrades.put(type, count);
        }
        if (onChangeListener != null) {
            onChangeListener.run(); // Invalida caches de custo
        }
    }

    /** Retorna true se pelo menos 1 upgrade de MUFFLING estiver instalado. */
    public boolean isMuffled() {
        return getCount(UpgradeType.MUFFLING) > 0;
    }

    // =========================================================================
    // SERIALIZAÇÃO NBT
    // =========================================================================

    @Override
    public void saveToNBT(CompoundTag tag) {
        CompoundTag upgradeTag = new CompoundTag();
        installedUpgrades.forEach((type, count) ->
            upgradeTag.putInt(type.getSerializedName(), count)
        );
        tag.put("upgrades", upgradeTag);
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        installedUpgrades.clear();
        if (!tag.contains("upgrades")) return;
        CompoundTag upgradeTag = tag.getCompound("upgrades");
        for (UpgradeType type : UpgradeType.values()) {
            if (upgradeTag.contains(type.getSerializedName())) {
                int count = upgradeTag.getInt(type.getSerializedName());
                if (count > 0 && count <= type.maxStack) {
                    installedUpgrades.put(type, count);
                }
            }
        }
    }
}
```

**Na BlockEntity dona, após registrar o componente:**
```java
UpgradeComponent upgrades = registerComponent(new UpgradeComponent());
upgrades.setOnChangeListener(this::onUpgradesChanged);
```

```java
/** Invalida os caches de custo calculado em todos os componentes. */
private void onUpgradesChanged() {
    getComponent(ProcessingComponent.class).invalidateCostCache();
    getComponent(EnergyComponent.class).invalidateCostCache();
    if (hasComponent(EntropyComponent.class)) {
        getComponent(EntropyComponent.class).invalidateCostCache();
    }
    setChanged();
}
```

---

## Tarefa 2.4 — Adaptar `ProcessingComponent.java`

Adicionar cache de custo calculado com dirty flag. Os métodos de custo raw permanecem, mas toda lógica de tick usa os métodos calculados.

```java
// Adicionar campos de cache:
private boolean costCacheDirty = true;
private int cachedMaxProgress;
private long cachedFEPerTick;

/** Invalidado por UpgradeComponent.onChangeListener via onUpgradesChanged(). */
public void invalidateCostCache() {
    costCacheDirty = true;
}

/**
 * Retorna o maxProgress efetivo considerando upgrades SPEED e OVERCLOCK.
 * Resultado cacheado — recalcula APENAS quando invalidateCostCache() for chamado.
 * NUNCA chame ScalingMath dentro do tick sem este cache.
 */
public int getEffectiveMaxProgress(AlienMachineBlockEntity machine) {
    if (costCacheDirty) recalculateCostCache(machine);
    return cachedMaxProgress;
}

public long getEffectiveFEPerTick(AlienMachineBlockEntity machine) {
    if (costCacheDirty) recalculateCostCache(machine);
    return cachedFEPerTick;
}

private void recalculateCostCache(AlienMachineBlockEntity machine) {
    UpgradeComponent upgrades = machine.getComponent(UpgradeComponent.class);

    int speedCount     = upgrades.getCount(UpgradeType.SPEED);
    int overclockCount = upgrades.getCount(UpgradeType.OVERCLOCK);
    int effCount       = upgrades.getCount(UpgradeType.ENERGY_EFFICIENCY);

    // maxProgress: SPEED reduz tempo → scaleDown. OVERCLOCK também reduz tempo.
    long progress = baseMaxProgress;
    progress = ScalingMath.scaleDown(progress, speedCount,     UpgradeType.SPEED.scaleFactor);
    progress = ScalingMath.scaleDown(progress, overclockCount, UpgradeType.OVERCLOCK.scaleFactor);
    cachedMaxProgress = (int) Math.max(1L, progress);

    // fePerTick: EFFICIENCY reduz custo. OVERCLOCK AUMENTA custo.
    long fe = baseFEPerTick;
    fe = ScalingMath.scaleDown(fe, effCount,       UpgradeType.ENERGY_EFFICIENCY.scaleFactor);
    fe = ScalingMath.scale    (fe, overclockCount, UpgradeType.OVERCLOCK.scaleFactor);
    cachedFEPerTick = fe;

    costCacheDirty = false;
}
```

---

## Tarefa 2.5 — Adaptar `EnergyComponent.java`

Adicionar o mesmo padrão de cache:

```java
private boolean capacityCacheDirty = true;
private long cachedMaxCapacity;

public void invalidateCostCache() {
    capacityCacheDirty = true;
}

/**
 * Capacidade máxima do buffer de FE.
 * Upgrades de ENERGY_EFFICIENCY aumentam a capacidade do buffer (fator 0.3).
 * Resultado cacheado — recalcula apenas quando upgrades mudam.
 */
public long getEffectiveMaxCapacity(AlienMachineBlockEntity machine) {
    if (capacityCacheDirty) {
        int effCount = machine.getComponent(UpgradeComponent.class)
            .getCount(UpgradeType.ENERGY_EFFICIENCY);
        cachedMaxCapacity = ScalingMath.scale(baseCapacity, effCount, 0.3);
        capacityCacheDirty = false;
    }
    return cachedMaxCapacity;
}
```

---

## Tarefa 2.6 — Efeito MUFFLING (Client-Side)

**Onde implementar:** Na `Screen` da máquina (classe que estende `AbstractContainerScreen`).

```java
// No método que toca o som de operação da máquina (ex: chamado a cada N ticks):
private void playOperationSound() {
    // Verificar MUFFLING antes de tocar o som
    // O valor de isMuffled é sincronizado via SyncableLong (ou campo booleano sync)
    if (isMuffledSync.get() > 0) {
        return; // MUFFLING ativo — suprimir som sem nenhuma outra lógica
    }
    Minecraft.getInstance().getSoundManager().play(
        SimpleSoundInstance.forUI(ModSounds.MACHINE_HUM.get(), 1.0f)
    );
}
```

**Sincronização do flag de MUFFLING:**
- Adicionar um `SyncableLong` no `AlienContainerMenu` da máquina com supplier:
  `() -> (long) machine.getComponent(UpgradeComponent.class).getCount(UpgradeType.MUFFLING)`
- A Screen lê este valor. **O servidor nunca processa MUFFLING — apenas armazena a contagem.**

---

## Tarefa 2.7 — Slots de Upgrade na GUI

**No `AlienContainerMenu` da máquina:**

```java
// Adicionar slots de upgrade ao container.
// A quantidade de slots vem do MachineTier (Fase 3). Por ora, usar 4 como padrão.
private static final int UPGRADE_SLOT_COUNT = 4;

// Registrar slots de upgrade no InventoryComponent com flag especial:
for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
    int slotIndex = baseInventorySize + i; // Após os slots normais
    addSlot(new AlienUpgradeSlot(inventory, slotIndex, x, y + i * 18));
}
```

**`AlienUpgradeSlot.java`** — slot que só aceita itens com `UPGRADE_DATA` DataComponent:

```java
public class AlienUpgradeSlot extends Slot {

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Verificar se o item tem DataComponent de upgrade registrado
        return stack.has(ModDataComponents.UPGRADE_DATA.get());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // Sincronizar UpgradeComponent com os itens nos slots
        syncUpgradesToComponent();
    }

    private void syncUpgradesToComponent() {
        // Percorrer slots de upgrade, ler UpgradeData de cada item,
        // chamar upgradeComponent.setCount() com a contagem agregada por tipo
    }
}
```

---

## ✅ Checklist Final de Validação (Fases 0–2)

```
FASE 0:
[ ] ScalingMath.scale(1000, 8, 0.5) retorna 2500 (±1 de arredondamento)
[ ] ScalingMath.scaleDown(1000, 8, 0.5) retorna 400 (±1)
[ ] TickableComponent.isActive() retorna false em máquina sem items ou sem faces ativas
[ ] tickServer() NÃO chama componentes com isActive() == false
[ ] ModDataComponents.REGISTRY registrado no mod bus sem exceção
[ ] Mutação de cache em nível server lança ISE se chamada no client

FASE 1:
[ ] IOSideMode.BOTH lança IllegalArgumentException quando aplicado a ENERGY ou ENTROPY
[ ] SidedItemHandlerWrapper.insertItem retorna stack intacto em face NONE ou OUTPUT
[ ] SidedEnergyStorageWrapper.receiveEnergy retorna 0 em face NONE ou OUTPUT
[ ] AutoTransferComponent.isActive() retorna false quando nenhuma face é PUSH/PULL
[ ] AutoTransferComponent.tick NÃO é chamado quando isActive() == false
[ ] invalidateNeighborCache() é chamado em onNeighborChanged da BlockEntity
[ ] SideConfigComponent serializa e deserializa corretamente (round-trip NBT)
[ ] Packet de configuração valida distância do jogador no servidor
[ ] GUI exibe ícones corretos por modo e reseta em click direito

FASE 2:
[ ] UpgradeComponent.setCount(SPEED, 9) lança IAE (maxStack = 8)
[ ] ProcessingComponent.getEffectiveMaxProgress NÃO chama ScalingMath quando cache é válido
[ ] invalidateCostCache() é chamado em onUpgradesChanged
[ ] onUpgradesChanged é disparado ao inserir/remover item de slot de upgrade
[ ] MUFFLING com count >= 1 suprime SoundEvent na Screen (testável via F3)
[ ] Item de upgrade com UpgradeData DataComponent inserível no slot de upgrade
[ ] Item SEM UpgradeData é rejeitado pelo AlienUpgradeSlot.mayPlace()
[ ] OVERCLOCK com 4 upgrades: maxProgress reduz E fePerTick aumenta (não apenas velocidade)
```

---

## 🚫 Anti-Patterns Específicos Destas Fases

| ❌ Proibido | ✅ Correto | Fase |
|---|---|:---:|
| `for (Direction d : Direction.values()) { if (mode == PUSH)... }` no tick | `sideConfig.getActiveFaces()` (lista pré-filtrada) | 1 |
| `level.getBlockEntity(neighborPos)` cacheado fora de `invalidateNeighborCache()` | `resolveNeighbor()` com lazy cache por dirty flag | 1 |
| `itemStack.getTag().getString("upgrade_type")` | `itemStack.get(ModDataComponents.UPGRADE_DATA.get()).type()` | 2 |
| `fePerTick * speedUpgrades * 0.5` | `ScalingMath.scaleDown(fePerTick, speedUpgrades, factor)` | 2 |
| Recalcular `ScalingMath` a cada tick | Calcular uma vez, cachear, invalidar com `dirty flag` | 2 |
| Processar `MUFFLING` no server tick | Ler `isMuffled` apenas na Screen, server só armazena contagem | 2 |
| `SideConfigComponent` implementando `TickableComponent` | `SideConfigComponent` é consultado — não tickado | 1 |

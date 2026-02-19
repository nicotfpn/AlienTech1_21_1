# Contexto Completo do Mod AlienTech para Minecraft NeoForge 1.21.1

## 📋 Índice
1. [Visão Geral e Filosofia](#visão-geral-e-filosofia)
2. [Arquitetura Técnica](#arquitetura-técnica)
3. [Sistemas Principais](#sistemas-principais)
4. [Fluxo de Gameplay](#fluxo-de-gameplay)
5. [Padrões de Código](#padrões-de-código)
6. [Como Contribuir](#como-contribuir)

---

## 🎯 Visão Geral e Filosofia

### Conceito Central: Entropia e Decaimento

**AlienTech** é um mod de tecnologia/magia baseado no conceito de **Entropia** — a energia caótica liberada pelo decaimento biológico. Ao invés de minerar minérios para energia, os jogadores:

1. **Capturam** entidades vivas em Prisões Dimensionais de Bolso
2. **Aceleram** o decaimento dessas entidades em uma Câmara de Decaimento
3. **Colhem** a energia de entropia resultante (Entropy Biomass)
4. **Refinam** a biomassa em um Reservatório de Entropia para produzir Grávitons Decadentes
5. **Geram** energia Forge Energy (FE) queimando grávitons em uma Turbina de Vácuo Quântico
6. **Amplificam** a geração com Pirâmides Alienígenas
7. **Evoluem** usando entropia para ganhar habilidades especiais

### Progression Pipeline Completo

```
Mob Vivo
    ↓
[Pocket Dimensional Prison] → Captura entidade
    ↓
[Decay Chamber] → Processa mob → Gera Entropy Biomass
    ↓
[Entropy Reservoir] → Refina biomassa + FE → Produz Decaying Graviton
    ↓
[Quantum Vacuum Turbine] → Queima graviton → Gera FE (energia)
    ↓
[Alien Pyramid] → Amplifica geração de FE (opcional)
    ↓
[Entropy Cables] → Transporta entropia entre máquinas
    ↓
[Evolution Chamber] → Consome entropia → Evolui jogador
    ↓
[Player Evolution] → Desbloqueia habilidades baseadas em entropia
```

---

## 🏗️ Arquitetura Técnica

### Stack Tecnológico

- **Minecraft Version**: 1.21.1
- **Mod Loader**: NeoForge
- **Linguagem**: Java
- **Padrão de Arquitetura**: Component-based, modular, production-grade

### Estrutura de Diretórios

```
src/main/java/net/nicotfpn/alientech/
├── block/              # Blocos customizados e BlockEntities
│   ├── custom/         # Blocos com comportamento especial
│   └── entity/         # BlockEntities (tile entities)
├── item/               # Itens customizados e Creative Tabs
│   └── custom/         # Itens com comportamento especial
├── entropy/            # Sistema de Entropia (energia customizada)
│   ├── IEntropyHandler.java      # Interface de entropia
│   ├── EntropyStorage.java       # Armazenamento de entropia
│   ├── EntropyTransaction.java   # Transferências atômicas
│   └── ModCapabilities.java      # Registro de capabilities
├── evolution/          # Sistema de Evolução do Jogador
│   ├── IEvolutionData.java       # Interface de dados de evolução
│   ├── PlayerEvolutionData.java # Dados persistentes do jogador
│   ├── PlayerEvolutionHelper.java # API de acesso fácil
│   ├── ModAttachments.java      # Registro de attachments NeoForge
│   └── ability/                  # Sistema de habilidades
│       ├── IEvolutionAbility.java
│       ├── BaseEvolutionAbility.java
│       ├── AbilityRegistry.java
│       └── impl/                 # Implementações de habilidades
├── machine/            # Máquinas do mod
│   ├── core/           # Framework modular de máquinas
│   │   ├── MachineInventory.java
│   │   ├── MachineEnergy.java
│   │   ├── MachineProcessor.java
│   │   ├── MachineAutomation.java
│   │   └── MachineCapabilities.java
│   ├── decay/          # Câmara de Decaimento
│   ├── entropy/        # Reservatório de Entropia
│   ├── turbine/        # Turbina de Vácuo Quântico
│   └── evolution/       # Câmara de Evolução
├── pyramid/            # Sistema de validação de pirâmides
├── util/               # Utilitários e helpers
│   ├── CapabilityUtils.java     # Acesso seguro a capabilities
│   ├── SafeNBT.java             # Leitura segura de NBT
│   ├── StateValidator.java      # Validação e clamping
│   ├── AlienTechDebug.java     # Sistema de debug condicional
│   └── EntityStorageUtil.java  # Armazenamento de entidades
├── network/            # Sistema de networking
├── event/               # Event handlers
├── screen/              # GUIs (futuro)
└── Config.java          # Configurações do mod
```

---

## 🔧 Sistemas Principais

### 1. Sistema de Entropia

#### Conceito
A entropia é uma **energia customizada** separada do Forge Energy (FE). Ela representa energia biológica caótica extraída de organismos vivos.

#### Componentes Principais

**IEntropyHandler** (Interface)
```java
// Interface similar a IEnergyStorage, mas para entropia
int getEntropy()
int getMaxEntropy()
int insertEntropy(int amount, boolean simulate)
int extractEntropy(int amount, boolean simulate)
boolean canInsert()
boolean canExtract()
```

**EntropyStorage** (Implementação)
- Armazena entropia com capacidade configurável
- Limites de inserção/extração por operação
- Validação automática de estado (nunca negativo, nunca excede capacidade)
- Callback `onChanged` para marcação de dirty/sync

**EntropyTransaction** (Transferências Atômicas)
- Garante transferências seguras: simula → executa
- Previne duplicação ou perda de entropia
- Padrão: simulate primeiro, commit depois

**ModCapabilities** (Registro)
- Registra `ENTROPY` como BlockCapability NeoForge
- Permite acesso via `level.getCapability(ModCapabilities.ENTROPY, pos, side)`

#### Máquinas que Usam Entropia

1. **Decay Chamber Controller**
   - **Produz** entropia (não consome)
   - Output: Entropy Biomass items + EntropyStorage interno

2. **Entropy Reservoir**
   - **Consome** entropia (via biomassa)
   - **Produz** Decaying Graviton items
   - Usa FE para processar

3. **Entropy Cable**
   - **Transporta** entropia entre máquinas
   - Stateless (não armazena)
   - Usa EntropyTransaction para transferências seguras

4. **Evolution Chamber**
   - **Consome** entropia de vizinhos
   - Evolui jogadores para estágios superiores

### 2. Sistema de Evolução do Jogador

#### Conceito
Jogadores podem evoluir para estágios superiores, ganhando capacidade de entropia e desbloqueando habilidades.

#### Componentes

**PlayerEvolutionData** (Attachment NeoForge)
- Persiste entre saves e morte (`copyOnDeath`)
- Armazena:
  - `evolutionStage` (0 = baseline, máximo configurável)
  - `entropyCapacity` (capacidade de armazenar entropia)
  - `storedEntropy` (entropia atual armazenada)

**PlayerEvolutionHelper** (API de Acesso)
```java
PlayerEvolutionData data = PlayerEvolutionHelper.get(player);
data.insertEntropy(100);
data.setEvolutionStage(2);
```

**ModAttachments** (Registro)
- Registra attachment usando sistema NeoForge
- Serialização/deserialização automática
- Persiste em NBT

#### Como Aumentar Evolução

**ÚNICA FORMA**: Usar a **Evolution Chamber**
- Jogador fica em cima do bloco
- Máquina consome entropia gradualmente
- Após processamento completo, aumenta `evolutionStage`

### 3. Sistema de Habilidades

#### Conceito
Habilidades são poderes especiais que jogadores podem ativar consumindo entropia armazenada.

#### Arquitetura

**IEvolutionAbility** (Interface)
- Define contrato de habilidade
- Requisitos: stage mínimo, custo de entropia, cooldown

**BaseEvolutionAbility** (Classe Base)
- Implementa lógica comum:
  - Verificação de pré-requisitos
  - Consumo de entropia
  - Sistema de cooldown thread-safe
  - Limpeza automática de memória

**AbilityRegistry** (Registro Central)
- Mantém todas as habilidades registradas
- Lookup por ID ou ResourceLocation
- Filtragem por stage disponível

**Habilidades Implementadas**

1. **Entropy Shield** (Stage 1)
   - Custo: 200 entropia
   - Efeito: Resistência II por 10 segundos

2. **Decay Vision** (Stage 1)
   - Custo: 150 entropia
   - Efeito: Visão noturna + revela entidades próximas

3. **Gravitational Pull** (Stage 2)
   - Custo: 300 entropia
   - Efeito: Puxa entidades próximas em direção ao jogador

4. **Entropy Burst** (Stage 3)
   - Custo: 500 entropia
   - Efeito: Explosão de entropia que causa dano

#### Ativação de Habilidades

**Client-Side**:
- Keybinds: V, B, N, M (slots 1-4)
- Envia `AbilityActivationPacket` para servidor

**Server-Side**:
- Valida tudo server-side (nunca confia no cliente)
- Verifica stage, entropia, cooldown
- Consome entropia atomicamente
- Aplica efeito

### 4. Framework de Máquinas

#### Conceito
Framework modular que previne duplicação de código e garante consistência.

#### Componentes do Framework

**AbstractMachineBlockEntity** (Classe Base)
- Compõe módulos independentes:
  - `MachineInventory` — gerenciamento de slots
  - `MachineEnergy` — armazenamento FE
  - `MachineProcessor` — lógica de processamento
  - `MachineAutomation` — auto-push/pull
  - `MachineTicker` — orquestração de ticks

**Padrão de Uso**:
```java
public class MyMachine extends AbstractMachineBlockEntity {
    // Define slot layout
    // Implementa IMachineProcess (lógica de receita)
    // Implementa SlotAccessRules (regras de acesso)
    // Framework cuida do resto
}
```

### 5. Sistema de Multiblocks

#### Decay Chamber
- Estrutura: Cubo 3x3x3 oco
- Validação: `DecayChamberStructure.isValid()`
- Controller: `DecayChamberControllerBlockEntity`

#### Alien Pyramid
- Estrutura: Pirâmide (Casing → Gold → Core)
- Validação: `PyramidStructureValidator`
- Tiers:
  - Tier 1: Base 5x5 (Scan Range: 32)
  - Tier 2: Base 7x7 (Scan Range: 48)
  - Tier 3: Base 9x9 (Scan Range: 64)
- Função: Amplifica geração de FE de Turbinas próximas

### 6. Sistema de Segurança e Validação

#### CapabilityUtils
Acesso seguro a capabilities:
```java
IEntropyHandler handler = CapabilityUtils.safeGetEntropyHandler(level, pos, side);
// Sempre retorna null se inválido, nunca lança exceção
```

#### SafeNBT
Leitura segura de NBT com defaults:
```java
int value = SafeNBT.getInt(tag, "Key", defaultValue);
// Nunca lança exceção, sempre retorna valor válido
```

#### StateValidator
Clamping seguro de valores:
```java
entropy = StateValidator.clampEntropy(value, capacity);
// Garante valor sempre em [0, capacity]
```

#### AlienTechDebug
Logging condicional (desabilitado por padrão):
```java
if (AlienTechDebug.ENTROPY.isEnabled()) {
    AlienTechDebug.ENTROPY.log("Transfer: {} -> {}", source, dest);
}
```

#### Validação de Estado
Todos os BlockEntities implementam `validateState()`:
- Chamado após mutações críticas
- Chamado após load de NBT
- Corrige valores inválidos automaticamente
- Loga correções quando debug habilitado

---

## 🎮 Fluxo de Gameplay

### Fase 1: Captura de Mobs

1. **Criar Pocket Dimensional Prison**
   - Item craftável
   - Usa DataComponents (NÃO NBT) para armazenar entidade

2. **Capturar Mob**
   - Right-click no mob com a prison
   - `EntityStorageUtil.storeMob()` salva snapshot completo
   - Mob é removido do mundo

### Fase 2: Decaimento

1. **Construir Decay Chamber**
   - Estrutura 3x3x3 oco
   - Controller no centro

2. **Inserir Prison**
   - Right-click no controller com prison
   - `DecayChamberControllerBlockEntity.acceptMob()`
   - Valida estrutura antes de aceitar

3. **Processamento**
   - Máquina consome HP do mob ao longo do tempo
   - Gera Entropy Biomass proporcional ao HP máximo
   - Output via EntropyStorage + ItemStackHandler

### Fase 3: Refinamento

1. **Construir Entropy Reservoir**
   - Máquina que usa framework `AbstractMachineBlockEntity`

2. **Processar Biomass**
   - Input: 2x Entropy Biomass (slots separados)
   - Consome FE para processar
   - Output: Decaying Graviton

### Fase 4: Geração de Energia

1. **Construir Quantum Vacuum Turbine**
   - Gerador (não processador)
   - Queima Decaying Graviton como combustível

2. **Geração de FE**
   - Base: Config.QVT_FE_PER_TICK
   - Pode ser amplificado por Pirâmides próximas
   - Auto-push para vizinhos

### Fase 5: Amplificação (Opcional)

1. **Construir Alien Pyramid**
   - Estrutura multiblock
   - Core valida estrutura periodicamente
   - Escaneia Turbinas próximas
   - Aplica multiplicador de boost (highest-wins)

### Fase 6: Transporte de Entropia

1. **Conectar com Entropy Cables**
   - Cables conectam máquinas
   - Transferência determinística e segura
   - Usa EntropyTransaction para atomicidade

### Fase 7: Evolução do Jogador

1. **Construir Evolution Chamber**
   - Máquina que consome entropia

2. **Evoluir**
   - Jogador fica em cima do bloco
   - Máquina detecta jogador (AABB)
   - Consome entropia gradualmente
   - Após completar, aumenta evolution stage

### Fase 8: Habilidades

1. **Desbloquear Habilidades**
   - Habilidades desbloqueiam baseadas em stage
   - Stage 1: Entropy Shield, Decay Vision
   - Stage 2: Gravitational Pull
   - Stage 3: Entropy Burst

2. **Ativar Habilidades**
   - Pressionar V/B/N/M (keybinds)
   - Consome entropia armazenada
   - Aplica efeito
   - Entra em cooldown

---

## 💻 Padrões de Código

### Padrão de Block Entity

```java
public class MyBlockEntity extends AlienBlockEntity {
    // State
    private int progress = 0;
    
    // Components (se necessário)
    private final EntropyStorage entropyStorage;
    
    // Constructor
    public MyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_BE.get(), pos, state);
        // Initialize components
    }
    
    // Tick Logic
    @Override
    protected void onUpdateServer() {
        if (!CapabilityUtils.isValidServerLevel(level)) return;
        // Processamento aqui
    }
    
    // State Validation
    public void validateState() {
        // Corrigir valores inválidos
    }
    
    // Lifecycle
    @Override
    public void onLoad() {
        super.onLoad();
        validateState();
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        // Limpar referências
    }
    
    // NBT
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // Salvar state
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // Carregar com SafeNBT
        validateState();
    }
}
```

### Padrão de Acesso a Capabilities

```java
// SEMPRE use CapabilityUtils
IEntropyHandler handler = CapabilityUtils.safeGetEntropyHandler(level, pos, side);
if (handler == null) return; // Não encontrado ou inválido

// Para neighbors
IEntropyHandler neighbor = CapabilityUtils.safeGetNeighborEntropyHandler(level, pos, direction);
```

### Padrão de Transferência de Entropia

```java
// Para transferências entre handlers
EntropyTransaction transaction = EntropyTransaction.transfer(source, dest, amount);
if (transaction.isCommitted()) {
    int transferred = transaction.getAmount();
    // Sucesso
}

// Para consumo (extract sem destination)
int extracted = handler.extractEntropy(amount, false);
if (extracted < amount) {
    // Falhou
}
```

### Padrão de Acesso a Dados de Evolução

```java
// SEMPRE use PlayerEvolutionHelper
PlayerEvolutionData data = PlayerEvolutionHelper.get(player);
// Ou use CapabilityUtils para validação extra
PlayerEvolutionData data = CapabilityUtils.safeGetEvolutionData(player);
if (data == null) return;

// Operações
data.insertEntropy(100);
data.extractEntropy(50);
data.setEvolutionStage(2);
```

### Padrão de NBT

```java
// Leitura: SEMPRE use SafeNBT
int value = SafeNBT.getInt(tag, "Key", defaultValue);
float f = SafeNBT.getFloat(tag, "Key", 0.0f);
CompoundTag sub = SafeNBT.getCompound(tag, "Key");

// Escrita: direto
tag.putInt("Key", value);
```

### Padrão de Validação

```java
// Use StateValidator para clamping
entropy = StateValidator.clampEntropy(value, capacity);
progress = StateValidator.clampProgress(progress, maxProgress);
multiplier = StateValidator.clampMultiplier(multiplier, min, max);
```

### Padrão de Debug

```java
// Debug condicional (zero overhead quando desabilitado)
if (AlienTechDebug.ENTROPY.isEnabled()) {
    AlienTechDebug.ENTROPY.log("Message: {}", value);
}
```

---

## 🔐 Garantias de Segurança

### Entropia
- ✅ Zero duplicação (EntropyTransaction)
- ✅ Zero perda não intencional
- ✅ Nunca negativo (validação automática)
- ✅ Nunca excede capacidade (clamping)
- ✅ Transferências atômicas

### Evolução
- ✅ Server-authoritative (cliente nunca decide)
- ✅ Validação de stage antes de evoluir
- ✅ Entropia consumida atomicamente
- ✅ Persiste entre saves e morte

### Habilidades
- ✅ Cooldown thread-safe (ConcurrentHashMap)
- ✅ Limpeza automática de memória
- ✅ Validação server-side completa
- ✅ Packet spam protection

### BlockEntities
- ✅ Validação de level antes de operações
- ✅ Validação de estado após mutações
- ✅ Safe NBT loading
- ✅ Cleanup em setRemoved()

### Networking
- ✅ Validação server-side de todos os packets
- ✅ Namespace validation
- ✅ Nunca confia no cliente

---

## 📊 Configurações Importantes

### Config.java - Principais Valores

**Entropia**:
- `ENTROPY_CABLE_TRANSFER_RATE`: 500 (entropia/tick por conexão)

**Decay Chamber**:
- `DECAY_CHAMBER_TICKS_PER_HP`: 10 (ticks por HP do mob)
- `DECAY_CHAMBER_BIOMASS_PER_HP`: 0.5 (biomass por HP)

**Entropy Reservoir**:
- `ENTROPY_RESERVOIR_PROCESS_TIME`: 200 ticks
- `ENTROPY_RESERVOIR_ENERGY_PER_TICK`: 20 FE/t

**Quantum Vacuum Turbine**:
- `QVT_FE_PER_TICK`: 256 FE/t (base)
- `QVT_BURN_TIME_PER_GRAVITON`: 400 ticks

**Pyramid**:
- `PYRAMID_TIER1_MULTIPLIER`: 4.0x (boost base)
- `PYRAMID_SCAN_INTERVAL`: 200 ticks

**Evolution Chamber**:
- `MAX_EVOLUTION_STAGE`: 5
- `EVOLUTION_CHAMBER_ENTROPY_COST`: [0, 5000, 20000, 80000, 250000, 1000000]
- `EVOLUTION_CHAMBER_TICKS_PER_STAGE`: [0, 200, 400, 600, 800, 1200]

**Habilidades**:
- Custos e cooldowns configuráveis por habilidade

---

## 🎯 Como Adicionar Novos Componentes

### Adicionar Nova Máquina

1. **Criar BlockEntity**
   ```java
   public class MyMachineBlockEntity extends AlienBlockEntity {
       // Ou AbstractMachineBlockEntity se usar framework
   }
   ```

2. **Criar Block**
   ```java
   public class MyMachineBlock extends BaseEntityBlock {
       // Ticker server-side apenas
   }
   ```

3. **Registrar**
   - `ModBlocks.registerBlock()`
   - `ModBlockEntities.register()`
   - `ModCreativeModeTabs` (se necessário)

4. **Registrar Capabilities** (se necessário)
   - `CommonModEvents.registerCapabilities()`

5. **Adicionar Config** (se necessário)
   - `Config.java`

### Adicionar Nova Habilidade

1. **Criar Classe**
   ```java
   public class MyAbility extends BaseEvolutionAbility {
       // Implementar métodos abstratos
   }
   ```

2. **Registrar**
   ```java
   // Em ModAbilities.register()
   AbilityRegistry.register(new MyAbility());
   ```

3. **Adicionar Config**
   - Custo, cooldown, duração

4. **Adicionar Keybind** (opcional)
   - `AbilityKeyHandler` já suporta 4 slots

### Adicionar Novo Item

1. **Criar Classe**
   ```java
   public class MyItem extends Item {
       // Comportamento customizado
   }
   ```

2. **Registrar**
   ```java
   // Em ModItems
   public static final DeferredItem<Item> MY_ITEM = ITEMS.register(...);
   ```

---

## 🐛 Debugging

### Habilitar Debug Logging

Editar `AlienTechDebug.java`:
```java
private static final boolean ENABLE_ENTROPY_DEBUG = true;
private static final boolean ENABLE_EVOLUTION_DEBUG = true;
private static final boolean ENABLE_MACHINE_DEBUG = true;
private static final boolean ENABLE_ABILITY_DEBUG = true;
```

### Logs Importantes

- **Entropy transfers**: Logados quando ENTROPY debug habilitado
- **Evolution changes**: Logados quando EVOLUTION debug habilitado
- **State corrections**: Logados quando valores são corrigidos
- **Ability activations**: Logados quando ABILITY debug habilitado

---

## 🔄 Ciclo de Vida de BlockEntities

### Tick Cycle

1. **Block.getTicker()** → chama `BlockEntityTicker`
2. **BlockEntityTicker** → chama método estático `tickServer()`
3. **tickServer()** → valida inputs → chama `onUpdateServer()`
4. **onUpdateServer()** → lógica da máquina

### Load/Save Cycle

1. **onLoad()** → chamado quando chunk carrega
   - Validar estado
   - Limpar referências inválidas

2. **loadAdditional()** → chamado ao deserializar NBT
   - Usar SafeNBT
   - Validar após load

3. **saveAdditional()** → chamado ao serializar
   - Salvar state

4. **setRemoved()** → chamado quando bloco removido
   - Limpar referências
   - Dropar itens (se necessário)

---

## 🎨 Assets e Recursos

### Estrutura de Assets

```
src/main/resources/
├── assets/alientech/
│   ├── blockstates/        # Definições de estados de bloco
│   ├── models/
│   │   ├── block/          # Modelos de blocos
│   │   └── item/           # Modelos de itens
│   ├── lang/
│   │   ├── en_us.json      # Inglês
│   │   └── pt_br.json      # Português
│   └── textures/           # Texturas (futuro)
└── data/alientech/
    └── recipes/            # Receitas customizadas
```

### Modelos Padrão

- **cube_all**: Para blocos simples
- **Texture placeholder**: `minecraft:block/obsidian` (temporário)

---

## 🚀 Como o Mod Inicializa

### Fase de Registro (Mod Loading)

1. **AlienTech Constructor**
   - Registra todos os DeferredRegisters
   - Registra event listeners
   - Registra config

2. **DeferredRegisters**
   - `ModItems` → itens
   - `ModBlocks` → blocos
   - `ModBlockEntities` → block entities
   - `ModMenuTypes` → menus (futuro)
   - `ModRecipes` → receitas
   - `ModAttachments` → attachments
   - `ModAbilities` → habilidades

3. **CommonModEvents**
   - Registra capabilities
   - Registra event handlers

### Fase de Setup (Common Setup)

1. **onCommonSetup()**
   - Registra habilidades (`ModAbilities.register()`)
   - Outras inicializações

### Fase de Runtime (Server Starting)

1. **onServerStarting()**
   - Validações finais
   - Log de inicialização

---

## 🔗 Dependências Entre Sistemas

### Entropia → Evolução
- Evolution Chamber consome entropia
- Jogador armazena entropia para habilidades

### Evolução → Habilidades
- Stage determina quais habilidades estão disponíveis
- Habilidades consomem entropia armazenada

### Máquinas → Entropia
- Decay Chamber produz entropia
- Entropy Reservoir consome biomassa (entropia materializada)
- Evolution Chamber consome entropia

### Energia → Máquinas
- Entropy Reservoir precisa de FE
- Quantum Vacuum Turbine produz FE

### Pirâmides → Turbinas
- Pirâmides amplificam geração de FE
- Turbinas recebem boost multiplier

---

## 📝 Convenções de Nomenclatura

### Classes
- **Blocks**: `*Block.java` (ex: `EvolutionChamberBlock`)
- **BlockEntities**: `*BlockEntity.java` (ex: `EvolutionChamberBlockEntity`)
- **Items**: `*Item.java` (ex: `PocketDimensionalPrisonItem`)
- **Interfaces**: `I*` (ex: `IEntropyHandler`)
- **Utilities**: `*Utils.java` ou `*Helper.java`

### Métodos
- **Getters**: `get*()` (ex: `getEntropy()`)
- **Setters**: `set*()` (ex: `setEvolutionStage()`)
- **Validators**: `validate*()` ou `is*()` (ex: `validateState()`)
- **Tick methods**: `onUpdateServer()` ou `serverTick()`

### Variáveis
- **State**: camelCase (ex: `progress`, `targetStage`)
- **Constants**: UPPER_SNAKE_CASE (ex: `KEY_PROGRESS`)
- **Config**: UPPER_SNAKE_CASE (ex: `MAX_EVOLUTION_STAGE`)

---

## ⚠️ Regras Críticas

### NUNCA FAÇA

1. ❌ Acessar capabilities sem `CapabilityUtils`
2. ❌ Ler NBT sem `SafeNBT`
3. ❌ Modificar entropia sem validação
4. ❌ Confiar em dados do cliente
5. ❌ Criar referências estáticas para Level/Player/BlockEntity
6. ❌ Esquecer de validar estado após mutações
7. ❌ Usar FE ao invés de entropia (são sistemas separados)
8. ❌ Modificar evolution stage diretamente (use Evolution Chamber)

### SEMPRE FAÇA

1. ✅ Validar level antes de operações
2. ✅ Usar `CapabilityUtils` para capabilities
3. ✅ Usar `SafeNBT` para NBT
4. ✅ Validar estado após load e mutações
5. ✅ Limpar referências em `setRemoved()`
6. ✅ Validar server-side todos os packets
7. ✅ Usar `StateValidator` para clamping
8. ✅ Logar com `AlienTechDebug` (quando apropriado)

---

## 🎓 Exemplos Práticos

### Exemplo: Criar Máquina que Consome Entropia

```java
public class MyEntropyMachine extends AlienBlockEntity {
    private final EntropyStorage entropyStorage;
    
    public MyEntropyMachine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_MACHINE_BE.get(), pos, state);
        this.entropyStorage = new EntropyStorage(
            10000, // capacity
            0, 0, // no limits
            false, true, // can extract only
            this::setChanged
        );
    }
    
    @Override
    protected void onUpdateServer() {
        if (!CapabilityUtils.isValidServerLevel(level)) return;
        
        // Consumir entropia de neighbors
        for (Direction dir : Direction.values()) {
            IEntropyHandler source = CapabilityUtils.safeGetNeighborEntropyHandler(
                level, worldPosition, dir);
            if (source != null && source.canExtract()) {
                int extracted = source.extractEntropy(100, false);
                if (extracted > 0) {
                    entropyStorage.insertEntropy(extracted, false);
                    break;
                }
            }
        }
        
        // Usar entropia armazenada
        if (entropyStorage.getEntropy() >= 1000) {
            entropyStorage.extractEntropy(1000, false);
            // Fazer algo com a entropia
        }
    }
}
```

### Exemplo: Criar Habilidade Customizada

```java
public class MyCustomAbility extends BaseEvolutionAbility {
    @Override
    public String getId() { return "my_custom_ability"; }
    
    @Override
    public String getDisplayName() { return "My Custom Ability"; }
    
    @Override
    public int getRequiredStage() { return 2; }
    
    @Override
    public int getEntropyCost() { return Config.MY_ABILITY_COST.get(); }
    
    @Override
    public int getCooldownTicks() { return Config.MY_ABILITY_COOLDOWN.get(); }
    
    @Override
    public String getDescription() {
        return "Does something cool";
    }
    
    @Override
    protected void applyEffect(ServerPlayer player) {
        if (player == null || !CapabilityUtils.isValidServerLevel(player.level())) {
            return;
        }
        // Implementar efeito aqui
    }
}
```

---

## 📚 Referências Rápidas

### Acessar Entropia
```java
IEntropyHandler handler = CapabilityUtils.safeGetEntropyHandler(level, pos, side);
int entropy = handler.getEntropy();
int inserted = handler.insertEntropy(100, false);
```

### Acessar Evolução
```java
PlayerEvolutionData data = PlayerEvolutionHelper.get(player);
int stage = data.getEvolutionStage();
data.insertEntropy(100);
data.setEvolutionStage(2);
```

### Transferir Entropia
```java
EntropyTransaction tx = EntropyTransaction.transfer(source, dest, amount);
if (tx.isCommitted()) {
    int transferred = tx.getAmount();
}
```

### Validar Estado
```java
public void validateState() {
    entropy = StateValidator.clampEntropy(entropy, capacity);
    progress = StateValidator.clampProgress(progress, maxProgress);
}
```

### Debug Logging
```java
AlienTechDebug.ENTROPY.log("Message: {}", value);
AlienTechDebug.EVOLUTION.log("Player evolved to stage {}", stage);
```

---

## 🎯 Resumo Executivo

**AlienTech** é um mod de tecnologia/magia onde:

1. **Entropia** é energia biológica extraída de mobs
2. **Máquinas** processam entropia em diferentes estágios
3. **Jogadores** evoluem consumindo entropia
4. **Habilidades** são desbloqueadas por evolução
5. **Tudo** é server-authoritative e production-grade

**Arquitetura**:
- Modular e extensível
- Segura e validada
- Determinística
- Multiplayer-safe
- Memory-safe

**Padrões**:
- Use frameworks existentes
- Siga convenções de nomenclatura
- Valide tudo server-side
- Use utilities de segurança
- Documente comportamento

Este documento serve como guia completo para entender e trabalhar com o mod AlienTech. Qualquer IA ou desenvolvedor pode usar este contexto para ajudar no desenvolvimento futuro do mod.

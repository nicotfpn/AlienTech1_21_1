# Análise de Arquitetura: Mekanism vs AlienTech

Fiz uma análise no código-fonte do **Mekanism** (versão 1.21.1) recém-clonado. O Mekanism é a verdadeira referência máxima (Gold Standard) de mods de tecnologia no NeoForge. Comparando a arquitetura dele com o estado atual do `AlienTech`, encontrei várias melhorias estruturais e de performance que podemos adotar no nosso mod!

---

## 🏗️ 1. BlockEntities e "Components" ao invés de Herança Cega

**Como o AlienTech faz hoje:**
Temos o `AbstractMachineBlockEntity` que embute slot layout, armazenamento FE, lógicas de processamento e auto-push/pull. Todas as máquinas estendem ele e ganham esses métodos.

**Como o Mekanism faz (`TileEntityMekanism.java` e `CapabilityTileEntity.java`):**
Eles **não** colocam toda a lógica pesada na classe pai. Em vez disso, a classe base atua como uma **Casca Registradora**. Eles usam uma abordagem "Entity Component System" (ECS).
- A máquina só instancia o que precisa: `components.add(new TileComponentUpgrade(this))`
- Eles separam a lógica de *Ticking* (ticks) da lógica de *Armazenamento/Capabilities*.
- Eles usam `CapabilityCache` (no `CapabilityTileEntity`) para deixar os `getCapability` extremamente rápidos e evitar alocações de memória a cada frame/tick.

**🚀 O que podemos melhorar no AlienTech:**
- Criar um **Cache de Capabilities** centralizado no `AbstractMachineBlockEntity` para não re-verificar `getCapability` toda vez que a `Quantum Vacuum Turbine` for exportar energia, salvando muito tempo de CPU.
- Extrair o *Auto-Push/Pull* e o *Machine Energy* para **Componentes Menores**, permitindo que futuras máquinas usem partes da lógica sem herdar o lixo que não precisam.

---

## 🧩 2. Gerenciamento de Multiblocks

**Como o AlienTech faz hoje:**
A *Decay Chamber* e a *Alien Pyramid* validam a estrutura (percorrem os blocos XYZ num range O(N³)) periodicamente. Cada vez que isso roda, é um leve peso pro servidor.

**Como o Mekanism faz (`MultiblockManager.java` e `MultiblockCache.java`):**
O Mekanism é **BRUTAL** nisso. O framework de multiblock dele gera um `UUID` único para a "Formação".
- Uma vez formada, os blocos periféricos (Casings) apontam diretamente pra memória RAM do Core.
- Quando a máquina sofre uma interrupção (alguém quebra uma parede), o manager salva o inventário num `MultiblockCache` assíncrono. Se o player refizer a parede, ele restaura a energia/itens baseados no ID sem precisar re-dropar itens no chão.
- Eles evitam escanear o mundo em ticks contínuos. A validação é engatilhada apenas por "Atualizações de Vizinhos" (Neighbor Block Updates) disparados pelo próprio Minecraft quando um bloco quebra ou é posto.

**🚀 O que podemos melhorar no AlienTech:**
- Em vez do `PyramidCoreBlockEntity` escanear `PYRAMID_SCAN_INTERVAL`, ele deve reagir a um evento de estado do mundo ou de `neighbor update`.
- Implementar um pequeno **Cache (UUID) In-Memory** para a *Decay Chamber*, garantindo que se o jogador quebrar a casca por acidente no meio do processo biológico/entropia pesada, o decaimento congele em vez de resetar ou dar NullPointer.

---

## 🖥️ 3. Sincronização Server/Client (Gui & Containers)

**Como o AlienTech faz hoje:**
O mod já tem uma implementação “Mekanism-like” usando `AlienContainerMenu` + `SyncableValue`/`SyncableLong`, o que evita os limites de 32-bit do `DataSlot` e reduz drasticamente o tráfego de rede. No entanto, havia um problema clássico: o cliente ao abrir uma GUI recebia valores zerados porque o servidor só enviava atualizações quando algo mudava (e a primeira sincronização jamais ocorria).

**Como o Mekanism faz (`MekanismContainer.java`):**
O Mekanism criou uma abstração impecável chamada `ISyncableData` (com classes como `SyncableLong`, `SyncableFluidStack`, e `SyncableChemicalStack`).
- O Container do Servidor só manda um pacote **quando o valor exato no setter sofre uma mutação detectada (isDirty)**.
- O cliente recebe pedaços incrivelmente pequenos. Se a máquina parar, a rede não transmite mais NADA relacionado aquela máquina, enquanto as GUIs vanilla costumam fazer polling burro.
- Crucialmente, o Mekanism garante **sync inicial**: o container ALWAYS manda o estado inicial quando o player abre a GUI.

**🚀 O que podemos melhorar no AlienTech:**
- Certificar que o `SyncableLong` envia o estado inicial no primeiro `broadcastChanges()` após o container ser aberto (fix já aplicado).
- As nossas variáveis de Entropia já estão lidando com números absurdos (ex: Stage 5 requer 1,000,000 de Entropia). Isso é tranquilo para ints, mas se expandirmos para tiers cósmicos, vamos estourar.
- Consolidar a lógica de sincronização em uma base reutilizável (ex: `SyncableValue`/`SyncableLong`) e garantir que todas as GUIs de máquina usem o mesmo padrão, evitando que alguma ainda caia de volta nos `DataSlot` ou em `NetworkHooks.openGui` com pacotes pesados.

# Guia Definitivo: Animações de Máquinas com GeckoLib 4 no NeoForge 1.21.1

A biblioteca **GeckoLib** facilita enormemente o processo de animar blocos (BlockEntities), itens e entidades usando keyframes (como as animações feitas no BlockBrench). Este guia detalha as marcações para criar e implementar o BlockEntity animado de uma máquina (como a Decay Chamber).

---

## 🎨 1. Configurando o Blockbench

1. Abra o **Blockbench**.
2. Vá em `File > Plugins`, procure por **"GeckoLib Animation Utils"** e instale.
3. Vá em `File > New` e escolha **GeckoLib Animated Model**.
4. Crie seus ossos (`bones/groups`) e adicione as meshes (cubos) dentro deles.
   * **IMPORTANTE:** Para que partes rodem/animem, elas DEVEM estar dentro de uma pasta (bone) dedicada (exemplo: `rotor_externo`, `pistoes`, `nucleo`). O pivot point de cada bone será a âncora de rotação.
5. Crie a textura e pinte seu modelo. Salve como `.png`.
6. Vá na aba **Animate**, crie uma animação (exemplo: `animation.minhamaquina.idle` ou `animation.minhamaquina.working`). Pode colocar o `Loop Mode` como `loop` se for contínua.
7. Quando tudo estiver pronto, faça os 3 exports:
   - `Export GeckoLib Model` (`.geo.json`)
   - `Export Animations` (`.animation.json`)
   - Save Texture (`.png`)

### Onde colocar os arquivos gerados em `src/main/resources/assets/alientech/`:
* `geo/block/minhamaquina.geo.json` (Modelo)
* `animations/block/minhamaquina.animation.json` (Animação)
* `textures/block/minhamaquina.png` (Textura)

---

## 🛠️ 2. Criando o Block Entity

No mod AlienTech, o ideal é a sua BlockEntity estender `AbstractMachineBlockEntity` (agora você também precisará implementar a interface `GeoBlockEntity` do GeckoLib).

**Exemplo Prático:**

```java
package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MyAnimatedMachineBlockEntity extends AbstractMachineBlockEntity implements GeoBlockEntity {
    
    // 1. O cache gerenciador de estado criado exclusivamente para a instância deste BE
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 2. Animações cruas para agilizar
    private static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("animation.minhamaquina.working");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.minhamaquina.idle");

    public MyAnimatedMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_ANIMATED_MACHINE.get(), pos, state);
    }

    // 3. Método da interface GeoBlockEntity
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // 4. Registro de Controladores (Aqui vc dita as regras de quando animar)
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine_controller", 5, state -> {
            
            // Aqui vc checa o status da sua BE (isProcessing vem do AbstractMachineBlockEntity)
            if (this.isProcessing()) {
                return state.setAndContinue(WORKING_ANIM);
            }
            
            // Animação padrão parada
            return state.setAndContinue(IDLE_ANIM);
        }));
    }
}
```

> **📌 Dica Importante sobre Client/Server:** O BlockEntity do Server processa os itens/energia, mas o BlockEntity do *Client* é quem roda a renderização. Portanto, certifique-se que variáveis de estado (como boolean "is_working") sejam sincronizadas do servidor para o cliente (pelo `getUpdatePacket()` / `getUpdateTag()` no BE).

---

## 🧱 3. Criando o Modelo de Renderização (GeoModel)

Aqui é a classe que indica onde estão os arquivos JSON (`geo`, `animations`) e a `texture` exportada pelo Blockbench.

```java
package net.nicotfpn.alientech.client.renderer.model;

import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.entity.MyAnimatedMachineBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class MyAnimatedMachineModel extends GeoModel<MyAnimatedMachineBlockEntity> {

    @Override
    public ResourceLocation getModelResource(MyAnimatedMachineBlockEntity object) {
        // Aponta para: src/main/resources/assets/alientech/geo/block/minhamaquina.geo.json
        return ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "geo/block/minhamaquina.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MyAnimatedMachineBlockEntity object) {
        // Aponta para: src/main/resources/assets/alientech/textures/block/minhamaquina.png
        return ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "textures/block/minhamaquina.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MyAnimatedMachineBlockEntity object) {
        // Aponta para: src/main/resources/assets/alientech/animations/block/minhamaquina.animation.json
        return ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "animations/block/minhamaquina.animation.json");
    }
}
```

---

## 🎥 4. Criando o BlockEntityRenderer (GeoBlockRenderer)

O renderer nativamente providenciado pelo GeckoLib para renderizar o modelo no mundo:

```java
package net.nicotfpn.alientech.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.nicotfpn.alientech.block.entity.MyAnimatedMachineBlockEntity;
import net.nicotfpn.alientech.client.renderer.model.MyAnimatedMachineModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MyAnimatedMachineRenderer extends GeoBlockRenderer<MyAnimatedMachineBlockEntity> {
    public MyAnimatedMachineRenderer(BlockEntityRendererProvider.Context context) {
        super(new MyAnimatedMachineModel());
    }
}
```

---

## 🔌 5. Registrando tudo no Mod Event Bus (Client-Side)

Para informar o NeoForge a usar seu novo Renderer em vez do render padronizado de blocos cubicos da Mojang:

1. No block register, garanta que a propriedade do bloco para renderização **não seja o padrão**.
```java
// Dentro do seu MachineBlock
@Override
protected RenderShape getRenderShape(BlockState pState) {
    return RenderShape.ENTITYBLOCK_ANIMATED; 
    // ^ Isso avisa o minecraft: "Ei, não desenhe modelos JSON, deixe para os renderers mágicos"
}
```

2. Registre o visual no *Event Bus do Client*:

```java
package net.nicotfpn.alientech.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.client.renderer.MyAnimatedMachineRenderer;

@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MY_ANIMATED_MACHINE.get(),
                MyAnimatedMachineRenderer::new);
    }
}
```

*(Obs: os imports do `@EventBusSubscriber` aqui são pra Forge/NeoForge antigos, apenas adapte se no NeoForge 1.21.1 for na estrutura `net.neoforged.neoforge.client.event...`)*

---

### 🎉 Parabéns! 
Sua máquina irá agora rodar engrenagens e pistões maravilhosamente usando GeckoLib. Siga esse esqueleto sempre que quiser migrar uma máquina para ter modelo animado 3D!

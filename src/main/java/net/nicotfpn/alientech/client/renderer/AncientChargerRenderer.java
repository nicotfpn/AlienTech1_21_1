package net.nicotfpn.alientech.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.nicotfpn.alientech.block.entity.AncientChargerBlockEntity;

public class AncientChargerRenderer implements BlockEntityRenderer<AncientChargerBlockEntity> {
    public AncientChargerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AncientChargerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        ItemStack itemStack = blockEntity.getItemHandler().getStackInSlot(0);
        if (itemStack.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Position the item on top of the block (0.5 centers it, 0.55 puts it slightly
        // above 8-pixel height)
        poseStack.translate(0.5f, 0.55f, 0.5f);

        // Scale it to look nice (75% size)
        poseStack.scale(0.75f, 0.75f, 0.75f);

        // Rotate slowly if desired (or fixed for stability)
        // poseStack.mulPose(Axis.YP.rotationDegrees((blockEntity.getLevel().getGameTime()
        // + partialTick) * 2));
        // Fixed rotation for "Docked" look
        poseStack.mulPose(Axis.XP.rotationDegrees(90)); // Lay flat? Or stand up?
        // 90 degrees X makes it lie flat on the surface.

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedModel = itemRenderer.getModel(itemStack, blockEntity.getLevel(), null, 0);

        itemRenderer.render(itemStack, ItemDisplayContext.FIXED, true, poseStack, bufferSource, packedLight,
                packedOverlay, bakedModel);

        poseStack.popPose();
    }
}

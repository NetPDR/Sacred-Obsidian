package com.netpdrmod.client.renderer;

import com.netpdrmod.entity.SacredObsidianEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class SacredObsidianRenderer extends EntityRenderer<SacredObsidianEntity> {

    private final ItemRenderer itemRenderer;


    public SacredObsidianRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();  // 获取默认的物品渲染器 // Get the default item renderer
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SacredObsidianEntity entity) {
        // 这里是指定黑曜石材质的路径 // Specify the texture path for the obsidian item
        return new ResourceLocation("minecraft", "textures/item/obsidian.png");
    }

    @Override
    public void render(SacredObsidianEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // 只做旋转：继续用服务器同步过来的 entity.getXRot()/getYRot() // Rotate only: continue with entity.getXRot()/getYRot() that the server syncs
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(entity.getXRot())));
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(entity.getYRot())));

        // 让 ItemRenderer 画出 obsidian 方块 // Let the ItemRenderer draw the obsidian block
        ItemStack stack = entity.getItem();
        // 获取物品模型 // Get the item model
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, 0);
        itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight,
                OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
    }
}

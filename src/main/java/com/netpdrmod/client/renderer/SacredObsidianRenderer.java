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
import net.minecraft.world.level.Level;
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
        ItemStack itemStack = entity.getItem();

        // 获取世界和 LivingEntity // Get the world and LivingEntity
        Level level = entity.getCommandSenderWorld();  // 获取当前实体所在的世界 // Get the world the entity is in

        // 获取物品模型 // Get the item model
        BakedModel bakedModel = itemRenderer.getModel(itemStack, level, null, 0);

        // 推入 PoseStack 保存当前变换 // Push to PoseStack to save the current transformation
        poseStack.pushPose();

        // 绕 X 轴旋转 // Rotate around the X axis
        Quaternionf rotationX = new Quaternionf().rotateX((float) Math.toRadians(entity.getXRot()));
        poseStack.mulPose(rotationX);

        // 绕 Y 轴旋转 // Rotate around the Y axis
        Quaternionf rotationY = new Quaternionf().rotateY((float) Math.toRadians(entity.getYRot()));
        poseStack.mulPose(rotationY);

        // 绕 Z 轴旋转 // Rotate around the Z axis
        Quaternionf rotationZ = new Quaternionf().rotateZ((float) Math.toRadians(entity.getYRot()));
        poseStack.mulPose(rotationZ);

        // 调用 render 方法时，确保传递 BakedModel // Ensure that the BakedModel is passed when calling the render method
        itemRenderer.render(itemStack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);

        // 恢复 PoseStack 的状态 // Restore the PoseStack state
        poseStack.popPose();  // 恢复变换

        // 渲染实体的默认操作 // Default rendering operation for the entity
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

}

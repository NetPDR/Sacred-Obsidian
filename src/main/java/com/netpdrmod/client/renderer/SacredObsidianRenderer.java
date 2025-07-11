package com.netpdrmod.client.renderer;

import com.mojang.math.Axis;
import com.netpdrmod.entity.SacredObsidianEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SacredObsidianRenderer extends EntityRenderer<SacredObsidianEntity> {

    private final ItemRenderer itemRenderer;

    private static final Map<Integer, Vec3> renderPositions = new HashMap<>();

    public SacredObsidianRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();  // 获取默认的物品渲染器 // Get the default item renderer
        this.shadowRadius = 0.15F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SacredObsidianEntity entity) {
        // 这里是指定黑曜石材质的路径 // Specify the texture path for the obsidian item
        return new ResourceLocation("minecraft", "textures/item/obsidian.png");
    }

    @Override
    public void render(SacredObsidianEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        int id = entity.getId();
        Vec3 actual = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        Vec3 previous = renderPositions.getOrDefault(id, actual);
        Vec3 interpolated = previous.lerp(actual, partialTicks);

        renderPositions.put(id, actual);

        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();
        poseStack.translate(interpolated.x - camera.x, interpolated.y - camera.y, interpolated.z - camera.z);

        // 插值旋转
        float yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

        // 让 ItemRenderer 画出 obsidian 方块 // Let the ItemRenderer draw the obsidian block
        ItemStack stack = entity.getItem();

        // 获取物品模型 // Get the item model
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, 0);
        itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight,
                OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(@NotNull SacredObsidianEntity entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}

package com.netpdrmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.netpdrmod.client.ClientObsidianManager.ObsidianEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ObsidianRenderer {
    public static void render(ObsidianEntry e,
                              PoseStack ps,
                              ItemRenderer itemRenderer,
                              MultiBufferSource buffers,
                              float pticks) {
        Minecraft mc = Minecraft.getInstance();

        ps.pushPose();

        // 插值旋转 // Interpolation rotation
        ps.mulPose(Axis.YP.rotationDegrees(e.yRot));

        // 让方块稍微浮起来一点 // Let the square float up a little bit
        ps.translate(0, 0.25, 0);

        // 光照（这里仍然用世界坐标） // Lighting (still using world coordinates here)
        double wx = Mth.lerp(pticks, e.prevPos.x, e.currentPos.x);
        double wy = Mth.lerp(pticks, e.prevPos.y, e.currentPos.y);
        double wz = Mth.lerp(pticks, e.prevPos.z, e.currentPos.z);

        int light = mc.level != null
                ? LevelRenderer.getLightColor(mc.level, BlockPos.containing(wx, wy, wz))
                : 15728880;

        // 渲染黑曜石方块作为物品 // Render obsidian blocks as items
        ItemStack stack = new ItemStack(Items.OBSIDIAN);
        itemRenderer.render(
                stack,
                ItemDisplayContext.GROUND,
                false,
                ps,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY,
                itemRenderer.getModel(stack, mc.level, null, 0)
        );

        ps.popPose();
    }
}
package com.netpdrmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.netpdrmod.client.ClientObsidianManager.ObsidianEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ObsidianRenderer {

    public static void render(
            ObsidianEntry e,
            PoseStack ps,
            ItemRenderer itemRenderer,
            EntityRenderDispatcher disp,
            float pticks
    ) {
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        ps.pushPose();

        double x = Mth.lerp(pticks, e.prevPos.x, e.currentPos.x);
        double y = Mth.lerp(pticks, e.prevPos.y, e.currentPos.y);
        double z = Mth.lerp(pticks, e.prevPos.z, e.currentPos.z);

        double camX = disp.camera.getPosition().x;
        double camY = disp.camera.getPosition().y;
        double camZ = disp.camera.getPosition().z;

        ps.translate(x - camX, y - camY, z - camZ);
        ps.mulPose(Axis.YP.rotationDegrees(e.yRot));

        ItemStack stack = new ItemStack(Items.OBSIDIAN);

        int light = 0;
        if (mc.level != null) {
            light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(x, y, z));
        }

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
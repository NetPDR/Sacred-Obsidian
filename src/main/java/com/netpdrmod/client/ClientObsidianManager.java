package com.netpdrmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.netpdrmod.client.renderer.ObsidianRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientObsidianManager {
    public static final ClientObsidianManager INSTANCE = new ClientObsidianManager();

    public static class ObsidianEntry {
        public Vec3 prevPos, currentPos;
        public float yRot;
        public Supplier<Vec3> targetSupplier;

        public ObsidianEntry(Vec3 start, Supplier<Vec3> targetSupplier) {
            this.prevPos = start;
            this.currentPos = start;
            this.targetSupplier = targetSupplier;
            this.yRot = 0;
        }

        public Vec3 getTarget() {
            return targetSupplier.get();
        }
    }

    private final List<ObsidianEntry> entries = new LinkedList<>();

    public void spawnAtFollowPlayer(Vec3 start) {
        entries.add(new ObsidianEntry(start, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            return p != null ? p.position().add(0, 1.0, 0) : start;
        }));
    }

    public void spawnAt(Vec3 start, Vec3 target) {
        entries.add(new ObsidianEntry(start, () -> target));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) INSTANCE.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        INSTANCE.onRenderLast(event.getPoseStack(), event.getPartialTick());
    }

    private void onClientTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Iterator<ObsidianEntry> it = entries.iterator();
        while (it.hasNext()) {
            ObsidianEntry e = it.next();
            e.yRot = (e.yRot + 2.5F) % 360;
            e.prevPos = e.currentPos;

            Vec3 target = e.getTarget();
            Vec3 dir = target.subtract(e.currentPos).normalize();
            Vec3 velocity = dir.scale(0.1);
            e.currentPos = e.currentPos.add(velocity);

            double distToPlayer = e.currentPos.distanceTo(player.position().add(0, 1.0, 0));
            boolean reached = distToPlayer < 0.5;
            boolean tooFar = e.currentPos.distanceTo(player.position()) > 128.0;

            if (reached || tooFar) {
                if (reached) {
                    player.getCommandSenderWorld().playSound(
                            player, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            0.2F, 1.0F);
                }
                it.remove();
            }
        }
    }

    private void onRenderLast(PoseStack ps, float pticks) {
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (ObsidianEntry e : entries) {
            ps.pushPose();

            // 世界坐标 → 相机相对坐标 // World coordinates → Camera relative coordinates
            double x = Mth.lerp(pticks, e.prevPos.x, e.currentPos.x) - camPos.x;
            double y = Mth.lerp(pticks, e.prevPos.y, e.currentPos.y) - camPos.y;
            double z = Mth.lerp(pticks, e.prevPos.z, e.currentPos.z) - camPos.z;
            ps.translate(x, y, z);

            // 注意这里 buffers 传进去 // Note that the buffers are passed in here
            ObsidianRenderer.render(e, ps, mc.getItemRenderer(), buffer, pticks);

            ps.popPose();
        }

        buffer.endBatch(); // 刷新渲染缓冲 // Refresh render buffer
    }
}
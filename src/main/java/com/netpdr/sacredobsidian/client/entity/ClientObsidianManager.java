package com.netpdr.sacredobsidian.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.netpdr.sacredobsidian.client.render.ObsidianRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * 改造后的客户端伪黑曜石管理器（保留原始渲染接口兼容性） Remodeled client pseudo-Obsidian manager (retaining compatibility with the original rendering interface)
 * - 不再使用 followPlayer 语义 No longer use the followPlayer semantic
 * - 每个条目保留 ownerUUID 与 lastKnownTarget（由服务端初始快照设定） Each entry retains ownerUUID and lastKnownTarget (set by the server's initial snapshot)
 * - 客户端仅进行平滑插值和轻度追踪（如果 owner 在本客户端存在） The client only performs smooth interpolation and light tracking (if the owner exists on this client)
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientObsidianManager {
    public static final ClientObsidianManager INSTANCE = new ClientObsidianManager();

    public static class ObsidianEntry {
        public Vec3 prevPos, currentPos;
        public float yRot;

        // 新增：owner UUID & last known target（由服务端快照传入） // Added: owner UUID & last known target (provided by the server snapshot)
        public final UUID ownerUUID;
        public Vec3 lastKnownTarget;

        public boolean soundPlayed = false;

        public ObsidianEntry(Vec3 start, UUID ownerUUID, Vec3 initialTarget) {
            this.prevPos = start;
            this.currentPos = start;
            this.yRot = 0;
            this.ownerUUID = ownerUUID;
            this.lastKnownTarget = initialTarget;
        }
    }

    private final List<ObsidianEntry> entries = new LinkedList<>();

    /**
     * 由 packet 调用：在客户端生成一个伪实体条目 Called by packet: generate a fake entity entry on the client
     */
    public void spawnWithOwner(Vec3 start, UUID ownerUUID, Vec3 ownerPosSnapshot) {
        entries.add(new ObsidianEntry(start, ownerUUID, ownerPosSnapshot));
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
        Minecraft mc = Minecraft.getInstance();

        if (mc.isPaused()) return;

        if (mc.level == null) return;

        LocalPlayer localPlayer = mc.player; // 可能为 null // May be null
        Iterator<ObsidianEntry> it = entries.iterator();
        while (it.hasNext()) {
            ObsidianEntry e = it.next();
            e.yRot = (e.yRot + 2.5F) % 360;
            e.prevPos = e.currentPos;

            // 如果 owner 在本客户端可见，则做一个轻度 lerp（让表现更平滑） // If the owner is visible in this client, perform a slight lerp (to make the movement smoother)
            Player owner = mc.level.getPlayerByUUID(e.ownerUUID);
            if (owner != null) {
                Vec3 desired = owner.position().add(0, 1.0, 0);
                // 仅改变 lastKnownTarget（缓慢靠近服务端快照或本地 owner） // Only change lastKnownTarget (slowly approach the server snapshot or the local owner)
                e.lastKnownTarget = e.lastKnownTarget.lerp(desired, 0.25);
            }

            Vec3 dirVec = e.lastKnownTarget.subtract(e.currentPos);
            double dist = dirVec.length();
            // 小心防止 normalize 抛异常 // Be careful to prevent normalize from throwing exceptions
            Vec3 velocity = (dist > 1e-6) ? dirVec.normalize().scale(0.1) : Vec3.ZERO;
            e.currentPos = e.currentPos.add(velocity);

            // 到达 owner 或过远时移除 // Remove when reaching the owner or if too far away
            boolean reached;
            if (localPlayer != null) {
                // 只有当本地玩家是 owner 才播放拾取音（避免所有客户端都播放） // The pickup sound is only played when the local player is the owner (to prevent all clients from playing it).
                if (localPlayer.getUUID().equals(e.ownerUUID)) {
                    double distToOwner = e.currentPos.distanceTo(localPlayer.position().add(0, 1.0, 0));
                    reached = distToOwner < 0.5;
                } else {
                    reached = e.currentPos.distanceTo(e.lastKnownTarget) < 0.5;
                }
            } else {
                reached = e.currentPos.distanceTo(e.lastKnownTarget) < 0.5;
            }

            boolean tooFar = e.currentPos.distanceTo(e.lastKnownTarget) > 128.0;

            if (reached && !e.soundPlayed) {
                e.soundPlayed = true;

                if (localPlayer != null) {
                    localPlayer.getCommandSenderWorld().playSound(
                            localPlayer,
                            e.currentPos.x,
                            e.currentPos.y,
                            e.currentPos.z,
                            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            0.2F,
                            1.0F
                    );
                }
            }

            if (reached || tooFar) {
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

            // World -> camera-relative
            double x = Mth.lerp(pticks, e.prevPos.x, e.currentPos.x) - camPos.x;
            double y = Mth.lerp(pticks, e.prevPos.y, e.currentPos.y) - camPos.y;
            double z = Mth.lerp(pticks, e.prevPos.z, e.currentPos.z) - camPos.z;
            ps.translate(x, y, z);

            // 渲染（兼容旧 ObsidianRenderer，传入 entry） // Render (compatible with the old ObsidianRenderer, passing in entry)
            ObsidianRenderer.render(e, ps, mc.getItemRenderer(), buffer, pticks);

            ps.popPose();
        }

        buffer.endBatch();
    }
}
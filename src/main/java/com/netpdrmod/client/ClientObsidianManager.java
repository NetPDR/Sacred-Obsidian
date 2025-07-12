package com.netpdrmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.netpdrmod.client.renderer.ObsidianRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        public Vec3 prevPos;
        public Vec3 currentPos;
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

    // 用于跟随玩家 // Used to follow the player
    public void spawnAtFollowPlayer(Vec3 start) {
        entries.add(new ObsidianEntry(start, () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            return p != null ? p.position().add(0, 1.0, 0) : start;
        }));
    }

    // 用于静态目标 // Used for static targets
    public void spawnAt(Vec3 start, Vec3 target) {
        entries.add(new ObsidianEntry(start, () -> target));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            INSTANCE.onClientTick();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            INSTANCE.onRenderLast(event.getPoseStack(), event.getPartialTick());
        }
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
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                            0.2F, 1.0F);
                }
                it.remove();
            }
        }
    }

    private void onRenderLast(PoseStack ps, float pticks) {
        Minecraft mc = Minecraft.getInstance();
        for (ObsidianEntry e : entries) {
            ObsidianRenderer.render(e, ps, mc.getItemRenderer(), mc.getEntityRenderDispatcher(), pticks);
        }
    }
}
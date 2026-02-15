package com.netpdr.sacredobsidian.client.key.interaction;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.network.VanillaInteractPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;


import static com.netpdr.sacredobsidian.client.key.interaction.VanillaInteractKeyRegistrar.VANILLA_INTERACT_KEY;

@EventBusSubscriber(modid = Sacredobsidian.MODID, value = Dist.CLIENT)
public class VanillaInteractKeyHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!VANILLA_INTERACT_KEY.consumeClick()) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        VanillaInteractPayload payload =
                VanillaInteractPayload.fromHitResult(hit);

        net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
    }
}

package com.netpdr.sacredobsidian.client.key.interaction;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.network.VanillaInteractPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.netpdr.sacredobsidian.client.key.interaction.VanillaInteractKeyRegistrar.VANILLA_INTERACT_KEY;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, value = Dist.CLIENT)
public class VanillaInteractKeyHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!VANILLA_INTERACT_KEY.consumeClick()) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        Sacredobsidian.CHANNEL.sendToServer(
                VanillaInteractPacket.fromHitResult(hit)
        );
    }
}

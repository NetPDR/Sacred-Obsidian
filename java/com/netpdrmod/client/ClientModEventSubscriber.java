package com.netpdrmod.client;

import com.netpdrmod.client.renderer.SacredObsidianRenderer;
import com.netpdrmod.registry.ModEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "netpdrmod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEventSubscriber {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntity.SACRED_OBSIDIAN_ITEM_ENTITY.get(), SacredObsidianRenderer::new);
    }
}

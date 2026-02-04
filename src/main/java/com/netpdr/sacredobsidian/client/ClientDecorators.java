package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.client.render.SacredObsidianCooldownDecorator;
import com.netpdr.sacredobsidian.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

@EventBusSubscriber(
        modid = "sacredobsidian",
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientDecorators {

    @SubscribeEvent
    public static void registerDecorators(RegisterItemDecorationsEvent event) {
        event.register(
                ModItems.SACRED_OBSIDIAN.get(),
                new SacredObsidianCooldownDecorator()
        );
    }
}
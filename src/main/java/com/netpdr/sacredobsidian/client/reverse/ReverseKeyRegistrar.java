package com.netpdr.sacredobsidian.client.reverse;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Sacredobsidian.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ReverseKeyRegistrar {
    public static KeyMapping REVERSE_KEY;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        REVERSE_KEY = new KeyMapping("key.sacredobsidian.reverse", GLFW.GLFW_KEY_V, "key.categories.sacredobsidian");
        event.register(REVERSE_KEY);
    }
}
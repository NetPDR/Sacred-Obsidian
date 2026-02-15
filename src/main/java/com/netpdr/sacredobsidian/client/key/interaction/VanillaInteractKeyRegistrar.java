package com.netpdr.sacredobsidian.client.key.interaction;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Sacredobsidian.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class VanillaInteractKeyRegistrar {
    public static KeyMapping VANILLA_INTERACT_KEY;
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        VANILLA_INTERACT_KEY = new KeyMapping("key.sacredobsidian.vanilla_interact", GLFW.GLFW_KEY_O, "key.categories.sacredobsidian");
        event.register(VANILLA_INTERACT_KEY);
    }
}
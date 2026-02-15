package com.netpdr.sacredobsidian.client.key.toggle;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Sacredobsidian.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ToggleKeyRegistrar {
    public static KeyMapping TOGGLE_DAMAGE_KEY;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        TOGGLE_DAMAGE_KEY = new KeyMapping("key.sacredobsidian.toggle_damage", GLFW.GLFW_KEY_X, "key.categories.sacredobsidian");
        event.register(TOGGLE_DAMAGE_KEY);
    }
}
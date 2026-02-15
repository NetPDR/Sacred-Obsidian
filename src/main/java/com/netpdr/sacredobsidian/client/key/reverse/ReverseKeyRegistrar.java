package com.netpdr.sacredobsidian.client.key.reverse;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ReverseKeyRegistrar {
    public static KeyMapping REVERSE_KEY;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        REVERSE_KEY = new KeyMapping("key.sacredobsidian.reverse", GLFW.GLFW_KEY_V, "key.categories.sacredobsidian");
        event.register(REVERSE_KEY);
    }
}
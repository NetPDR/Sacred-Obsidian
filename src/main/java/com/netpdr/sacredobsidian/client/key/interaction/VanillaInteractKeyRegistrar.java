package com.netpdr.sacredobsidian.client.key.interaction;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class VanillaInteractKeyRegistrar {
    public static KeyMapping VANILLA_INTERACT_KEY;
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        VANILLA_INTERACT_KEY = new KeyMapping("key.sacredobsidian.vanilla_interact", GLFW.GLFW_KEY_O, "key.categories.sacredobsidian");
        event.register(VANILLA_INTERACT_KEY);
    }
}
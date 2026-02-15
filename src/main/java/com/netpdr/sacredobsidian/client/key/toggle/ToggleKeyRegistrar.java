package com.netpdr.sacredobsidian.client.key.toggle;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ToggleKeyRegistrar {
    public static KeyMapping TOGGLE_DAMAGE_KEY;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        TOGGLE_DAMAGE_KEY = new KeyMapping("key.sacredobsidian.toggle_damage", GLFW.GLFW_KEY_X, "key.categories.sacredobsidian");
        event.register(TOGGLE_DAMAGE_KEY);
    }
}
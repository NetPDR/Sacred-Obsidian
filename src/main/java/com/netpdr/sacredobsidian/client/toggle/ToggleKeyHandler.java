package com.netpdr.sacredobsidian.client.toggle;

import com.netpdr.sacredobsidian.network.ToggleDamagePayload;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class ToggleKeyHandler {

    private static boolean prevDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ToggleKeyRegistrar.TOGGLE_DAMAGE_KEY == null) return;

        boolean down = ToggleKeyRegistrar.TOGGLE_DAMAGE_KEY.isDown();

        // 只在「按下瞬间」触发
        if (!(down && !prevDown)) {
            prevDown = down;
            return;
        }
        prevDown = true;

        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof SacredObsidianItem) {
            PacketDistributor.sendToServer(new ToggleDamagePayload(0));
        }

        ItemStack off = mc.player.getOffhandItem();
        if (off.getItem() instanceof SacredObsidianItem) {
            PacketDistributor.sendToServer(new ToggleDamagePayload(1));
        }
    }
}
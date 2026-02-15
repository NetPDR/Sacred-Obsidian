package com.netpdr.sacredobsidian.client.key.reverse;

import com.netpdr.sacredobsidian.network.ReverseModePayload;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class ReverseKeyHandler {

    private static boolean prevDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ReverseKeyRegistrar.REVERSE_KEY == null) return;

        boolean down = ReverseKeyRegistrar.REVERSE_KEY.isDown();
        if (down == prevDown) return;
        prevDown = down;

        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof SacredObsidianItem) {
            PacketDistributor.sendToServer(new ReverseModePayload(down, 0));
        }

        ItemStack off = mc.player.getOffhandItem();
        if (off.getItem() instanceof SacredObsidianItem) {
            PacketDistributor.sendToServer(new ReverseModePayload(down, 1));
        }
    }
}
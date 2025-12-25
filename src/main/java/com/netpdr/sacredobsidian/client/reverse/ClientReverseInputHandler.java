package com.netpdr.sacredobsidian.client.reverse;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.network.ReverseModePacket;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "netpdrmod", value = Dist.CLIENT)
public class ClientReverseInputHandler {
    private static boolean prevDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // 可能尚未注册 key（例如还没到 RegisterKeyMappingsEvent），检查空指针 // The key may not have been registered yet (for example, RegisterKeyMappingsEvent hasn’t occurred), check for a null pointer.
        if (ReverseKeyRegistrar.REVERSE_KEY == null) return;

        boolean down = ReverseKeyRegistrar.REVERSE_KEY.isDown();
        if (down == prevDown) return; // 只有变化时发包 // Only issue contracts when there are changes
        prevDown = down;

        ItemStack main = mc.player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof SacredObsidianItem) {
            Sacredobsidian.CHANNEL.sendToServer(new ReverseModePacket(down, 0));
        }
        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() instanceof SacredObsidianItem) {
            Sacredobsidian.CHANNEL.sendToServer(new ReverseModePacket(down, 1));
        }
    }
}
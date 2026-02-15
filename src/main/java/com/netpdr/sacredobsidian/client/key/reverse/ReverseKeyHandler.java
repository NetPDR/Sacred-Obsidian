package com.netpdr.sacredobsidian.client.key.reverse;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.network.ReverseModePacket;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, value = Dist.CLIENT)
public class ReverseKeyHandler {
    private static boolean prevDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ReverseKeyRegistrar.REVERSE_KEY == null) return;

        boolean down = ReverseKeyRegistrar.REVERSE_KEY.isDown();
        if (down == prevDown) return;
        prevDown = down;

        // 检查主手/副手是否有 SacredObsidianItem，分别发包 // Check if the main hand/off hand has a Sacred Obsidian Item, and send packets accordingly.
        ItemStack main = mc.player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof SacredObsidianItem) {
            try {
                // 优先用二参构造（hand 区分左右手），若不存在则降级到单参构造 // Prefer using the two-parameter constructor (hand distinguishes left and right hand); if it does not exist, fall back to the single-parameter constructor.
                Sacredobsidian.CHANNEL.sendToServer(new ReverseModePacket(down, 0));
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                Sacredobsidian.CHANNEL.sendToServer(new ReverseModePacket(down));
            }
        }

        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() instanceof SacredObsidianItem) {
            try {
                Sacredobsidian.CHANNEL.sendToServer(new ReverseModePacket(down, 1));
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                Sacredobsidian.CHANNEL.sendToServer(new ReverseModePacket(down));
            }
        }
    }
}
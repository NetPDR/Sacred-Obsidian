package com.netpdr.sacredobsidian.client.key.toggle;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.network.ToggleDamagePacket;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, value = Dist.CLIENT)
public class ToggleKeyHandler {
    private static boolean prevDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ToggleKeyRegistrar.TOGGLE_DAMAGE_KEY == null) return;

        boolean down = ToggleKeyRegistrar.TOGGLE_DAMAGE_KEY.isDown();

        // 只在按下瞬间触发（从未按 -> 按下） // Triggers only at the moment of pressing (from never pressed -> pressed)
        if (!(down && !prevDown)) {
            prevDown = down;
            return;
        }
        prevDown = true;

        // 主手/副手分别检查并发包（与现有 Reverse 实现一致） // Check the packet sending separately for the main hand and offhand (consistent with the existing Reverse implementation)
        ItemStack main = mc.player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof SacredObsidianItem) {
            Sacredobsidian.CHANNEL.sendToServer(new ToggleDamagePacket(0)); // 0 = main hand
        }

        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() instanceof SacredObsidianItem) {
            Sacredobsidian.CHANNEL.sendToServer(new ToggleDamagePacket(1)); // 1 = off hand
        }
    }
}
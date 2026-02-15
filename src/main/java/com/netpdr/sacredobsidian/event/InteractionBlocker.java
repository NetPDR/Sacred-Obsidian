package com.netpdr.sacredobsidian.event;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Sacredobsidian.MODID)
public class InteractionBlocker {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof SacredObsidianItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof SacredObsidianItem) {
            event.setCanceled(true);
        }
    }
}

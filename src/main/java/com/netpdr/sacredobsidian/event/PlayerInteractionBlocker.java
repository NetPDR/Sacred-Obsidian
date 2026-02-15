package com.netpdr.sacredobsidian.event;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID)
public class PlayerInteractionBlocker {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SacredObsidianItem item)) return;

        if (level.isClientSide) return;

        UseOnContext context = new UseOnContext(
                player,
                event.getHand(),
                event.getHitVec()
        );

        InteractionResult result = item.useOn(context);

        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SacredObsidianItem item)) return;
        if (level.isClientSide) return;

        Entity target = event.getTarget();

        if (!(target instanceof LivingEntity living)) return;

        InteractionResult result = item.interactLivingEntity(
                stack,
                player,
                living,
                event.getHand()
        );

        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}

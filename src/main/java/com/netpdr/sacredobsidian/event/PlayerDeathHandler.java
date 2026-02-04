package com.netpdr.sacredobsidian.event;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.data.SacredObsidianData;
import com.netpdr.sacredobsidian.data.SacredObsidianDataComponents;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = Sacredobsidian.MODID)
public class PlayerDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getCommandSenderWorld().isClientSide) return; // only run on server

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SacredObsidianItem) {
                stack.set(SacredObsidianDataComponents.IS_EXTENDING.value(), Boolean.FALSE);
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof SacredObsidianItem) {
                stack.set(SacredObsidianDataComponents.IS_EXTENDING.value(), Boolean.FALSE);
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof SacredObsidianItem) {
                stack.set(SacredObsidianDataComponents.IS_EXTENDING.value(), Boolean.FALSE);
            }
        }

        // 使用 SacredObsidianData 的帮助方法一次性移除该玩家的 owner 记录 // Use the help method of SacredObsidianData to remove the owner record of the player in one go
        if (player.getCommandSenderWorld() instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);
            data.removeAllOwnersFor(player.getUUID());
        }
    }
}
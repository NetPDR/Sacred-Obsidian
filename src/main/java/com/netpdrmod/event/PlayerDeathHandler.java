package com.netpdrmod.event;

import com.netpdrmod.Netpdrmod;
import com.netpdrmod.data.SacredObsidianData;
import com.netpdrmod.weapon.SacredObsidianItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Netpdrmod.MODID)
public class PlayerDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getCommandSenderWorld().isClientSide) return; // 只在服务端处理 // Only handle on the server side

        // 1) 停止物品上的延伸标记（在被掉落前修改 ItemStack 的 NBT） // Stop extending tags on items (modify the NBT of the ItemStack before it is dropped)
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SacredObsidianItem) {
                stack.getOrCreateTag().putBoolean("IsExtending", false);
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof SacredObsidianItem) {
                stack.getOrCreateTag().putBoolean("IsExtending", false);
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof SacredObsidianItem) {
                stack.getOrCreateTag().putBoolean("IsExtending", false);
            }
        }

        // 2) 使用 SacredObsidianData 的帮助方法一次性移除该玩家的 owner 记录 // Use the help method of SacredObsidianData to remove the owner record of the player in one go
        if (player.getCommandSenderWorld() instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);
            data.removeAllOwnersFor(player.getUUID());
        }
    }
}
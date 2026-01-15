package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 定期检查 AncientTomeAnvilHandler.PENDING 中的条目，判断是否已被玩家真实消费并发放成就。 Regularly check the entries in AncientTomeAnvilHandler.PENDING to determine whether they have actually been consumed by players and awarded as achievements.
 */
@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AncientTomePendingCleaner {
    // 检查/清理间隔（tick）：20 tick = 1 秒
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static long tickCounter = 0L;

    // pending 过期阈值（毫秒）
    private static final long EXPIRE_MS = 10_000L;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter % CHECK_INTERVAL_TICKS != 0) return; // 每 20 tick 检查一次 // Check every 20 ticks

        long now = System.currentTimeMillis();

        // 获得服务器实例 // Obtain server instance
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, AncientTomeAnvilHandler.PendingAncientTome>> it =
                AncientTomeAnvilHandler.PENDING.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, AncientTomeAnvilHandler.PendingAncientTome> entry = it.next();
            UUID uuid = entry.getKey();
            AncientTomeAnvilHandler.PendingAncientTome pending = entry.getValue();

            // 过期清理 // Expired Cleanup
            if (now - pending.timestamp > EXPIRE_MS) {
                it.remove();
                continue;
            }

            // 查找对应玩家（遍历在线玩家列表并按 UUID 匹配，跨映射稳健） // Find the corresponding player (traverse the online player list and match by UUID, robust across mappings)
            ServerPlayer player = null;
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                if (sp != null && sp.getUUID().equals(uuid)) {
                    player = sp;
                    break;
                }
            }

            if (player == null) {
                // 玩家离线或不在线：跳过（等待下次检查或到期被清理） // Player offline or not online: Skip (wait for the next check or be cleared upon expiration)
                continue;
            }

            boolean awarded = false;

            // 优先检查玩家物品栏是否已有与预期结果相同的物品（最可靠） // First check if the player's inventory already contains an item identical to the expected result (most reliable)
            for (ItemStack invStack : player.getInventory().items) {
                if (invStack != null && !invStack.isEmpty()) {
                    if (ItemStack.isSameItemSameTags(invStack, pending.expectedResultSnapshot)) {
                        // 发现匹配 -> 发放成就并移除 pending // Match found -> Grant achievement and remove pending
                        awardAdvancement(player);
                        awarded = true;
                        break;
                    }
                }
            }

            if (awarded) {
                it.remove();
                continue;
            }

            // 次优：如果玩家当前正在打开铁砧界面，检查右槽/结果槽状态变化 // Secondary: If the player is currently opening the anvil interface, check for changes in the right slot/result slot
            if (player.containerMenu instanceof AnvilMenu anvilMenu) {
                try {
                    Slot rightSlot = anvilMenu.getSlot(1);   // 右槽 index=1 (vanilla)
                    Slot resultSlot = anvilMenu.getSlot(2);  // 结果槽 index=2 (vanilla)

                    ItemStack rightNow = rightSlot.getItem();
                    ItemStack resultNow = resultSlot.getItem();

                    boolean resultGone = resultNow.isEmpty() || !ItemStack.isSameItemSameTags(resultNow, pending.expectedResultSnapshot);

                    boolean rightChanged = true;
                    if (!rightNow.isEmpty()) {
                        if (ItemStack.isSameItemSameTags(rightNow, pending.rightSlotSnapshot)) {
                            rightChanged = false;
                        }
                    }

                    if (resultGone && rightChanged) {
                        awardAdvancement(player);
                        awarded = true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (awarded) {
                it.remove();
            }
            // 否则保持 pending 等待下一次检查或到期清理 // Otherwise, remain pending and wait for the next check or expiration cleanup
        }
    }

    private static void awardAdvancement(ServerPlayer player) {
        try {
            ResourceLocation advId = new ResourceLocation(Sacredobsidian.MODID, "use_ancient_tome");
            Advancement adv = player.server.getAdvancements().getAdvancement(advId);
            if (adv == null) return;
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            if (progress.isDone()) return;
            for (String crit : progress.getRemainingCriteria()) {
                player.getAdvancements().award(adv, crit);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
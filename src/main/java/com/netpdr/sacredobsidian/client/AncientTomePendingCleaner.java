package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 定期清理 AncientTomeAnvilHandler.PENDING，
 * 判断 Ancient Tome 是否已被真实消耗并发放成就。
 */
@EventBusSubscriber(
        modid = Sacredobsidian.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class AncientTomePendingCleaner {

    /** 检查间隔（tick） */
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static long tickCounter = 0L;

    /** pending 过期时间（毫秒） */
    private static final long EXPIRE_MS = 10_000L;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL_TICKS != 0) return;

        long now = System.currentTimeMillis();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, AncientTomeAnvilHandler.PendingAncientTome>> it =
                AncientTomeAnvilHandler.PENDING.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, AncientTomeAnvilHandler.PendingAncientTome> entry = it.next();
            UUID uuid = entry.getKey();
            AncientTomeAnvilHandler.PendingAncientTome pending = entry.getValue();

            /* 1) 过期清理 */
            if (now - pending.timestamp > EXPIRE_MS) {
                it.remove();
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) continue;

            boolean awarded = false;

            /* 2) 最可靠：物品栏中已出现预期结果 */
            for (ItemStack invStack : player.getInventory().items) {
                if (!invStack.isEmpty()
                        && ItemStack.isSameItemSameComponents(
                        invStack,
                        pending.expectedResultSnapshot
                )) {
                    awardAdvancement(player);
                    awarded = true;
                    break;
                }
            }

            if (awarded) {
                it.remove();
                continue;
            }

            /* 3) 次级：仍在铁砧界面，判断结果槽是否被取走 */
            if (player.containerMenu instanceof AnvilMenu anvilMenu) {
                try {
                    Slot rightSlot = anvilMenu.getSlot(1);
                    Slot resultSlot = anvilMenu.getSlot(2);

                    ItemStack rightNow = rightSlot.getItem();
                    ItemStack resultNow = resultSlot.getItem();

                    boolean resultGone =
                            resultNow.isEmpty()
                                    || !ItemStack.isSameItemSameComponents(
                                    resultNow,
                                    pending.expectedResultSnapshot
                            );

                    boolean rightChanged =
                            rightNow.isEmpty()
                                    || !ItemStack.isSameItemSameComponents(
                                    rightNow,
                                    pending.rightSlotSnapshot
                            );

                    if (resultGone && rightChanged) {
                        awardAdvancement(player);
                        awarded = true;
                    }
                } catch (Throwable ignored) {
                }
            }

            if (awarded) {
                it.remove();
            }
        }
    }

    private static void awardAdvancement(ServerPlayer player) {
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    Sacredobsidian.MODID,
                    "use_ancient_tome"
            );

            AdvancementHolder holder =
                    player.server.getAdvancements().get(id);

            if (holder == null) return;

            AdvancementProgress progress =
                    player.getAdvancements().getOrStartProgress(holder);

            if (progress.isDone()) return;

            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
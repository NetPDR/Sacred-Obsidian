package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.common.AncientTomeCommon;
import com.netpdr.sacredobsidian.item.AncientTomeItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Sacredobsidian.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AncientTomeAnvilHandler {

    public static final Map<UUID, PendingAncientTome> PENDING =
            new ConcurrentHashMap<>();

    public static class PendingAncientTome {
        public final ItemStack expectedResultSnapshot;
        public final ItemStack rightSlotSnapshot;
        public final long timestamp;

        public PendingAncientTome(ItemStack result, ItemStack right) {
            this.expectedResultSnapshot = result.copy();
            this.rightSlotSnapshot = right.copy();
            this.timestamp = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) return;
        if (!(right.getItem() instanceof AncientTomeItem tome)) return;
        event.getPlayer();

        /* ✅ 直接拿 ResourceKey */
        ResourceKey<Enchantment> enchantKey =
                tome.getTargetEnchantmentKey();
        if (enchantKey == null) return;

        var level = event.getPlayer().getCommandSenderWorld();
        var registry = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);

        Holder<Enchantment> holder =
                registry.getHolder(enchantKey).orElse(null);
        if (holder == null) return;

        ItemEnchantments enchants =
                left.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        int currentLevel =
                EnchantmentHelper.getTagEnchantmentLevel(holder, left);

        if (currentLevel >= AncientTomeCommon.MAX_OVERLEVEL) return;

        int newLevel = Math.min(
                currentLevel + 1,
                AncientTomeCommon.MAX_OVERLEVEL
        );

        ItemStack result = left.copy();

        ItemEnchantments.Mutable mutable =
                new ItemEnchantments.Mutable(enchants);

        mutable.set(holder, newLevel);

        result.set(
                DataComponents.ENCHANTMENTS,
                mutable.toImmutable()
        );

        event.setOutput(result);
        event.setCost(AncientTomeCommon.XP_COST);
        event.setMaterialCost(1);

        if (event.getPlayer() instanceof ServerPlayer sp) {
            PENDING.put(
                    sp.getUUID(),
                    new PendingAncientTome(result, right)
            );
        }
    }
}
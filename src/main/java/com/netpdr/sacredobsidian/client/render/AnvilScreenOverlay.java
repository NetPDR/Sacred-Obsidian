package com.netpdr.sacredobsidian.client.render;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.common.AncientTomeCommon;
import com.netpdr.sacredobsidian.item.AncientTomeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(
        modid = Sacredobsidian.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public class AnvilScreenOverlay {

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AnvilScreen screen)) return;

        Slot left = screen.getMenu().getSlot(0);
        Slot right = screen.getMenu().getSlot(1);

        ItemStack base = left.getItem();
        ItemStack tomeStack = right.getItem();

        if (base.isEmpty() || tomeStack.isEmpty()) return;
        if (!(tomeStack.getItem() instanceof AncientTomeItem tome)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        /* =========================================================
         * 1) 从 AncientTomeItem 拿 ResourceKey
         * ========================================================= */
        ResourceKey<Enchantment> enchantKey =
                tome.getTargetEnchantmentKey();
        if (enchantKey == null) return;

        /* =========================================================
         * 2) 客户端 registry -> Holder<Enchantment>
         * ========================================================= */
        var registry = mc.level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);

        var optHolder = registry.getHolder(enchantKey);
        if (optHolder.isEmpty()) return;

        Holder<Enchantment> holder = optHolder.get();
        Enchantment enchantment = holder.value();

        /* =========================================================
         * 3) 当前等级（基于 Holder）
         * ========================================================= */
        int current =
                EnchantmentHelper.getTagEnchantmentLevel(
                        holder, base
                );

        /* =========================================================
         * 4) 决定是否显示提示文本
         * ========================================================= */
        Component text;

        if (current >= AncientTomeCommon.MAX_OVERLEVEL) {
            text = Component.translatable(
                    "gui.sacredobsidian.anvil_at_max"
            );
        } else {
            int vanillaMax = enchantment.getMaxLevel();
            int next = current + 1;

            if (next > vanillaMax) {
                text = Component.translatable(
                        "gui.sacredobsidian.anvil_over_limit",
                        current, next, vanillaMax
                );
            } else {
                return; // 没突破原版上限，不显示
            }
        }

        /* =========================================================
         * 5) 渲染
         * ========================================================= */
        GuiGraphics gg = event.getGuiGraphics();
        Font font = mc.font;

        int baseX = screen.getGuiLeft() + 60;
        int baseY = screen.getGuiTop() + 38;
        float scale = 0.8f;

        gg.pose().pushPose();
        gg.pose().translate(baseX, baseY, 0);
        gg.pose().scale(scale, scale, 1);

        gg.drawString(font, text, 0, 0, 0xFF4444, false);

        gg.pose().popPose();
    }
}
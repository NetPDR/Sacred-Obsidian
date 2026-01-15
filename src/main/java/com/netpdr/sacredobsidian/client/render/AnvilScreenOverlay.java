package com.netpdr.sacredobsidian.client.render;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.common.AncientTomeCommon;
import com.netpdr.sacredobsidian.item.AncientTomeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Sacredobsidian.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
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

        Enchantment enchantment = tome.getTargetEnchantment();
        int current = EnchantmentHelper.getTagEnchantmentLevel(enchantment, base);

        GuiGraphics gg = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int baseX = screen.getGuiLeft() + 60;
        int baseY = screen.getGuiTop() + 38; // 向下移动 // Move down
        float scale = 0.8f;

        Component text;

        if (current >= AncientTomeCommon.MAX_OVERLEVEL) {
            text = Component.translatable("gui.sacredobsidian.anvil_at_max");
        } else {
            int vanillaMax = enchantment.getMaxLevel();
            int next = current + 1;

            if (next > vanillaMax) {
                text = Component.translatable(
                        "gui.sacredobsidian.anvil_over_limit",
                        current, next, vanillaMax
                );
            } else {
                return; // 没突破，不提示 // No breakthrough, no notification
            }
        }

        gg.pose().pushPose();
        gg.pose().translate(baseX, baseY, 0);
        gg.pose().scale(scale, scale, 1);

        gg.drawString(font, text, 0, 0, 0xFF4444, false);

        gg.pose().popPose();
    }
}
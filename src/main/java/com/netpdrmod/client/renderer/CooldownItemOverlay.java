package com.netpdrmod.client.renderer;

import com.netpdrmod.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "netpdrmod", value = Dist.CLIENT)
public class CooldownItemOverlay {

    // 在游戏 HUD 上渲染（热键栏、副手） // Rendering on the game HUD (hotbar bar, off-hand)
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        GuiGraphics gg = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // 热键栏 9 个槽位 // Hotkey bar 9 slots
        for (int slot = 0; slot < 9; slot++) {
            int x = screenW / 2 - 90 + slot * 20 + 2;
            int y = screenH - 16 - 3;
            renderCooldownForSlot(gg, player, slot, x, y);
        }

        // 副手槽（原版在热键栏左侧或右侧，取决于主手设置） // Off-hand slot (original on the left or right side of the hotbar depending on the main hand setting)
        int offhandX;
        int offhandY = screenH - 16 - 3;
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            // 副手在左侧 // The deputy is on the left
            offhandX = screenW / 2 - 91 - 26;
        } else {
            // 副手在右侧 // The deputy is on the right side
            offhandX = screenW / 2 + 91 + 10;
        }
        renderCooldownForSlot(gg, player, 40, offhandX, offhandY);
    }

    // 在容器界面里渲染（自动兼容扩容背包） // Render in the container interface (automatically compatible with the expansion backpack)
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        GuiGraphics gg = event.getGuiGraphics();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            if (!(stack.getItem() instanceof SacredObsidianItem)) continue;

            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            renderCooldownForStack(gg, player, stack, x, y);
        }
    }

    private static void renderCooldownForSlot(GuiGraphics gg, Player player, int slot, int x, int y) {
        ItemStack stack = player.getInventory().getItem(slot);
        renderCooldownForStack(gg, player, stack, x, y);
    }

    private static void renderCooldownForStack(GuiGraphics gg, Player player, ItemStack stack, int x, int y) {
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        CompoundTag tag = stack.getOrCreateTag();
        long lastUse = tag.getLong("LastUseTime");
        long current = player.level().getGameTime();

        int cooldownTicks = SacredObsidianItem.COOLDOWN_TIME;
        long passed = current - lastUse;

        if (passed < cooldownTicks) {
            float progress = 1.0F - (passed / (float) cooldownTicks);
            renderCooldownOverlay(gg, x, y, progress);
        }
    }

    private static void renderCooldownOverlay(GuiGraphics gg, int x, int y, float progress) {
        int height = (int) (16 * progress);
        // 黑色 + 半透明 (0x64 alpha = ~40% 透明度)  // Black + Translucent (0x64 alpha = ~40% transparency)
        int color = 0x64000000;
        gg.fill(x, y + (16 - height), x + 16, y + 16, color);
    }
}
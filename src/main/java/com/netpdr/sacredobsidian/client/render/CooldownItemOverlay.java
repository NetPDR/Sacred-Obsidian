package com.netpdr.sacredobsidian.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "sacredobsidian", value = Dist.CLIENT)
public final class CooldownItemOverlay {

    private static final ResourceLocation COOLDOWN =
            new ResourceLocation("sacredobsidian", "textures/gui/cooldown.png");

    /* ---------------- HUD（热键栏 + 副手） ---------------- */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        GuiGraphics gg = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        long now = player.level().getGameTime();

        for (int slot = 0; slot < 9; slot++) {
            int x = screenW / 2 - 90 + slot * 20 + 2;
            int y = screenH - 16 - 3;
            renderCooldown(gg, player.getInventory().getItem(slot), x, y, now);
        }

        int offhandY = screenH - 16 - 3;
        int offhandX = (player.getMainArm() == HumanoidArm.RIGHT)
                ? screenW / 2 - 91 - 26
                : screenW / 2 + 91 + 10;

        renderCooldown(gg, player.getInventory().getItem(40), offhandX, offhandY, now);
    }

    /* ---------------- 容器界面（尽量使用同一套渲染） ---------------- */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        GuiGraphics gg = event.getGuiGraphics();
        long now = player.level().getGameTime();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            renderCooldown(gg, stack, x, y, now);
        }
    }

    /* ---------------- 核心渲染逻辑（HUD + 容器均用） ---------------- */
    private static void renderCooldown(
            GuiGraphics gg,
            ItemStack stack,
            int x,
            int y,
            long now
    ) {
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("LastUseTime")) return;

        long start = tag.getLong("LastUseTime");
        int cooldown = tag.contains("Cooldown")
                ? tag.getInt("Cooldown")
                : SacredObsidianItem.COOLDOWN_TIME;

        long end = start + cooldown;
        if (now >= end) return;

        float progress = Mth.clamp((end - now) / (float) cooldown, 0.0F, 1.0F);
        int height = (int) (16 * progress);

        // Push pose, move to foreground, bind GUI shader and texture, ensure blending
        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 200.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, COOLDOWN);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Disable depth test to ensure overlay sits on top of item models
        RenderSystem.disableDepthTest();

        gg.blit(
                COOLDOWN,
                x,
                y + (16 - height),
                0,
                16 - height,
                16,
                height,
                16,
                16
        );

        // Restore depth and blend and pose
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        gg.pose().popPose();
    }
}
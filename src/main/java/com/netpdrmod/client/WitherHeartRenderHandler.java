package com.netpdrmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.netpdrmod.registry.ModEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class WitherHeartRenderHandler {

    // 定义心形图标的资源路径 // Define the resource path for the heart icon
    private static final ResourceLocation HEARTS_TEXTURE = new ResourceLocation("minecraft", "textures/gui/icons.png");

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        // 确保玩家和游戏模式存在 // Ensure the player and game mode exist
        if (player != null && Minecraft.getInstance().gameMode != null) {
            GameType gameMode = Minecraft.getInstance().gameMode.getPlayerMode();

            // 只在生存和冒险模式下渲染彩色心 // Only render colorful hearts in Survival and Adventure modes
            if (gameMode == GameType.CREATIVE || gameMode == GameType.SPECTATOR) {
                return; // 如果是创造模式或旁观者模式，跳过渲染 // If the player is in Creative or Spectator mode, skip rendering
            }

            // 检查玩家是否受到凋零效果 // Check if the player is affected by Wither effect
            if (player.hasEffect(ModEffect.IRRECONCILABLE_CRACK.get())) {
                event.setCanceled(true);
                renderDynamicRainbowHearts(event.getGuiGraphics(), player);
            }
        }
    }

    private static void renderDynamicRainbowHearts(GuiGraphics guiGraphics, LocalPlayer player) {
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();
        int absorption = (int) player.getAbsorptionAmount();

        RenderSystem.setShaderTexture(0, HEARTS_TEXTURE);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int baseX = screenWidth / 2 - 91;
        int baseY = screenHeight - 39;

        int heartsPerRow = 10;

        // 使用时间生成动态颜色 // Generate dynamic color using time
        float time = (System.currentTimeMillis() % 10000L) / 10000.0F; // 使用时间生成色调 // Generate color using time
        float red = (float) Math.sin(time * 2 * Math.PI) * 0.5F + 0.5F; // 红色分量 // Red component
        float green = (float) Math.sin(time * 2 * Math.PI + (2 * Math.PI / 3)) * 0.5F + 0.5F; // 绿色分量 // Green component
        float blue = (float) Math.sin(time * 2 * Math.PI + (4 * Math.PI / 3)) * 0.5F + 0.5F; // 蓝色分量 // Blue component

        // 渲染普通心形 // Render regular hearts
        for (int i = 0; i < (maxHealth + 1) / 2; i++) {
            int x = baseX + (i % heartsPerRow) * 8;
            int y = baseY - (i / heartsPerRow) * 10;

            RenderSystem.setShaderColor(red, green, blue, 1.0F); // 设置动态颜色

            if (i * 2 + 1 < health) {
                guiGraphics.blit(HEARTS_TEXTURE, x, y, 16, 0, 9, 9); // 完整心形 // Full heart
            } else if (i * 2 + 1 == health) {
                guiGraphics.blit(HEARTS_TEXTURE, x, y, 25, 0, 9, 9); // 半颗心形 // Half heart
            } else {
                guiGraphics.blit(HEARTS_TEXTURE, x, y, 34, 0, 9, 9); // 空心形 // Empty heart
            }
        }

        // 吸收心 // Absorption hearts
        for (int i = 0; i < (absorption + 1) / 2; i++) {
            int x = baseX + ((i + (maxHealth + 1) / 2) % heartsPerRow) * 8;
            int y = baseY - ((i + (maxHealth + 1) / 2) / heartsPerRow) * 10;

            RenderSystem.setShaderColor(red, green, blue, 1.0F); // 使用相同的动态颜色 // Use the same dynamic color

            if (i * 2 + 1 < absorption) {
                guiGraphics.blit(HEARTS_TEXTURE, x, y, 160, 0, 9, 9); // 完整吸收心 // Render full absorption heart
            } else {
                guiGraphics.blit(HEARTS_TEXTURE, x, y, 169, 0, 9, 9); // 半颗吸收心 // Half absorption heart
            }
        }

        // 重置颜色 // Reset color
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
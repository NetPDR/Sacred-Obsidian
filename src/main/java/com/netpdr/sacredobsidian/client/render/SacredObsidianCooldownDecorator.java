package com.netpdr.sacredobsidian.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.netpdr.sacredobsidian.data.SacredObsidianDataComponents;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import org.jetbrains.annotations.NotNull;

public class SacredObsidianCooldownDecorator implements IItemDecorator {

    private static final ResourceLocation COOLDOWN =
            ResourceLocation.fromNamespaceAndPath("sacredobsidian", "textures/gui/cooldown.png");

    @Override
    public boolean render(
            @NotNull GuiGraphics gg,
            @NotNull Font font,
            ItemStack stack,
            int x,
            int y
    ) {
        if (!(stack.getItem() instanceof SacredObsidianItem)) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        long startTick = stack.getOrDefault(
                SacredObsidianDataComponents.LAST_USE_TICK.get(), -1L
        );
        if (startTick < 0) return false;

        int cooldown = stack.getOrDefault(
                SacredObsidianDataComponents.COOLDOWN_TICKS.get(),
                SacredObsidianItem.COOLDOWN_TIME
        );

        long now = player.getCommandSenderWorld().getGameTime();
        long end = startTick + cooldown;
        if (now >= end) return false;

        float progress = Mth.clamp(
                (end - now) / (float) cooldown,
                0F, 1F
        );

        int height = (int) (16 * progress);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

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

        RenderSystem.disableBlend();

        return true;
    }
}
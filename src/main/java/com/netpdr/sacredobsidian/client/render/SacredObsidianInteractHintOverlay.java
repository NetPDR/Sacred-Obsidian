package com.netpdr.sacredobsidian.client.render;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(
        modid = Sacredobsidian.MODID,
        value = Dist.CLIENT
)

public class SacredObsidianInteractHintOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        if (!canInteract(player, hit)) return;

        renderHint(event);
    }

    /* ---------- 判断逻辑 ---------- */

    private static boolean canInteract(Player player, HitResult hit) {
        if (hit instanceof BlockHitResult bhr) {
            return canBlockInteract(player, bhr);
        }
        if (hit instanceof EntityHitResult ehr) {
            return canEntityInteract(ehr.getEntity());
        }
        return false;
    }

    private static boolean canBlockInteract(Player player, BlockHitResult bhr) {
        BlockState state = player.getCommandSenderWorld().getBlockState(bhr.getBlockPos());
        return state.getBlock() instanceof EntityBlock
                || state.hasBlockEntity();
    }
    
    private static boolean canEntityInteract(Entity entity) {
        return entity instanceof net.minecraft.world.entity.npc.AbstractVillager
                || entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse
                || entity instanceof net.minecraft.world.entity.item.ItemEntity
                || entity instanceof net.minecraft.world.entity.vehicle.ContainerEntity
                || entity instanceof net.minecraft.world.entity.player.Player;
    }

    /* ---------- HUD ---------- */

    private static void renderHint(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        String keyName = KeyMapping.createNameSupplier(
                "key.sacredobsidian.vanilla_interact"
        ).get().getString();

        Component text = Component.translatable(
                "hud.sacredobsidian.interact",
                Component.literal(keyName).withStyle(ChatFormatting.YELLOW)
        ).withStyle(ChatFormatting.YELLOW);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = (screenWidth - font.width(text)) / 2;
        int y = screenHeight - 60;

        graphics.drawString(
                font,
                text,
                x,
                y,
                0xFFFF55,
                true
        );
    }
}
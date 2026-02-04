package com.netpdr.sacredobsidian.network;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.data.SacredObsidianDataComponents;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ToggleDamagePayload(int handIndex)
        implements CustomPacketPayload {

    public static final Type<ToggleDamagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Sacredobsidian.MODID, "toggle_damage"));

    public static final StreamCodec<FriendlyByteBuf, ToggleDamagePayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeByte(pkt.handIndex),
                    buf -> new ToggleDamagePayload(buf.readByte())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /* ================= server ================= */

    public static void handle(ToggleDamagePayload pkt, ServerPlayer player) {
        int idx = Math.max(0, Math.min(1, pkt.handIndex));
        InteractionHand hand =
                idx == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        boolean current =
                stack.getOrDefault(
                        SacredObsidianDataComponents.DAMAGE_ENABLED.get(),
                        true // 默认允许伤害
                );

        boolean next = !current;

        stack.set(
                SacredObsidianDataComponents.DAMAGE_ENABLED.get(),
                next
        );

        player.setItemInHand(hand, stack);
        player.containerMenu.broadcastChanges();

        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        next
                                ? "message.sacred_obsidian.damage_on"
                                : "message.sacred_obsidian.damage_off"
                ),
                true
        );
    }
}
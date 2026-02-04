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

public record ReverseModePayload(boolean down, int handIndex)
        implements CustomPacketPayload {

    public static final Type<ReverseModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Sacredobsidian.MODID, "reverse_mode"));

    public static final StreamCodec<FriendlyByteBuf, ReverseModePayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeBoolean(pkt.down);
                        buf.writeByte(pkt.handIndex);
                    },
                    buf -> new ReverseModePayload(
                            buf.readBoolean(),
                            buf.readByte()
                    )
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /* ================= server ================= */

    public static void handle(ReverseModePayload pkt, ServerPlayer player) {
        if (pkt.handIndex == -1) {
            handleHand(player, InteractionHand.MAIN_HAND, pkt.down);
            handleHand(player, InteractionHand.OFF_HAND, pkt.down);
        } else {
            InteractionHand hand =
                    pkt.handIndex == 0
                            ? InteractionHand.MAIN_HAND
                            : InteractionHand.OFF_HAND;
            handleHand(player, hand, pkt.down);
        }
    }

    private static void handleHand(ServerPlayer player, InteractionHand hand, boolean down) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        // 必须处于第二形态 + 正在延伸
        if (!stack.getOrDefault(SacredObsidianDataComponents.SECOND_FORM.get(), false))
            return;
        if (!stack.getOrDefault(SacredObsidianDataComponents.IS_EXTENDING.get(), false))
            return;

        // 写入 DataComponent
        stack.set(
                SacredObsidianDataComponents.REVERSE_MODE.get(),
                down
        );

        player.setItemInHand(hand, stack);
        player.containerMenu.broadcastChanges();
    }
}
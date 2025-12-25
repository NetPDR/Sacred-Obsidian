package com.netpdr.sacredobsidian.network;

import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReverseModePacket {
    private final boolean down;
    private final int handIndex; // 0 = main hand, 1 = offhand

    public ReverseModePacket(boolean down, int handIndex) {
        this.down = down;
        this.handIndex = handIndex;
    }

    public ReverseModePacket(boolean down) {
        this(down, -1); // -1 表示同时处理两只手（兼容旧客户端） // -1 indicates processing both hands simultaneously (compatible with older clients)
    }

    public static void encode(ReverseModePacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.down);
        buf.writeByte(pkt.handIndex);
    }

    public static ReverseModePacket decode(FriendlyByteBuf buf) {
        boolean down = buf.readBoolean();
        int hand = buf.readByte();
        return new ReverseModePacket(down, hand);
    }

    public static void handle(ReverseModePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // handIndex == -1 -> 兼容：检查两只手 // handIndex == -1 -> Compatibility: Check both hands
            if (pkt.handIndex == -1) {
                handleForHand(sender, InteractionHand.MAIN_HAND, pkt.down);
                handleForHand(sender, InteractionHand.OFF_HAND, pkt.down);
            } else {
                // 防御：只接受 0/1 // Defense: Only accepts 0/1
                int idx = Math.max(0, Math.min(1, pkt.handIndex));
                InteractionHand hand = (idx == 0) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                handleForHand(sender, hand, pkt.down);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleForHand(ServerPlayer player, InteractionHand hand, boolean down) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        CompoundTag tag = stack.getOrCreateTag();
        // 只有在第二形态并且正在延伸时才允许写 ReverseMode // ReverseMode is only allowed when in the second form and extending.
        if (!tag.getBoolean("SecondForm") || !tag.getBoolean("IsExtending")) return;

        tag.putBoolean("ReverseMode", down);

        // 把写好的 ItemStack 放回相应手位以确保服务器-客户端能同步 // Put the completed ItemStack back into the corresponding hand slot to ensure server-client synchronization.
        player.setItemInHand(hand, stack);

        // broadcastChanges 在 ServerPlayer 上总是可用，调用以确保变化广播到客户端（如果必要） // broadcastChanges is always available on ServerPlayer and is called to ensure changes are broadcast to the client (if necessary)
        player.containerMenu.broadcastChanges();
    }
}
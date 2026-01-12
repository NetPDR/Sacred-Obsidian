package com.netpdr.sacredobsidian.network;

import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ToggleDamagePacket {
    private final int handIndex; // 0 = main, 1 = offhand

    public ToggleDamagePacket(int handIndex) {
        this.handIndex = handIndex;
    }

    public static void encode(ToggleDamagePacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.handIndex);
    }

    public static ToggleDamagePacket decode(FriendlyByteBuf buf) {
        int hand = buf.readByte();
        return new ToggleDamagePacket(hand);
    }

    public static void handle(ToggleDamagePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            int idx = Math.max(0, Math.min(1, pkt.handIndex));
            InteractionHand hand = (idx == 0) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            ItemStack stack = sender.getItemInHand(hand);
            if (stack.isEmpty()) return;
            if (!(stack.getItem() instanceof SacredObsidianItem)) return;

            CompoundTag tag = stack.getOrCreateTag();
            // 切换：若无值则视为 true -> 切为 false；若有则取反 // Toggle: If there is no value, it is considered true -> switch to false; if there is a value, invert it
            boolean current = !tag.contains(SacredObsidianItem.TAG_DAMAGE_ENABLED) || tag.getBoolean(SacredObsidianItem.TAG_DAMAGE_ENABLED);
            boolean next = !current;
            tag.putBoolean(SacredObsidianItem.TAG_DAMAGE_ENABLED, next);

            // 把写好的 ItemStack 放回以保证服务器-客户端同步 // Put the completed ItemStack back to ensure server-client synchronization
            sender.setItemInHand(hand, stack);

            // 广播容器变化以确保 sync（和你 reverse 包一样） // Broadcast container changes to ensure sync (just like your reverse package)
            sender.containerMenu.broadcastChanges();

            // 给玩家提示 // Give the player a hint
            sender.displayClientMessage(Component.translatable(next ? "message.sacred_obsidian.damage_on" : "message.sacred_obsidian.damage_off"), true);
        });
        ctx.setPacketHandled(true);
    }
}
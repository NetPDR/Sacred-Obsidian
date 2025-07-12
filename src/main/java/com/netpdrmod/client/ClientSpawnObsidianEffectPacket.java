package com.netpdrmod.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSpawnObsidianEffectPacket(Vec3 start, Vec3 target, boolean followPlayer) {

    public static void encode(ClientSpawnObsidianEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.start.x);
        buf.writeDouble(msg.start.y);
        buf.writeDouble(msg.start.z);

        buf.writeDouble(msg.target.x);
        buf.writeDouble(msg.target.y);
        buf.writeDouble(msg.target.z);

        buf.writeBoolean(msg.followPlayer);
    }

    public static ClientSpawnObsidianEffectPacket decode(FriendlyByteBuf buf) {
        Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        boolean followPlayer = buf.readBoolean();
        return new ClientSpawnObsidianEffectPacket(start, target, followPlayer);
    }

    public static void handle(ClientSpawnObsidianEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.followPlayer) {
                ClientObsidianManager.INSTANCE.spawnAtFollowPlayer(msg.start());
            } else {
                ClientObsidianManager.INSTANCE.spawnAt(msg.start(), msg.target());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
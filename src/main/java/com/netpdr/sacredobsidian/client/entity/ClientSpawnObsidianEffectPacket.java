package com.netpdr.sacredobsidian.client.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 新的 packet：
 * - start: 生成位置（方块中心）
 * - ownerUUID: 该伪实体归属玩家的 UUID（服务端确定）
 * - ownerPosSnapshot: 服务端在发送时抓取的 owner 位置快照（客户端可用作初始目标）
 * 客户端接收后调用 ClientObsidianManager.spawnWithOwner(...)
 */
public record ClientSpawnObsidianEffectPacket(Vec3 start, UUID ownerUUID, Vec3 ownerPosSnapshot) {

    public static void encode(ClientSpawnObsidianEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.start.x);
        buf.writeDouble(msg.start.y);
        buf.writeDouble(msg.start.z);

        buf.writeUUID(msg.ownerUUID);

        buf.writeDouble(msg.ownerPosSnapshot.x);
        buf.writeDouble(msg.ownerPosSnapshot.y);
        buf.writeDouble(msg.ownerPosSnapshot.z);
    }

    public static ClientSpawnObsidianEffectPacket decode(FriendlyByteBuf buf) {
        Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        UUID ownerUUID = buf.readUUID();
        Vec3 ownerPosSnapshot = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new ClientSpawnObsidianEffectPacket(start, ownerUUID, ownerPosSnapshot);
    }

    public static void handle(ClientSpawnObsidianEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientObsidianManager.INSTANCE.spawnWithOwner(msg.start(), msg.ownerUUID(), msg.ownerPosSnapshot()));
        ctx.get().setPacketHandled(true);
    }
}
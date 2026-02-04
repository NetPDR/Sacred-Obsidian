package com.netpdr.sacredobsidian.client.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ClientSpawnObsidianEffectPacket(
        Vec3 start,
        UUID ownerUUID,
        Vec3 ownerPosSnapshot
) implements CustomPacketPayload {

    /* ================= Packet Type ================= */

    public static final Type<ClientSpawnObsidianEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("sacredobsidian", "client_spawn_obsidian_effect"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /* ================= Codec ================= */

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientSpawnObsidianEffectPacket> CODEC =
            StreamCodec.of(
                    // encode
                    (buf, msg) -> {
                        buf.writeDouble(msg.start.x);
                        buf.writeDouble(msg.start.y);
                        buf.writeDouble(msg.start.z);

                        buf.writeUUID(msg.ownerUUID);

                        buf.writeDouble(msg.ownerPosSnapshot.x);
                        buf.writeDouble(msg.ownerPosSnapshot.y);
                        buf.writeDouble(msg.ownerPosSnapshot.z);
                    },
                    // decode
                    buf -> {
                        Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                        UUID ownerUUID = buf.readUUID();
                        Vec3 ownerPosSnapshot = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                        return new ClientSpawnObsidianEffectPacket(start, ownerUUID, ownerPosSnapshot);
                    }
            );

    /* ================= Handler ================= */

    public static void handle(ClientSpawnObsidianEffectPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientObsidianManager.INSTANCE.spawnWithOwner(
                msg.start(),
                msg.ownerUUID(),
                msg.ownerPosSnapshot()
        ));
    }
}
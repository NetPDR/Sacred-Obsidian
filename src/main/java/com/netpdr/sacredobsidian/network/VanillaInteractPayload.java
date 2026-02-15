package com.netpdr.sacredobsidian.network;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.data.SacredObsidianDataComponents;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public record VanillaInteractPayload(
        TargetType targetType,
        BlockPos blockPos,
        Direction face,
        Vec3 hitLocation,
        int entityId
) implements CustomPacketPayload {

    /* ---------- 类型 ---------- */

    public enum TargetType {
        BLOCK,
        ENTITY
    }

    public static final Type<VanillaInteractPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Sacredobsidian.MODID, "vanilla_interact"));

    public static final StreamCodec<FriendlyByteBuf, VanillaInteractPayload> CODEC =
            StreamCodec.of(
                    VanillaInteractPayload::encode,
                    VanillaInteractPayload::decode
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /* ---------- 工厂（客户端） ---------- */

    public static VanillaInteractPayload fromHitResult(HitResult hit) {

        if (hit instanceof BlockHitResult bhr) {
            return new VanillaInteractPayload(
                    TargetType.BLOCK,
                    bhr.getBlockPos(),
                    bhr.getDirection(),
                    bhr.getLocation(),
                    -1
            );
        }

        if (hit instanceof EntityHitResult ehr) {
            return new VanillaInteractPayload(
                    TargetType.ENTITY,
                    null,
                    null,
                    null,
                    ehr.getEntity().getId()
            );
        }

        throw new IllegalArgumentException("Unsupported HitResult: " + hit);
    }

    /* ---------- Codec ---------- */

    private static void encode(FriendlyByteBuf buf, VanillaInteractPayload pkt) {
        buf.writeEnum(pkt.targetType);

        if (pkt.targetType == TargetType.BLOCK) {
            buf.writeBlockPos(pkt.blockPos);
            buf.writeEnum(pkt.face);
            buf.writeDouble(pkt.hitLocation.x);
            buf.writeDouble(pkt.hitLocation.y);
            buf.writeDouble(pkt.hitLocation.z);
        } else {
            buf.writeInt(pkt.entityId);
        }
    }

    private static VanillaInteractPayload decode(FriendlyByteBuf buf) {
        TargetType type = buf.readEnum(TargetType.class);

        return switch (type) {
            case BLOCK -> {
                BlockPos pos = buf.readBlockPos();
                Direction face = buf.readEnum(Direction.class);
                Vec3 hitLoc = new Vec3(
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble()
                );
                yield new VanillaInteractPayload(
                        type,
                        pos,
                        face,
                        hitLoc,
                        -1
                );
            }
            case ENTITY -> new VanillaInteractPayload(
                    type,
                    null,
                    null,
                    null,
                    buf.readInt()
            );
        };
    }

    /* ---------- Server ---------- */

    public static void handle(VanillaInteractPayload pkt, ServerPlayer player) {

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SacredObsidianItem)) return;

        // 标记：本次为“原版交互”
        stack.set(SacredObsidianDataComponents.VANILLA_INTERACTING.get(), true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        try {
            switch (pkt.targetType) {
                case BLOCK -> handleBlock(player.serverLevel(), player, pkt);
                case ENTITY -> handleEntity(player.serverLevel(), player, pkt);
            }
        } finally {
            // 清理标记
            stack.remove(SacredObsidianDataComponents.VANILLA_INTERACTING.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        }
    }

    /* ---------- 方块交互 ---------- */

    private static void handleBlock(ServerLevel level,
                                    ServerPlayer player,
                                    VanillaInteractPayload pkt) {

        BlockPos pos = pkt.blockPos();
        if (!level.isLoaded(pos)) return;

        BlockHitResult hit = new BlockHitResult(
                pkt.hitLocation(),
                pkt.face(),
                pos,
                false
        );

        // 保存原物品
        ItemStack original = player.getMainHandItem();

        // 切换为空手
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        try {
            // 这一步才是真正的“原版右键方块”
            player.gameMode.useItemOn(
                    player,
                    level,
                    ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND,
                    hit
            );
        } finally {
            // 还原 SacredObsidianItem
            player.setItemInHand(InteractionHand.MAIN_HAND, original);
        }
    }

    /* ---------- 实体交互 ---------- */

    private static void handleEntity(ServerLevel level,
                                     ServerPlayer player,
                                     VanillaInteractPayload pkt) {

        Entity entity = level.getEntity(pkt.entityId);
        if (entity == null) return;

        entity.interact(player, InteractionHand.MAIN_HAND);
    }
}
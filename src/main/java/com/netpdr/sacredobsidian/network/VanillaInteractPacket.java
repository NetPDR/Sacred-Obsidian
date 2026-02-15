package com.netpdr.sacredobsidian.network;

import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VanillaInteractPacket {

    public enum TargetType {
        BLOCK,
        ENTITY
    }

    private final TargetType type;

    // Block
    private final BlockPos blockPos;
    private final Direction face;

    // Entity
    private final int entityId;

    /* ---------- 构造 ---------- */

    private VanillaInteractPacket(
            TargetType type,
            BlockPos blockPos,
            Direction face,
            int entityId
    ) {
        this.type = type;
        this.blockPos = blockPos;
        this.face = face;
        this.entityId = entityId;
    }

    /* ---------- 工厂 ---------- */

    public static VanillaInteractPacket fromHitResult(HitResult hit) {
        if (hit instanceof BlockHitResult bhr) {
            return new VanillaInteractPacket(
                    TargetType.BLOCK,
                    bhr.getBlockPos(),
                    bhr.getDirection(),
                    -1
            );
        }

        if (hit instanceof EntityHitResult ehr) {
            return new VanillaInteractPacket(
                    TargetType.ENTITY,
                    null,
                    null,
                    ehr.getEntity().getId()
            );
        }

        throw new IllegalArgumentException("Unsupported HitResult: " + hit);
    }

    public static void encode(VanillaInteractPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);

        if (msg.type == TargetType.BLOCK) {
            buf.writeBlockPos(msg.blockPos);
            buf.writeEnum(msg.face);
        } else {
            buf.writeInt(msg.entityId);
        }
    }

    public static VanillaInteractPacket decode(FriendlyByteBuf buf) {
        TargetType type = buf.readEnum(TargetType.class);

        return switch (type) {
            case BLOCK -> new VanillaInteractPacket(
                    type,
                    buf.readBlockPos(),
                    buf.readEnum(Direction.class),
                    -1
            );
            case ENTITY -> new VanillaInteractPacket(
                    type,
                    null,
                    null,
                    buf.readInt()
            );
        };
    }

    public static void handle(VanillaInteractPacket msg,
                              Supplier<NetworkEvent.Context> ctx) {

        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof SacredObsidianItem)) return;

            ServerLevel level = player.serverLevel();

            switch (msg.type) {
                case BLOCK -> handleBlock(level, player, msg);
                case ENTITY -> handleEntity(level, player, msg);
            }
        });

        ctx.get().setPacketHandled(true);
    }

    private static void handleBlock(ServerLevel level,
                                    ServerPlayer player,
                                    VanillaInteractPacket msg) {

        BlockPos pos = msg.blockPos;
        if (!level.isLoaded(pos)) return;

        BlockState state = level.getBlockState(pos);

        BlockHitResult hit = new BlockHitResult(
                player.getEyePosition(),
                msg.face,
                pos,
                false
        );

        state.use(level, player, InteractionHand.MAIN_HAND, hit);
    }

    private static void handleEntity(ServerLevel level,
                                     ServerPlayer player,
                                     VanillaInteractPacket msg) {

        Entity entity = level.getEntity(msg.entityId);
        if (entity == null) return;

        entity.interact(player, InteractionHand.MAIN_HAND);
    }
}
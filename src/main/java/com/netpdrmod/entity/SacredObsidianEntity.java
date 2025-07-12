package com.netpdrmod.entity;

import com.netpdrmod.registry.ModEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SacredObsidianEntity extends Entity {

    private Player owner;  // 吸引的目标玩家 // The player who owns the entity
    private UUID ownerUUID;  // 玩家UUID用于保存玩家信息 // UUID of the player to store the player's information

    //private static final EntityDataAccessor<Float> TARGET_X =
            //SynchedEntityData.defineId(SacredObsidianEntity.class, EntityDataSerializers.FLOAT);
    //private static final EntityDataAccessor<Float> TARGET_Y =
            //SynchedEntityData.defineId(SacredObsidianEntity.class, EntityDataSerializers.FLOAT);
    //private static final EntityDataAccessor<Float> TARGET_Z =
            //SynchedEntityData.defineId(SacredObsidianEntity.class, EntityDataSerializers.FLOAT);

    //private boolean positionInitialized = false; // 防止重复瞬移

    public SacredObsidianEntity(EntityType<? extends SacredObsidianEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;  // 禁用碰撞检测 // Disable collision detection
    }

    public SacredObsidianEntity(Level world, double x, double y, double z) {
        super(ModEntity.SACRED_OBSIDIAN_ITEM_ENTITY.get(), world);  // 使用自定义实体类型
        this.setPosRaw(x, y, z); // 原始位置设置，跳过同步标记
        this.noPhysics = true;  // 禁用碰撞检测 // Disable collision detection
    }

    // 设置实体拥有者（目标玩家） // Set the owner of the entity (the target player)
    public void setOwner(Player player) {
        this.owner = player;
        this.ownerUUID = player.getUUID();  // 保存玩家UUID // Store the player's UUID
    }

    @Override
    protected void defineSynchedData() {
        // 此处可以定义需要同步的数据字段 // Define fields that need to be synchronized here
        //this.entityData.define(TARGET_X, (float) this.getX());
        //this.entityData.define(TARGET_Y, (float) this.getY());
        //this.entityData.define(TARGET_Z, (float) this.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            System.out.printf("[Server] Tick Pos: %.3f, %.3f, %.3f\n", getX(), getY(), getZ());
        } else {
            System.out.printf("[Client] Tick Pos: %.3f, %.3f, %.3f | Old: %.3f, %.3f, %.3f\n",
                    getX(), getY(), getZ(), xOld, yOld, zOld);
        }

        // 旋转动画
        this.setYRot(this.getYRot() + 2.5F);

        // 恢复 owner
        if (owner == null && ownerUUID != null) {
            Player player = level().getPlayerByUUID(ownerUUID);
            if (player != null) owner = player;
        }
        if (owner == null) return;

        if (!level().isClientSide) {
            double dist = owner.distanceTo(this);
            if (dist > 128.0) {
                this.discard();
                return;
            }

            if (this.getBoundingBox().intersects(owner.getBoundingBox())) {
                playPickUpSound();
                this.discard();
                return;
            }

            Vec3 target = owner.position().add(0, 1.0, 0);
            Vec3 velocity = target.subtract(this.position()).normalize().scale(0.1);
            this.move(MoverType.SELF, velocity);
        }
    }

    // 禁用重力 // Disable gravity
    @Override
    public boolean isNoGravity() {
        return true;
    }

    // 播放拾取声音 // Play the pickup sound
    private void playPickUpSound() {
        this.getCommandSenderWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F
        );
    }

    // 设置黑曜石的物品显示 // Set the obsidian item display
    public ItemStack getItem() {
        return new ItemStack(net.minecraft.world.item.Items.OBSIDIAN);
    }

    // 读取额外保存的数据 // Read additional saved data
    @Override
    protected void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag compound) {

        // 读取玩家UUID // Read the player's UUID
        if (compound.contains("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
    }

    // 添加额外保存的数据 // Add additional saved data
    @Override
    protected void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag compound) {

        // 保存玩家UUID // Save the player's UUID
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();

        if (!level().isClientSide) {
            if (this.ownerUUID != null) {
                Player player = this.level().getPlayerByUUID(ownerUUID);
                if (player != null) {
                    this.owner = player;
                }
            }
        } else {
            // 初始化插值参考点
            this.xOld = this.getX();
            this.yOld = this.getY();
            this.zOld = this.getZ();
        }
    }
}
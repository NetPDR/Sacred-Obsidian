package com.netpdrmod.entity;

import com.netpdrmod.registry.ModEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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

    public SacredObsidianEntity(EntityType<? extends SacredObsidianEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;  // 禁用碰撞检测 // Disable collision detection
    }

    public SacredObsidianEntity(Level world, double x, double y, double z) {
        super(ModEntity.SACRED_OBSIDIAN_ITEM_ENTITY.get(), world);  // 使用自定义实体类型
        this.setPos(x, y, z);
        this.noPhysics = true;  // 禁用碰撞检测 // Disable collision detection
    }

    // 设置实体拥有者（目标玩家） // Set the owner of the entity (the target player)
    public void setOwner(Player player) {
        this.owner = player;
        this.ownerUUID = player.getUUID();  // 保存玩家UUID // Store the player's UUID
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }

    @Override
    public void tick() {
        super.tick();

        // 检查是否存在owner // Check if the owner exists
        if (owner == null && ownerUUID != null) {
            // 如果没有owner，尝试恢复 // If there is no owner, try to recover it
            Player player = this.getCommandSenderWorld().getPlayerByUUID(ownerUUID);
            if (player != null) {
                this.owner = player;
            }
        }

        // 使黑曜石实体绕Z轴自旋 // Make the obsidian entity rotate around the Z-axis
        this.setXRot(this.getXRot() + 1.0F);  // 每帧绕X轴自旋（你可以调整旋转角度）// Rotate around the X-axis by 1 degree per frame (can adjust the rotation angle)
        this.setYRot(this.getYRot() + 5.0F);  // 每帧绕Y轴自旋（你可以调整旋转角度）// Rotate around the Y-axis by 5 degrees per frame (can adjust the rotation angle)

        // 如果存在owner，并且不在客户端，执行吸引逻辑 // If there is an owner, and it's not on the client side, perform the attraction logic
        if (owner != null && !this.getCommandSenderWorld().isClientSide) {
            double distance = this.owner.distanceTo(this);  // 计算与玩家的距离 // Calculate the distance to the player
            double maxDistance = 128.0; // 设置距离限制 // Set a maximum distance limit

            if (distance > maxDistance) {
                this.discard(); // 如果距离超过限制，移除实体 // If distance exceeds the limit, remove the entity
                return;
            }

            // 如果黑曜石实体距离玩家足够近，则移除并播放拾取声音 // If the obsidian entity is close enough to the player, remove it and play the pickup sound
            double minDistance = 1.0;
            if (distance < minDistance) {
                playPickUpSound();
                this.discard();  // 移除实体 // Remove the entity
                return;
            }

            // 计算朝向玩家的运动方向 // Calculate the direction towards the player
            Vec3 direction = owner.position().subtract(this.position()).normalize();
            // 吸引速度系数 // Attraction speed coefficient
            double attractionSpeed = 0.2;
            Vec3 targetVelocity = direction.scale(attractionSpeed);  // 吸引速度

            // 更新实体的运动，使其朝向玩家靠近 // Update the entity's movement to approach the player
            this.setDeltaMovement(targetVelocity);
            this.moveTo(this.getX() + targetVelocity.x, this.getY() + targetVelocity.y, this.getZ() + targetVelocity.z);
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

        // 在实体添加到世界时恢复玩家  // Restore the player when the entity is added to the world
        if (this.ownerUUID != null) {
            Player player = this.getCommandSenderWorld().getPlayerByUUID(ownerUUID);
            if (player != null) {
                this.owner = player;
            }
        }
    }
}
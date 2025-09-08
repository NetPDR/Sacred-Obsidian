package com.netpdrmod.weapon;

import com.netpdrmod.Netpdrmod;
import com.netpdrmod.blockentity.SacredObsidianBlockEntity;
import com.netpdrmod.client.ClientSpawnObsidianEffectPacket;
import com.netpdrmod.data.SacredObsidianData;
import com.netpdrmod.registry.ModEffect;
import com.netpdrmod.registry.ModEnchantments;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SacredObsidianItem extends BaseItem {

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        // 采掘速度 // mining speed
        float netheriteEfficiency = 25.0F;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? netheriteEfficiency : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        // // 检查方块是否可以用镐采集，如果可以则返回 true // Check if the block can be mined with a pickaxe; if so, return true
        return super.isCorrectToolForDrops(state);
    }

    //private static final int MAX_DISTANCE = 15;  // 最大延伸距离 // Maximum extension distance
    private static final int BASE_MAX_DISTANCE = 15;
    private static final int EXTRA_PER_LEVEL = 5;  // 每级多延伸 5 格 //Each level extends by 5 more tiles
    public static final float OBSIDIAN_DAMAGE = 25.0F;  // 黑曜石伤害 // Obsidian damage
    private static final double MAX_DIRECTION_CHANGE = 0.1;  // 随机方向变化幅度 // Random direction change amplitude
    public static final int COOLDOWN_TIME = 30;  // 冷却时间 (30 ticks = 1.5秒) // Cooldown time (30 ticks = 1.5 seconds)
    public static final int OBSIDIAN_LIFETIME = 60;  // 黑曜石的存在时间 (60 ticks = 3秒) // Obsidian's lifetime (60 ticks = 3 seconds)

    public SacredObsidianItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        CompoundTag tag = itemStack.getOrCreateTag();

        // 切换形态（Shift+右键） Switch Form // (Shift+Right Click)
        if (!world.isClientSide && player.isShiftKeyDown()) {
            boolean secondForm = tag.getBoolean("SecondForm");
            tag.putBoolean("SecondForm", !secondForm);

            // 切换时终止延伸 Terminate // extension during switching
            tag.putBoolean("IsExtending", false);

            if (!secondForm) {
                player.displayClientMessage(Component.translatable("message.sacred_obsidian.switch_to_second"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.sacred_obsidian.switch_to_first"), true);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                ResourceLocation id = new ResourceLocation("netpdrmod", "switch_form");
                Advancement adv = serverPlayer.server.getAdvancements().getAdvancement(id);
                if (adv != null) {
                    AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(adv);
                    if (!progress.isDone()) {
                        for (String criteria : progress.getRemainingCriteria()) {
                            serverPlayer.getAdvancements().award(adv, criteria);
                        }
                    }
                }
            }

            return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
        }

        if (!world.isClientSide) {
                long currentTime = world.getGameTime();
                long lastUse = tag.getLong("LastUseTime");

                if (currentTime - lastUse < COOLDOWN_TIME) {

                    return InteractionResultHolder.fail(itemStack);
                }

                // 设置新的使用时间 // Set new usage time
                tag.putLong("LastUseTime", currentTime);
                tag.putInt("CooldownTicks", COOLDOWN_TIME);

            if (!tag.getBoolean("SecondForm")) {
                // 读取附魔等级，算出最大延伸距离 Read the enchantment level and calculate the maximum extension distance
                Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(itemStack);
                int reachLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_REACH.get(), 0);
                int maxDistance = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL;

                // 播放铁砧声音 // Play anvil sound
                world.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);

                // 射线检测以获取命中位置 // Ray tracing to get hit position
                BlockHitResult hitResult = rayTrace(world, player, maxDistance);
                BlockPos hitPos = hitResult.getBlockPos();

                // 延伸黑曜石并处理目标实体 // Extend obsidian and handle target entity
                extendObsidianPathAndDamage(world, player, hitPos, itemStack, maxDistance);

            } else {
                // ------------------ 第二形态逻辑 Second-form logic------------------
                Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(itemStack);
                int reachLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_REACH.get(), 0);
                int maxBlocks = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL;

                tag.putBoolean("IsExtending", true);
                tag.putInt("ExtendBlocks", 0);
                tag.putInt("MaxBlocks", maxBlocks);

                tag.putDouble("DirX", player.getLookAngle().x);
                tag.putDouble("DirY", player.getLookAngle().y);
                tag.putDouble("DirZ", player.getLookAngle().z);
                tag.putDouble("PosX", player.getX());
                tag.putDouble("PosY", player.getEyeY());
                tag.putDouble("PosZ", player.getZ());

                world.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("IsExtending", false); // 停止延伸 // Stop extending
        return true;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!(entity instanceof Player player) || world.isClientSide) return;

        // 如果玩家死亡、进入旁观者模式或实体已移除，则立即把 IsExtending 设为 false 并返回 // If the player dies, enters spectator mode, or the entity has been removed, immediately set IsExtending to false and return
        if (!player.isAlive() || player.isSpectator() || player.isRemoved()) {
            CompoundTag t = stack.getOrCreateTag();
            t.putBoolean("IsExtending", false);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.getBoolean("SecondForm") || !tag.getBoolean("IsExtending")) return;

        int blocks = tag.getInt("ExtendBlocks");
        int maxBlocks = tag.getInt("MaxBlocks");
        if (blocks >= maxBlocks) {
            tag.putBoolean("IsExtending", false);
            return;
        }

        // ===== 可调常量（可改为类常量）Adjustable constant (can be changed to class constant) =====
        final double STEP_DISTANCE = 1.0;         // 每块间距（格数） // Interval (number of grids) per piece
        final double MAX_MOVE_PER_TICK = 0.8;     // 游标每 tick 最大移动（格数） // Maximum cursor movement per tick (in number of squares)
        final double DESIRED_DISTANCE_MULT = blocks + 1; // desired 距离倍数 // desired distance multiplier
        final double QUICK_ACCEPT_ALIGNMENT = 0.92; // 快速接受阈值 // Quick acceptance threshold
        // ======================================

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();

        // desired（基于玩家眼位和实时视角） // desired（基于玩家眼位和实时视角）
        Vec3 desired = eye.add(look.scale(DESIRED_DISTANCE_MULT * STEP_DISTANCE));

        // 读取或初始化 cursor（游标） // Read or initialize cursor
        Vec3 cursor;
        if (tag.contains("CursorX")) {
            cursor = new Vec3(tag.getDouble("CursorX"), tag.getDouble("CursorY"), tag.getDouble("CursorZ"));
        } else {
            cursor = desired;
            tag.putDouble("CursorX", cursor.x);
            tag.putDouble("CursorY", cursor.y);
            tag.putDouble("CursorZ", cursor.z);
        }

        // 限速平滑移动 cursor 朝 desired（直接使用常量，无冗余局部变量） // Limit the speed and smoothly move the cursor towards the desired position (using constants directly, without redundant local variables)
        Vec3 delta = desired.subtract(cursor);
        if (delta.length() > 1e-6) {
            Vec3 move = delta.length() > MAX_MOVE_PER_TICK ? delta.normalize().scale(MAX_MOVE_PER_TICK) : delta;
            cursor = cursor.add(move);
            tag.putDouble("CursorX", cursor.x);
            tag.putDouble("CursorY", cursor.y);
            tag.putDouble("CursorZ", cursor.z);
        }

        // 获取上次已放方块中心 // Get the center of the last placed block
        if (blocks == 0) {
            // 第一个方块：直接以玩家眼位沿视线 STEP_DISTANCE 处为目标 // The first square: directly target the position STEP_DISTANCE away along the player's line of sight
            BlockPos targetPos = BlockPos.containing(eye.add(look.scale(STEP_DISTANCE)));
            BlockState state = world.getBlockState(targetPos);
            if (world.isEmptyBlock(targetPos) || state.canBeReplaced()) {
                placeObsidianAt(world, stack, tag, player, targetPos, look);
            } else {
                tag.putBoolean("IsExtending", false);
            }
            return;
        }

        Vec3 lastCenter = new Vec3(tag.getDouble("PosX"), tag.getDouble("PosY"), tag.getDouble("PosZ"));
        BlockPos lastPos = BlockPos.containing(lastCenter);

        // 当游标距离上一个中心达到阈值才尝试放下下一块 // Only when the cursor is within a certain threshold distance from the previous center, will an attempt be made to place the next piece
        if (cursor.distanceTo(lastCenter) < STEP_DISTANCE - 1e-6) return;// 尚未到达放置阈值 // The placement threshold has not been reached yet

        // 选择相邻格：使用整洁的 helper（不含多余形参） // Select adjacent cells: Use a clean helper function (without unnecessary parameters)
        BlockPos nextPos = chooseAdjacentTowardsCursor(world, lastPos, cursor, look, QUICK_ACCEPT_ALIGNMENT);
        if (nextPos == null) {
            tag.putBoolean("IsExtending", false); // 被阻挡 -> 停止 // Being blocked -> Stop
            return;
        }

        placeObsidianAt(world, stack, tag, player, nextPos, look);
    }

    /**
     * 选择 lastPos 六个相邻格中最朝向 cursor 的那个可放格；无可放则返回 null。 Select the most cursor-oriented available grid from the six adjacent grids of lastPos; if there is no available grid, return null.
     * quickAcceptThreshold 用于快速接受高度对齐格子。 Used for quickly accepting highly aligned grids
     */
    @Nullable
    private BlockPos chooseAdjacentTowardsCursor(Level world, BlockPos lastPos, Vec3 cursor, Vec3 look, double quickAcceptThreshold) {
        BlockPos[] neighbors = new BlockPos[] {
                lastPos.north(), lastPos.south(), lastPos.west(), lastPos.east(), lastPos.above(), lastPos.below()
        };

        Vec3 lastCenter = new Vec3(lastPos.getX() + 0.5, lastPos.getY() + 0.5, lastPos.getZ() + 0.5);
        Vec3 dirToCursor = cursor.subtract(lastCenter);
        if (dirToCursor.length() == 0) dirToCursor = look;
        else dirToCursor = dirToCursor.normalize();

        BlockPos best = null;
        double bestDot = Double.NEGATIVE_INFINITY;

        for (BlockPos cand : neighbors) {
            BlockState s = world.getBlockState(cand);
            if (!(world.isEmptyBlock(cand) || s.canBeReplaced())) continue;

            Vec3 candCenter = new Vec3(cand.getX() + 0.5, cand.getY() + 0.5, cand.getZ() + 0.5);
            Vec3 neighborVec = candCenter.subtract(lastCenter);
            if (neighborVec.length() == 0) continue;
            neighborVec = neighborVec.normalize();

            double dot = neighborVec.dot(dirToCursor);
            if (dot > bestDot) {
                bestDot = dot;
                best = cand;
            }
            if (dot >= quickAcceptThreshold) return cand; // 快速接受 // Quick acceptance
        }
        return best;
    }

    /** placeObsidianAt 保持原逻辑：放方块、加入 SacredObsidianData、命中检测、更新 NBT ; placeObsidianAt maintains the original logic: placing blocks, adding SacredObsidianData, hit detection, and updating NBT */
    private void placeObsidianAt(Level world, ItemStack stack, CompoundTag tag, Player player, BlockPos pos, Vec3 dirForUpdate) {
        world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        world.setBlockEntity(new SacredObsidianBlockEntity(pos, Blocks.OBSIDIAN.defaultBlockState()));

        if (world instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);
            data.putObsidian(pos, OBSIDIAN_LIFETIME);   // 使用封装，自动 setDirty() // Use encapsulation to automatically call setDirty()
            data.setOwner(pos, player.getUUID()); // 绑定归属 // Binding ownership
        }

        LivingEntity target = findTargetEntityAtPosition(world, pos, player);
        if (target != null) {
            Map<Enchantment,Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
            int powerLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_POWER.get(), 0);
            float damage = OBSIDIAN_DAMAGE + powerLevel * 5.0F;

            DamageSource src = world.damageSources().playerAttack(player);
            target.hurt(src, damage);

            target.addEffect(new MobEffectInstance(ModEffect.IRRECONCILABLE_CRACK.get(), 100, 0));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));

            world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);
            tag.putBoolean("IsExtending", false);
            return;
        }

        Vec3 nextCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        tag.putDouble("PosX", nextCenter.x);
        tag.putDouble("PosY", nextCenter.y);
        tag.putDouble("PosZ", nextCenter.z);
        tag.putInt("ExtendBlocks", tag.getInt("ExtendBlocks") + 1);

        tag.putDouble("DirX", dirForUpdate.x);
        tag.putDouble("DirY", dirForUpdate.y);
        tag.putDouble("DirZ", dirForUpdate.z);
    }

    /**
     * 扩展黑曜石路径并对目标实体造成伤害 Extend the obsidian path and deal damage to the target entity
     *
     * @param world       当前游戏世界 Current game world
     * @param player      当前使用物品的玩家 Player using the item
     * @param targetPos   射线检测命中的位置 Hit position from ray tracing
     * @param maxDistance 本次可以延伸的最大距离 The maximum distance that can be extended this time
     */
    private void extendObsidianPathAndDamage(Level world, Player player, BlockPos targetPos, ItemStack stack, int maxDistance) {

        // 先读取 Obsidian Power 等级 // Read the Obsidian Power grade first
        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        int powerLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_POWER.get(), 0);

        // 计算每次造成的伤害：基础 + 每级5点 // Calculate the damage dealt each time: base + 5 points per level
        float damagePerHit = OBSIDIAN_DAMAGE + powerLevel * 5.0F;

        Random random = new Random();
        Vec3 currentPos = player.position();
        Vec3 direction = Vec3.atCenterOf(targetPos).subtract(currentPos).normalize();
        SacredObsidianData data = SacredObsidianData.get((ServerLevel) world); // 获取数据存储 // Get data store

        for (int i = 0; i < maxDistance; i++) {
            direction = applyRandomDirectionChange(direction, random);
            currentPos = currentPos.add(direction);
            BlockPos nextPos = BlockPos.containing(currentPos.x, currentPos.y, currentPos.z);

            BlockState blockState = world.getBlockState(nextPos);
            // 放置黑曜石并附加 BlockEntity // Place obsidian and attach BlockEntity
            if (world.isEmptyBlock(nextPos) || blockState.canBeReplaced()) {
                world.setBlock(nextPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                world.setBlockEntity(new SacredObsidianBlockEntity(nextPos, Blocks.OBSIDIAN.defaultBlockState()));
                data.putObsidian(nextPos, OBSIDIAN_LIFETIME); // 使用封装 // Use encapsulation
                data.setOwner(nextPos, player.getUUID()); // 绑定归属 // Binding ownership
            }

            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, new AABB(nextPos).inflate(0.5), e -> e != player);
            if (!entities.isEmpty()) {
                LivingEntity target = entities.get(0);
                DamageSource damageSource = world.damageSources().playerAttack(player);
                target.hurt(damageSource, damagePerHit);
                target.addEffect(new MobEffectInstance(ModEffect.IRRECONCILABLE_CRACK.get(), 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                break;  // 停止延伸 // Stop extending
            }
        }
    }

    /**
     * 随机改变路径方向 Randomly change path direction
     *
     * @param direction 当前的方向向量 Current direction vector
     * @param random    随机数生成器 Random number generator
     * @return 改变后的方向向量 Changed direction vector
     */
    private Vec3 applyRandomDirectionChange(Vec3 direction, Random random) {
        double randomX = (random.nextDouble() - 0.5) * MAX_DIRECTION_CHANGE;
        double randomY = (random.nextDouble() - 0.5) * MAX_DIRECTION_CHANGE;
        double randomZ = (random.nextDouble() - 0.5) * MAX_DIRECTION_CHANGE;
        return direction.add(randomX, randomY, randomZ).normalize();
    }

    /**
     * 查找方块位置的实体，排除玩家自己 Find entity at block position, excluding the player
     *
     * @param world  当前游戏世界 Current game world
     * @param pos    方块位置 Block position
     * @param player 当前玩家 Current player
     * @return 查找到的实体，若无则返回 null Found entity, or null if none
     */
    @Nullable
    private LivingEntity findTargetEntityAtPosition(Level world, BlockPos pos, Player player) {
        return world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos)).stream()
                .filter(entity -> entity != player)  // 排除玩家自己 // Exclude the player
                .findFirst()
                .orElse(null);
    }

    /**
     * 射线检测，返回命中的方块或实体 Ray trace, returning the hit block or entity
     *
     * @param world  当前游戏世界 Current game world
     * @param player 当前玩家 Current player
     * @return 返回命中的方块 The hit block
     */
    private BlockHitResult rayTrace(Level world, Player player, int maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().scale(maxDistance);
        return world.clip(new ClipContext(eyePos, eyePos.add(look), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /**
     * 遍历所有维度并调用每个维度的 tick 逻辑
     * Iterate over all dimensions and call tick logic for each dimension.
     *
     * @param mainWorld 主世界 // Main world
     */
    public static void tickAllDimensions(ServerLevel mainWorld) {
        for (ServerLevel world : mainWorld.getServer().getAllLevels()) {
            tick(world);  // 调用现有的 tick 方法处理每个维度中的黑曜石逻辑 // Call the existing tick method to handle the obsidian logic in each dimension
        }
    }

    /**
     * 在 tick 方法中移除黑曜石并生成黑曜石实体以及添加粒子效果 In the tick method, remove obsidian whilst spawning obsidian entity and adding particle effects.
     * 仅移除由 SacredObsidianItem 放置的黑曜石方块，不会影响其他方块 Only remove obsidian blocks placed by SacredObsidianItem; it won't affect other blocks.
     *
     * @param world 当前游戏世界 Current game world
     */
    public static void tick(Level world) {

        if (world instanceof ServerLevel serverLevel) {  // 确保是 ServerLevel // Ensure that is ServerLevel
            SacredObsidianData data = SacredObsidianData.get(serverLevel);

            // 先拷贝一份可变 Map 以便安全修改（并避免直接操作可能为不可变视图的返回值）
            Map<BlockPos, Integer> obsidianBlocks = new HashMap<>(data.getObsidianData());

            for (Map.Entry<BlockPos, Integer> entry : obsidianBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                int ticksLeft = entry.getValue();

                // 确保当前方块是 SacredObsidianItem 放置的黑曜石，并检查其剩余时间 // Ensure the current block is the obsidian placed by SacredObsidianItem, and check its remaining time
                BlockState blockState = world.getBlockState(pos);
                if (ticksLeft <= 0 && blockState.is(Blocks.OBSIDIAN)) {
                    // 移除黑曜石方块并播放挖掘音效 // Remove the obsidian block and play the digging sound
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    world.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // 获取并处理归属玩家
                    data.getOwner(pos).ifPresent(uuid -> {
                        Player player = serverLevel.getPlayerByUUID(uuid);
                        if (player != null && !world.isClientSide) {
                            Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                            Vec3 target = player.position().add(0, 1.0, 0);

                            Netpdrmod.CHANNEL.send(
                                    PacketDistributor.ALL.noArg(),
                                    new ClientSpawnObsidianEffectPacket(start, target, true)
                            );
                        }
                    });

                    //if (player != null) {
                    //SacredObsidianEntity obsidianEntity = new SacredObsidianEntity(serverLevel, pos.getX(), pos.getY(), pos.getZ());
                    //obsidianEntity.setOwner(player);  // 设置玩家为拥有者 // Set the player as the owner
                    //serverLevel.addFreshEntity(obsidianEntity);  // 将黑曜石实体添加到世界中 // Add the obsidian entity to the world
                    //}

                    // 通知客户端生成粒子效果 // Notify the client to generate particle effects
                    notifyClientForParticles(world, pos);

                    // 使用封装方法移除条目并清理 owner（会 setDirty） // Use encapsulation method to remove the entry and clean up the owner (which will setDirty)
                    data.removeObsidian(pos);
                    data.removeOwner(pos);

                } else if (blockState.is(Blocks.OBSIDIAN)) {
                    // 每次 tick 只减少 1 // Decrease by 1 each tick
                    data.putObsidian(pos, ticksLeft - 1);
                } else {
                    // 如果方块不是黑曜石，直接移除记录，防止误删其他方块 // If the block is not obsidian, remove the record directly to prevent incorrect deletion of other blocks
                    data.removeObsidian(pos);
                    data.removeOwner(pos); // 清理归属 // Ownership cleanup
                }
            }
        }
    }

    /**
     * 通过服务器端向客户端发送粒子效果生成通知 Notify the client to generate particle effects from the server side
     *
     * @param world 当前游戏世界 Current game world
     * @param pos   粒子效果的位置 Position for the particle effects
     */
    private static void notifyClientForParticles(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()), // 粒子类型 // Particle type
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, // 粒子的位置 // Particle position
                    20, // 粒子数量 // Number of particles
                    0.5D, 0.5D, 0.5D, // 粒子扩散范围 // Particle spread range
                    0.1D // 粒子的速度 // Particle speed
            );
        }
    }
}
package com.netpdr.sacredobsidian.weapon;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.blockentity.SacredObsidianBlockEntity;
import com.netpdr.sacredobsidian.client.entity.ClientSpawnObsidianEffectPacket;
import com.netpdr.sacredobsidian.data.SacredObsidianData;
import com.netpdr.sacredobsidian.data.SacredObsidianDataComponents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SacredObsidianItem extends BaseItem {

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        float netheriteEfficiency = 25.0F;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? netheriteEfficiency : super.getDestroySpeed(stack, state); // 如果是可用镐采掘的方块，使用高效率 // If it’s a block that can be mined with a pickaxe, use efficiency.
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, BlockState state) {
        return super.isCorrectToolForDrops(stack, state);
    }

    private static final int BASE_MAX_DISTANCE = 15; // 基础延伸块数 // Number of Base Extension Blocks
    private static final int EXTRA_PER_LEVEL = 5; // 每级附魔额外延伸块数 // Additional extension blocks per enchantment level
    public static final float OBSIDIAN_DAMAGE = 25.0F; // 黑曜石造成基础伤害 // Obsidian deals base damage
    private static final double MAX_DIRECTION_CHANGE = 0.1; // 延伸时随机扰动幅度 // Random perturbation amplitude during extension
    public static final int COOLDOWN_TIME = 30; // 冷却（tick） // Cooldown (tick)
    public static final int OBSIDIAN_LIFETIME = 60; // 黑曜石存活时间（tick） // Obsidian survival time (tick)

    private static final float ARMOR_BYPASS_PERCENT = 0.3F; // 30% 固定穿甲

    public SacredObsidianItem(Properties properties) {
        super(Tiers.NETHERITE, properties.stacksTo(1)); // 物品最大堆叠为 1 // The maximum stack size for this item is 1
    }

    public InteractionResultHolder<ItemStack> handleRightClick(
            Level world, Player player, InteractionHand hand
    ) {

        // 获取玩家手中的 ItemStack
        // Get the ItemStack in the player's hand
        ItemStack itemStack = player.getItemInHand(hand);

        /* =========================================================
         * Shift + 右键：切换形态（仅服务端）
         * Shift + Right Click: Switch form (server-side only)
         * ========================================================= */
        if (!world.isClientSide && player.isShiftKeyDown()) {

            // 当前是否为第二形态（默认 false）
            // Whether currently in second form (default: false)
            boolean secondForm = itemStack.getOrDefault(
                    SacredObsidianDataComponents.SECOND_FORM.get(),
                    false
            );

            // 切换形态
            // Toggle form
            itemStack.set(
                    SacredObsidianDataComponents.SECOND_FORM.get(),
                    !secondForm
            );

            // 切换时强制终止延伸
            // Force stop extending when switching form
            itemStack.set(
                    SacredObsidianDataComponents.IS_EXTENDING.get(),
                    false
            );

            // 清理反向模式（防止残留）
            // Clear reverse mode to prevent leftover state
            itemStack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());

            if (!secondForm) {
                player.displayClientMessage(
                        Component.translatable("message.sacred_obsidian.switch_to_second"),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.translatable("message.sacred_obsidian.switch_to_first"),
                        true
                );
            }

            /* ===== Advancement 触发（保持原逻辑） ===== */
            if (player instanceof ServerPlayer serverPlayer) {
                ResourceLocation id =
                        ResourceLocation.fromNamespaceAndPath("sacredobsidian", "switch_form");

                AdvancementHolder holder =
                        serverPlayer.server.getAdvancements().get(id);

                if (holder != null) {
                    PlayerAdvancements advancements = serverPlayer.getAdvancements();
                    AdvancementProgress progress =
                            advancements.getOrStartProgress(holder);

                    if (!progress.isDone()) {
                        for (String criteria : progress.getRemainingCriteria()) {
                            advancements.award(holder, criteria);
                        }
                    }
                }
            }

            return InteractionResultHolder.sidedSuccess(
                    itemStack, world.isClientSide()
            );
        }

        /* =========================================================
         * 非潜行右键：使用逻辑（仅服务端）
         * Normal use logic (server-side only)
         * ========================================================= */
        if (!world.isClientSide) {

            long currentTime = world.getGameTime();

            // 上次使用时间（默认 0）
            // Last use tick (default 0)
            long lastUse = itemStack.getOrDefault(
                    SacredObsidianDataComponents.LAST_USE_TICK.get(),
                    0L
            );

            // 冷却检测
            // Cooldown check
            if (currentTime - lastUse < COOLDOWN_TIME) {
                return InteractionResultHolder.fail(itemStack);
            }

            // 写入新的冷却数据
            // Write new cooldown data
            itemStack.set(
                    SacredObsidianDataComponents.LAST_USE_TICK.get(),
                    currentTime
            );

            itemStack.set(
                    SacredObsidianDataComponents.COOLDOWN_TICKS.get(),
                    COOLDOWN_TIME
            );

            /* ================= 第一形态 ================= */
            if (!itemStack.getOrDefault(
                    SacredObsidianDataComponents.SECOND_FORM.get(),
                    false
            )) {
                // 一次性直线延伸
                // One-time straight extension

                int reachLevel = getEnchantmentLevel(itemStack, world, Sacredobsidian.MODID + ":obsidian_reach");

                int maxDistance = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL;

                world.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.ANVIL_LAND,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                BlockHitResult hitResult =
                        rayTrace(world, player, maxDistance);
                BlockPos hitPos = hitResult.getBlockPos();

                extendObsidianPathAndDamage(
                        world,
                        player,
                        hitPos,
                        itemStack,
                        maxDistance
                );

            }
            /* ================= 第二形态 ================= */
            else {
                // 持续延伸模式（inventoryTick 处理）
                // Continuous extending mode (handled in inventoryTick)

                int reachLevel = getEnchantmentLevel(itemStack, world, Sacredobsidian.MODID + ":obsidian_reach");

                int maxBlocks = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL;

                itemStack.remove(SacredObsidianDataComponents.SUB_STATE.get());

                // 标记进入延伸状态
                // Mark as extending
                itemStack.set(
                        SacredObsidianDataComponents.IS_EXTENDING.get(),
                        true
                );

                itemStack.set(
                        SacredObsidianDataComponents.SUB_STATE.get(),
                        1 // LIFT
                );

                // 重置已延伸数量
                // Reset extended block count
                itemStack.set(
                        SacredObsidianDataComponents.EXTEND_BLOCKS.get(),
                        0
                );

                // 写入最大可延伸数量
                // Write maximum extendable blocks
                itemStack.set(
                        SacredObsidianDataComponents.MAX_BLOCKS.get(),
                        maxBlocks
                );

                // 初始化上一次放置位置（玩家眼位）
                // Initialize last placed position (player eye position)
                itemStack.set(
                        SacredObsidianDataComponents.LAST_POS.get(),
                        new Vec3(player.getX(), player.getEyeY(), player.getZ())
                );

                // 清除 cursor（首次在 inventoryTick 初始化）
                // Clear cursor (initialized in inventoryTick)
                itemStack.remove(SacredObsidianDataComponents.CURSOR.get());

                // 清理反向模式
                // Clear reverse mode
                itemStack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());

                world.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.ANVIL_LAND,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }

        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level world,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        // 原版交互授权中：完全放行，不执行黑曜石逻辑
        if (stack.getOrDefault(
                SacredObsidianDataComponents.VANILLA_INTERACTING.get(),
                false
        )) {
            return InteractionResultHolder.pass(stack);
        }

        return handleRightClick(world, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();

        // 原版交互授权中：放行方块交互
        if (stack.getOrDefault(
                SacredObsidianDataComponents.VANILLA_INTERACTING.get(),
                false
        )) {
            return InteractionResult.PASS;
        }

        handleRightClick(context.getLevel(), player, context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(
            @NotNull ItemStack stack,
            @NotNull Player player,
            @NotNull LivingEntity target,
            @NotNull InteractionHand hand
    ) {
        // 原版交互授权中：放行实体交互
        if (stack.getOrDefault(
                SacredObsidianDataComponents.VANILLA_INTERACTING.get(),
                false
        )) {
            return InteractionResult.PASS;
        }

        handleRightClick(player.level(), player, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onDroppedByPlayer(@NotNull ItemStack stack, @NotNull Player player) {
        // 停止延伸
        stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);

        stack.remove(SacredObsidianDataComponents.SUB_STATE.get());

        // 关闭反向模式
        stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());

        return true;
    }

    @Override
    public void inventoryTick(
            @NotNull ItemStack stack,
            @NotNull Level world,
            @NotNull Entity entity,
            int slot,
            boolean selected
    ) {
        super.inventoryTick(stack, world, entity, slot, selected);

        // 只在服务端玩家上处理
        // Only process on server-side players
        if (!(entity instanceof Player player) || world.isClientSide) return;
        if (!selected) return;

        /* =========================================================
         * 玩家死亡 / 旁观 / 实体移除 → 强制终止延伸
         * Player dead / spectator / removed → force stop extending
         * ========================================================= */
        if (!player.isAlive() || player.isSpectator() || player.isRemoved()) {
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.SUB_STATE.get());
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        boolean isExtending = stack.getOrDefault(
                SacredObsidianDataComponents.IS_EXTENDING.get(),
                false
        );

        if (!isExtending) {
            stack.remove(SacredObsidianDataComponents.SUB_STATE.get());
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        /* ================= 子状态 ================= */
        int subState = stack.getOrDefault(
                SacredObsidianDataComponents.SUB_STATE.get(), 0
        );

        if (subState == 1) { // LIFT
            handleLift(world, player, stack);
            return;
        }

        // 仅在第二形态 + 正在延伸时处理
        // Only handle when in second form and extending
        boolean secondForm = stack.getOrDefault(
                SacredObsidianDataComponents.SECOND_FORM.get(),
                false
        );

        if (!secondForm) return;

        int blocks = stack.getOrDefault(
                SacredObsidianDataComponents.EXTEND_BLOCKS.get(),
                0
        );
        int maxBlocks = stack.getOrDefault(
                SacredObsidianDataComponents.MAX_BLOCKS.get(),
                0
        );

        if (blocks >= maxBlocks) {
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        /* ================= 参数区（保持原数值） ================= */
        final double STEP_DISTANCE = 1.0;
        final double MAX_MOVE_PER_TICK = 0.85;
        final double DESIRED_DISTANCE_MULT = blocks + 1;
        final double QUICK_ACCEPT_ALIGNMENT = 0.88;
        /* ======================================================= */

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();

        // ReverseMode（由客户端按键控制）
        boolean reverse = stack.getOrDefault(
                SacredObsidianDataComponents.REVERSE_MODE.get(),
                false
        );
        if (reverse) {
            look = look.scale(-1.0).normalize();
        }

        Vec3 desired = eye.add(
                look.scale(DESIRED_DISTANCE_MULT * STEP_DISTANCE)
        );

        /* ================= Cursor（游标） ================= */
        Vec3 cursor = stack.getOrDefault(
                SacredObsidianDataComponents.CURSOR.get(),
                desired
        );

        Vec3 delta = desired.subtract(cursor);
        if (delta.length() > 1e-6) {
            Vec3 move = delta.length() > MAX_MOVE_PER_TICK
                    ? delta.normalize().scale(MAX_MOVE_PER_TICK)
                    : delta;

            cursor = cursor.add(move);
            stack.set(SacredObsidianDataComponents.CURSOR.get(), cursor);
        }

        /* ================= 首块放置 ================= */
        if (blocks == 0) {
            double startDist = 1.5;
            BlockPos targetPos = BlockPos.containing(
                    eye.add(look.scale(startDist))
            );

            int tries = 0;
            while (tries < 6) {
                Vec3 center = new Vec3(
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5
                );
                if (!player.getBoundingBox().contains(center)) break;
                startDist += 0.5;
                targetPos = BlockPos.containing(
                        eye.add(look.scale(startDist))
                );
                tries++;
            }

            if (canPlaceIgnoreOwn(world, targetPos)) {
                placeObsidianAt(world, stack, player, targetPos, look);
            } else {
                stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
                stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            }
            return;
        }

        /* ================= 后续方块 ================= */
        Vec3 lastCenter = stack.getOrDefault(
                SacredObsidianDataComponents.LAST_POS.get(),
                Vec3.ZERO
        );
        BlockPos lastPos = BlockPos.containing(lastCenter);

        if (cursor.distanceTo(lastCenter) < STEP_DISTANCE - 1e-6) return;

        BlockPos nextPos = chooseAdjacentTowardsCursor(
                world,
                lastPos,
                cursor,
                look,
                QUICK_ACCEPT_ALIGNMENT,
                player
        );

        if (nextPos == null) {
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        placeObsidianAt(world, stack, player, nextPos, look);
    }

    /**
     * 一次性延伸（第一形态）的逻辑。 The logic of one-time extension (first form).
     * 从玩家到目标点之间沿直线放置黑曜石，并对命中的实体造成伤害。 Place obsidian in a straight line from the player to the target point, dealing damage to any entity hit.
     */
    private void extendObsidianPathAndDamage(Level world, Player player, BlockPos targetPos, ItemStack stack, int maxDistance) {
        boolean damageEnabled = stack.getOrDefault(
                SacredObsidianDataComponents.DAMAGE_ENABLED,
                true
        );

        // 使用 ThreadLocalRandom 替代 new Random()，避免频繁实例化 Random // Use ThreadLocalRandom instead of new Random() to avoid frequent instantiation of Random
        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        Vec3 start = player.getEyePosition(1.0F); // 起点：玩家眼位 // Starting point: Player's eye level
        Vec3 end = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5); // 目标方块中心 // Target block center
        Vec3 dir = end.subtract(start);
        if (dir.length() == 0) return; // 无方向则退出 // Exit if there is no direction
        dir = dir.normalize(); // 归一化方向 // Normalization direction

        // 从眼前稍微偏移起步，避免在玩家身上 // Start slightly offset from the current position to avoid being on the player
        Vec3 current = start.add(dir.scale(1.2));
        int steps = 0;

        SacredObsidianData data = (world instanceof ServerLevel serverLevel) ? SacredObsidianData.get(serverLevel) : null; // 仅在服务端取数据存储 // Only fetch data on the server side for storage

        while (steps < maxDistance) {
            BlockPos pos = BlockPos.containing(current); // 取当前格 // Get current cell

            // 避免放在玩家体格内 // Avoid placing inside the player's body
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (player.getBoundingBox().contains(center)) {
                current = current.add(dir.scale(0.6)); // 跳过位于玩家处的格 // Skip the tile at the player's location
                steps++;
                continue;
            }

            BlockState blockState = world.getBlockState(pos);
            boolean canPlace = (world.isEmptyBlock(pos) || blockState.canBeReplaced())
                    || (blockState.is(Blocks.OBSIDIAN) && data != null && data.getObsidianData().containsKey(pos)); // 允许穿过本武器放的 obsidian // Allow passing through the obsidian placed by this weapon

            if (canPlace && world.isLoaded(pos)) { // 检查区块加载 // Check block loading
                world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3); // 放置黑曜石 // Place obsidian

                // 如果未来换成自定义 obsidian block 并绑定了 BlockEntity，这里会设置 BE 的初始信息 // If in the future it is changed to a custom obsidian block and bound to a BlockEntity, the initial information of the BE will be set here
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof SacredObsidianBlockEntity sbe) {
                    // 读取无副作用的方法以消除“空体/未使用变量”警告 // Read side-effect-free methods to eliminate 'empty body/unused variable' warnings
                    // 如果将来要做初始化，请在 SacredObsidianBlockEntity 添加初始化方法并在此调用 // If initialization is needed in the future, please add an initialization method in SacredObsidianBlockEntity and call it here
                    sbe.getBlockState(); // 只读调用，安全且能让编译器认为 sbe 被使用 // Read-only call, safe and convinces the compiler that sbe is being used
                }

                if (data != null) {
                    data.putObsidian(pos, OBSIDIAN_LIFETIME); // 记录寿命并 setDirty() // Record lifespan and setDirty()
                    data.setOwner(pos, player.getUUID()); // 记录归属 // Record Ownership
                }
            }

            LivingEntity target = findTargetEntityAtPosition(world, pos, player); // 检测命中实体 // Detect hit entity
            if (target != null) {
                if (damageEnabled) {
                    applyObsidianDamage(world, player, target, stack);
                }
                break; // 命中则停止延伸 // Stop extending upon hit
            }

            // 随机微扰以保持自然感（使用 MAX_DIRECTION_CHANGE） // Random perturbation to maintain a natural feel (using MAX_DIRECTION_CHANGE)
            double rx = tlr.nextDouble(-MAX_DIRECTION_CHANGE / 2.0, MAX_DIRECTION_CHANGE / 2.0);
            double ry = tlr.nextDouble(-MAX_DIRECTION_CHANGE / 2.0, MAX_DIRECTION_CHANGE / 2.0);
            double rz = tlr.nextDouble(-MAX_DIRECTION_CHANGE / 2.0, MAX_DIRECTION_CHANGE / 2.0);
            dir = dir.add(rx, ry, rz).normalize(); // 微扰并归一化 // Perturb and normalize

            current = current.add(dir); // 移动到下一个采样点 // Move to the next sample point
            steps++;
        }
    }

    /**
     * 判断是否可以在 pos 放置：空格、可替换方块、或是由本武器放置的 obsidian（允许穿过） Determine whether it is possible to place at pos: a space, a replaceable block, or obsidian placed by this weapon (can be passed through)
     */
    private boolean canPlaceIgnoreOwn(Level world, BlockPos pos) {
        // 避免使用被废弃的方法 isAreaLoaded(pos, 1) // Avoid using the deprecated method isAreaLoaded(pos, 1)
        if (!world.isLoaded(pos)) return false; // 区块未加载则不可放 // Cannot place if the chunk is not loaded

        BlockState s = world.getBlockState(pos);
        if (world.isEmptyBlock(pos) || s.canBeReplaced()) return true; // 空格或可替换 // Space or replaceable

        if (s.is(Blocks.OBSIDIAN) && world instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);
            return data.getObsidianData().containsKey(pos); // 由本武器放置的 obsidian 才允许穿过 // Only obsidian placed by this weapon is allowed to pass through
        }
        return false;
    }

    /**
     * 选择 lastPos 六个相邻格中最朝向 cursor 的那个可放格；无可放则返回 null。 Choose the placeable cell among the six adjacent cells of lastPos that is closest to the cursor; return null if none is placeable.
     * quickAcceptThreshold 用于快速接受高度对齐格子。 quickAcceptThreshold is used to quickly accept highly aligned grids.
     * 方法在选择时会排除玩家当前所占格，避免把方块放到玩家身上。 The method will exclude the tile currently occupied by the player when selecting, preventing the block from being placed on the player.
     */
    @Nullable
    private BlockPos chooseAdjacentTowardsCursor(Level world, BlockPos lastPos, Vec3 cursor, Vec3 look, double quickAcceptThreshold, Player player) {
        BlockPos[] neighbors = new BlockPos[] {
                lastPos.north(), lastPos.south(), lastPos.west(), lastPos.east(), lastPos.above(), lastPos.below()
        };

        Vec3 lastCenter = new Vec3(lastPos.getX() + 0.5, lastPos.getY() + 0.5, lastPos.getZ() + 0.5);
        Vec3 dirToCursor = cursor.subtract(lastCenter);
        if (dirToCursor.length() == 0) dirToCursor = look; // 如果游标与中心重合，使用视线方向 // If the cursor coincides with the center, use the gaze direction
        else dirToCursor = dirToCursor.normalize(); // 否则归一化方向 // Otherwise, normalize direction

        BlockPos best = null;
        double bestDot = Double.NEGATIVE_INFINITY;

        for (BlockPos cand : neighbors) {
            // 排除玩家所在位置 // Exclude the player's current position
            Vec3 candCenter = new Vec3(cand.getX() + 0.5, cand.getY() + 0.5, cand.getZ() + 0.5);
            if (player.getBoundingBox().contains(candCenter)) continue; // 玩家占据的格子不考虑 // The squares occupied by players are not considered

            // 允许替换空格、可替换方块、或自己放的 obsidian // Allows replacing spaces, replaceable blocks, or obsidian placed by yourself
            if (!canPlaceIgnoreOwn(world, cand)) continue;

            Vec3 neighborVec = candCenter.subtract(lastCenter);
            if (neighborVec.length() == 0) continue;
            neighborVec = neighborVec.normalize();

            double dot = neighborVec.dot(dirToCursor); // 计算方向相似度（点积） // Calculate directional similarity (dot product)
            if (dot > bestDot) {
                bestDot = dot;
                best = cand;
            }
            if (dot >= quickAcceptThreshold) return cand; // 快速接受高度对齐的格 // Quickly accept highly aligned grids
        }
        return best;
    }

    private void handleLift(Level world, Player player, ItemStack stack) {
        // ===== 延伸额度检查 =====
        int blocks = stack.getOrDefault(SacredObsidianDataComponents.EXTEND_BLOCKS.get(), 0);
        int maxBlocks = stack.getOrDefault(SacredObsidianDataComponents.MAX_BLOCKS.get(), 0);

        if (blocks >= maxBlocks) {
            // 清理 fractional 标志并结束延伸
            stack.remove(SacredObsidianDataComponents.FRACTIONAL_LIFTED.get());
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.SUB_STATE.get());
            return;
        }

        // ===== 视线必须明显朝下 =====
        if (player.getLookAngle().y > -0.6) {
            stack.set(SacredObsidianDataComponents.SUB_STATE.get(), 2);
            return;
        }

        // ===== 脚下检测 =====
        BlockPos groundPos = player.blockPosition().below();
        BlockState groundState = world.getBlockState(groundPos);

        if (groundState.isAir() || groundState.canBeReplaced()) {
            stack.set(SacredObsidianDataComponents.SUB_STATE.get(), 2);
            return;
        }

        // ===== 顶头检测 =====
        BlockPos headPos = player.blockPosition().above();
        if (HeadInspection(world, stack, headPos)) return;

        // ===== 初始放置位置 =====
        BlockPos placePos = player.blockPosition();

        // ===== 判断是否处于小数高度 =====
        double yFrac = player.getY() - Math.floor(player.getY());
        boolean isFractionalHeight = yFrac > 1e-4;

        // 读取是否已经做过“首块抬升”（会话内只允许一次）
        boolean alreadyFractionalLifted = stack.getOrDefault(
                SacredObsidianDataComponents.FRACTIONAL_LIFTED.get(), false
        );

        boolean needDoubleLiftThisTick = false;

        if (isFractionalHeight && !groundState.canBeReplaced() && !alreadyFractionalLifted) {
            placePos = placePos.above(); // 首块抬升一格
            needDoubleLiftThisTick = true;
            stack.set(SacredObsidianDataComponents.FRACTIONAL_LIFTED.get(), true);
        }

        // ===== 防止覆盖不可替代方块 =====
        if (HeadInspection(world, stack, placePos)) return;

        // ===== 玩家传送高度 =====
        double liftHeight = needDoubleLiftThisTick ? 2.0 : 1.0;

        player.teleportTo(
                player.getX(),
                player.getY() + liftHeight,
                player.getZ()
        );

        player.fallDistance = 0;

        // ===== 放置黑曜石 =====
        placeObsidianAt(
                world,
                stack,
                player,
                placePos,
                new Vec3(0.0, 1.0, 0.0)
        );
    }

    private boolean HeadInspection(Level world, ItemStack stack, BlockPos headPos) {
        BlockState headState = world.getBlockState(headPos);

        if (!headState.isAir() && !headState.canBeReplaced()) {
            // 清理 fractional 标志并结束延伸
            stack.remove(SacredObsidianDataComponents.FRACTIONAL_LIFTED.get());
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.SUB_STATE.get());
            return true;
        }
        return false;
    }
    
    /** placeObsidianAt 保持原逻辑：放方块、加入 SacredObsidianData、命中检测、更新 NBT */
    private void placeObsidianAt(
            Level world,
            ItemStack stack,
            Player player,
            BlockPos pos,
            Vec3 ignoredDirForUpdate // dirForUpdate: reserved for future directional effects
    ) {
        /* ================= 安全性检查 ================= */

        // 不在加载区，安全地停止延伸
        // Not in the loading area, safely stop extending
        if (!world.isLoaded(pos)) {
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        // 防止放到玩家身上（额外保险）
        // Prevent placing on the player (extra precaution)
        Vec3 center = new Vec3(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
        if (player.getBoundingBox().contains(center)) {
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        /* ================= 放置方块 ================= */

        // 直接放置黑曜石方块
        // Directly place obsidian blocks
        world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);

        // 如果未来换成自定义方块并注册了 BlockEntity，可在此初始化
        // If replaced with a custom block with BlockEntity in the future, initialize here
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof SacredObsidianBlockEntity sbe) {
            sbe.onPlacedBy(player);
        }

        // 记录世界级数据（寿命 & 归属）
        // Record world-level data (lifetime & ownership)
        if (world instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);
            data.putObsidian(pos, OBSIDIAN_LIFETIME);
            data.setOwner(pos, player.getUUID());
        }

        /* ================= 实体命中检测 ================= */

        LivingEntity target = findTargetEntityAtPosition(world, pos, player);

        // 伤害开关：未设置时默认为 true
        // Damage enabled by default if not explicitly set
        boolean damageEnabled = stack.getOrDefault(
                SacredObsidianDataComponents.DAMAGE_ENABLED.get(), true
        );

        if (target != null && damageEnabled) {

            /* ======= 伤害计算 ======= */
            applyObsidianDamage(world, player, target, stack);

            // 命中后停止延伸
            // Stop extending after hitting an entity
            stack.set(SacredObsidianDataComponents.IS_EXTENDING.get(), false);
            stack.remove(SacredObsidianDataComponents.REVERSE_MODE.get());
            return;
        }

        /* ================= 状态更新（未命中实体） ================= */

        Vec3 nextCenter = Vec3.atCenterOf(pos);

        // 更新“上一次放置位置”
        // Update last placed position
        stack.set(SacredObsidianDataComponents.LAST_POS.get(), nextCenter);

        // 已放置数量 +1
        // Increase placed block count
        int placed = stack.getOrDefault(
                SacredObsidianDataComponents.EXTEND_BLOCKS.get(), 0
        );
        stack.set(SacredObsidianDataComponents.EXTEND_BLOCKS.get(), placed + 1);
    }

    private void applyObsidianDamage(
            Level world,
            Player attacker,
            LivingEntity target,
            ItemStack stack
    ) {
        if (!target.isAlive()) return;

        int powerLevel = getEnchantmentLevel(
                stack, world, Sacredobsidian.MODID + ":obsidian_power"
        ); // 读取强力等级 // Read power level

        int shredderLevel = getEnchantmentLevel(
                stack, world, Sacredobsidian.MODID + ":armor_shredder"
        );

        float damagePerHit = OBSIDIAN_DAMAGE + powerLevel * 5.0F;

        // =========================
        // 基础伤害计算
        // =========================
        DamageSource normalSrc = world.damageSources().playerAttack(attacker);
        target.hurt(normalSrc, damagePerHit);

        // =========================
        // 精细护甲损耗（方向判定）
        // =========================
        if (!world.isClientSide && target.getArmorValue() > 0) {

            // 创造模式跳过护甲损耗
            boolean skipArmorDamage = target instanceof Player p && p.getAbilities().instabuild;

            if (!skipArmorDamage) {

                EquipmentSlot hitSlot = getEquipmentSlot(attacker, target);
                ItemStack hitArmor = target.getItemBySlot(hitSlot);

                if (!hitArmor.isEmpty() && hitArmor.isDamageableItem()) {

                    int L = shredderLevel;                     // ARMOR_SHREDDER 等级
                    int armorValue = target.getArmorValue();
                    float D = damagePerHit;                 // 本次黑曜石造成的伤害

                    int maxDur = hitArmor.getMaxDamage();
                    int currentDur = maxDur - hitArmor.getDamageValue();
                    float R = (float) currentDur / (float) maxDur;

                    // ===== PvP 平衡公式 =====
                    float formula =
                            (L * 0.03F * armorValue)
                                    + (D * 0.6F)
                                    + (0.25F * L)
                                    + (R * 0.03F * L);

                    // ===== 精确背刺判定（基于朝向）=====
                    Vec3 attackDir = attacker.getLookAngle().normalize();
                    Vec3 targetForward = target.getLookAngle().normalize();
                    double dot = attackDir.dot(targetForward);

                    boolean isBackstab = dot > 0.5;

                    if (isBackstab) {
                        formula *= 1.25F;
                    }

                    // ===== 精确爆头判定 =====
                    double attackerEyeY = attacker.getEyeY();
                    double headThreshold = target.getY() + target.getBbHeight() * 0.85;

                    boolean isHeadshot = attackerEyeY > headThreshold;

                    if (isHeadshot && hitSlot == EquipmentSlot.HEAD) {
                        formula *= 1.2F;
                    }

                    int damageToArmor = getDamageToArmor(target, maxDur, Math.round(formula));

                    int newDamage = hitArmor.getDamageValue() + damageToArmor;

                    boolean willBreak = newDamage >= hitArmor.getMaxDamage();

                    // ===== 实际损耗 =====
                    hitArmor.hurtAndBreak(damageToArmor, target, hitSlot);

                    // ===== 如果破损，手动发送实体事件 =====
                    if (willBreak) {

                        // 原版破损动画事件
                        target.getCommandSenderWorld().broadcastEntityEvent(
                                target,
                                (byte) (47 + hitSlot.getIndex())
                        );
                        
                        if (world instanceof ServerLevel server) {

                            BlockState particleState =
                                    getArmorParticleState(hitArmor);

                            server.sendParticles(
                                    new BlockParticleOption(
                                            ParticleTypes.BLOCK,
                                            particleState
                                    ),
                                    target.getX(),
                                    target.getY() + target.getBbHeight() / 2.0,
                                    target.getZ(),
                                    25,
                                    0.4,
                                    0.4,
                                    0.4,
                                    0.1
                            );

                            world.playSound(
                                    null,
                                    target.blockPosition(),
                                    SoundEvents.ITEM_BREAK,
                                    SoundSource.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                        }
                    }
                }
            }
        }

        // =========================
        // 无视护甲伤害
        // =========================
        float bypassDamage = damagePerHit * (ARMOR_BYPASS_PERCENT + (0.1F * shredderLevel));

        DamageSource magicSrc = new DamageSource(
                world.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.MAGIC),
                attacker
        );

        target.hurt(magicSrc, bypassDamage);

        // =========================
        // 自定义效果
        // =========================
        ResourceKey<MobEffect> key = ResourceKey.create(
                Registries.MOB_EFFECT,
                ResourceLocation.parse("sacredobsidian:irreconcilable_crack")
        );
        Holder<MobEffect> effectHolder = world.registryAccess()
                .registryOrThrow(Registries.MOB_EFFECT)
                .getHolder(key)
                .orElseThrow();

        target.addEffect(new MobEffectInstance(effectHolder, 100, 0));

        // =========================
        // 击退
        // =========================
        int kbLevel = getEnchantmentLevel(stack, world, Sacredobsidian.MODID + ":obsidian_enhanced");
        if (kbLevel > 0) {
            float kbStrength = 0.5F * kbLevel;
            double dx = attacker.getX() - target.getX();
            double dz = attacker.getZ() - target.getZ();
            target.knockback(kbStrength, dx, dz);
        }

        // =========================
        // 火焰附加
        // =========================
        int faLevel = getEnchantmentLevel(stack, world, Sacredobsidian.MODID + ":obsidian_enhanced");
        if (faLevel > 0) {
            int fireTicks = 4 * faLevel * 20;

            target.setRemainingFireTicks(
                    Math.max(target.getRemainingFireTicks(), fireTicks)
            );

            if (world instanceof ServerLevel server) {
                server.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(),
                        target.getY() + target.getBbHeight() / 2.0,
                        target.getZ(),
                        10,
                        0.3, 0.5, 0.3,
                        0.01
                );
            }

            playSoundAt(world, target.getX(), target.getY(), target.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        world.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);

        // =========================
        // 破盾
        // =========================
        if (target instanceof Player targetPlayer) {
            if (targetPlayer.isBlocking()) {

                targetPlayer.getCooldowns().addCooldown(
                        Items.SHIELD,
                        120
                );

                targetPlayer.stopUsingItem();

                world.playSound(
                        null,
                        targetPlayer.blockPosition(),
                        SoundEvents.SHIELD_BREAK,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }

        // ===== 命中音效 =====
        world.playSound(
                null,
                target.blockPosition(),
                SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }
    
    private static int getDamageToArmor(LivingEntity target, int maxDur, int formula) {
        int maxPerHit = (int) (maxDur * 0.08F); // 单次最多 8%

        int damageToArmor = Math.max(1, formula);
        damageToArmor = Math.min(damageToArmor, maxPerHit);

        // ===== 防止同 tick 多段伤害过强 =====
        if (target.invulnerableTime > 0) {
            float reductionFactor = 0.7F;
            damageToArmor = Math.max(
                    1,
                    Math.round(damageToArmor * reductionFactor)
            );
        }
        return damageToArmor;
    }

    private static @NotNull EquipmentSlot getEquipmentSlot(Player attacker, LivingEntity target) {
        double relativeY = attacker.getY() - target.getY();
        double heightRatio = relativeY / target.getBbHeight();

        EquipmentSlot hitSlot;

        if (heightRatio > 0.8) {
            hitSlot = EquipmentSlot.HEAD;
        } else if (heightRatio > 0.5) {
            hitSlot = EquipmentSlot.CHEST;
        } else if (heightRatio > 0.2) {
            hitSlot = EquipmentSlot.LEGS;
        } else {
            hitSlot = EquipmentSlot.FEET;
        }
        return hitSlot;
    }

    private BlockState getArmorParticleState(ItemStack stack) {

        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return Blocks.STONE.defaultBlockState();
        }

        Holder<ArmorMaterial> material = armor.getMaterial();

        if (material.is(ArmorMaterials.DIAMOND)) {
            return Blocks.DIAMOND_BLOCK.defaultBlockState();
        }
        if (material.is(ArmorMaterials.NETHERITE)) {
            return Blocks.NETHERITE_BLOCK.defaultBlockState();
        }
        if (material.is(ArmorMaterials.IRON)) {
            return Blocks.IRON_BLOCK.defaultBlockState();
        }
        if (material.is(ArmorMaterials.GOLD)) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (material.is(ArmorMaterials.LEATHER)) {
            return Blocks.BROWN_WOOL.defaultBlockState();
        }

        return Blocks.STONE.defaultBlockState();
    }

    public int getEnchantmentLevel(@NotNull ItemStack stack, Level level, @NotNull String enchantId) {
        return super.getEnchantmentLevel(stack, level, enchantId);
    }

    private static void playSoundAt(Level world, double x, double y, double z, net.minecraft.core.Holder.Reference<net.minecraft.sounds.SoundEvent> soundHolder, SoundSource source, float volume, float pitch) {
        // Holder.Reference.value() 返回实际的 SoundEvent
        net.minecraft.sounds.SoundEvent sound = soundHolder.value();
        if (world instanceof ServerLevel server) {
            // server.playSound 有 (Player, double, double, double, SoundEvent, SoundSource, float, float)
            server.playSound(null, x, y, z, sound, source, volume, pitch);
        } else {
            // Level 也有坐标版本的 playSound，传入 null 让所有玩家听到
            world.playSound(null, x, y, z, sound, source, volume, pitch);
        }
    }

    private LivingEntity findTargetEntityAtPosition(Level world, BlockPos pos, Player player) {
        // 使用放大 AABB 以提高命中稳定性 // Use an enlarged AABB to improve hit stability
        return world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(0.5D), e -> e != player)
                .stream().findFirst().orElse(null);
    }

    private BlockHitResult rayTrace(Level world, Player player, int maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().scale(maxDistance);
        return world.clip(new ClipContext(eyePos, eyePos.add(look), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    public static void tickAllDimensions(ServerLevel mainWorld) {
        for (ServerLevel world : mainWorld.getServer().getAllLevels()) {
            tick(world);
        }
    }

    public static void tick(Level world) {

        if (world instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);

            // 复制一份 key set 安全遍历（避免并发修改异常） // Copy a key set for safe iteration (to avoid concurrent modification exceptions)
            Map<BlockPos, Integer> snapshot = new HashMap<>(data.getObsidianData());

            for (Map.Entry<BlockPos, Integer> entry : snapshot.entrySet()) {
                BlockPos pos = entry.getKey();
                int ticksLeft = entry.getValue();

                BlockState blockState = world.getBlockState(pos);
                if (ticksLeft <= 0 && blockState.is(Blocks.OBSIDIAN)) {
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    world.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // 取出 owner UUID 并广播新版 packet（包含 owner UUID 与 ownerPos 快照） // Retrieve the owner UUID and broadcast the new version of the packet (including the owner UUID and ownerPos snapshot)
                    data.getOwner(pos).ifPresent(uuid -> {
                        // 获取 owner 的位置快照，如果 owner 离线就用碎块原点 start 作为快照 // Get a location snapshot of the owner; if the owner is offline, use the chunk origin start as the snapshot.
                        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        Vec3 ownerPosSnapshot = start;
                        Player ownerPlayer = serverLevel.getPlayerByUUID(uuid);
                        if (ownerPlayer != null) {
                            ownerPosSnapshot = ownerPlayer.position().add(0, 1.0, 0);
                        }

                        // 广播给所有客户端：客户端用 ownerUUID 与 ownerPosSnapshot 来驱动本地渲染 // Broadcast to all clients: The client uses ownerUUID and ownerPosSnapshot to drive local rendering.
                        PacketDistributor.sendToAllPlayers(
                                new ClientSpawnObsidianEffectPacket(
                                        start,
                                        uuid,
                                        ownerPosSnapshot
                                )
                        );
                    });

                    notifyClientForParticles(world, pos);

                    data.removeObsidian(pos);
                    data.removeOwner(pos);

                } else if (blockState.is(Blocks.OBSIDIAN)) {
                    data.putObsidian(pos, ticksLeft - 1); // 递减寿命并 setDirty // Decrease lifespan and setDirty
                } else {
                    data.removeObsidian(pos);
                    data.removeOwner(pos);
                }
            }
        }
    }

    private static void notifyClientForParticles(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    20,
                    0.5D, 0.5D, 0.5D,
                    0.1D
            );
        }
    }
}

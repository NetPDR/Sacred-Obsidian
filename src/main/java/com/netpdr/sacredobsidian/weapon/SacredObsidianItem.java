package com.netpdr.sacredobsidian.weapon;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.blockentity.SacredObsidianBlockEntity;
import com.netpdr.sacredobsidian.data.SacredObsidianData;
import com.netpdr.sacredobsidian.registry.ModEffects;
import com.netpdr.sacredobsidian.registry.ModEnchantments;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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

import net.minecraftforge.network.PacketDistributor;
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
    public boolean isCorrectToolForDrops(BlockState state) {
        return super.isCorrectToolForDrops(state); // 继承默认行为（保留） // Inherit default behavior (retain)
    }

    private static final int BASE_MAX_DISTANCE = 15; // 基础延伸块数 // Number of Base Extension Blocks
    private static final int EXTRA_PER_LEVEL = 5; // 每级附魔额外延伸块数 // Additional extension blocks per enchantment level
    public static final float OBSIDIAN_DAMAGE = 25.0F; // 黑曜石造成基础伤害 // Obsidian deals base damage
    private static final double MAX_DIRECTION_CHANGE = 0.1; // 延伸时随机扰动幅度 // Random perturbation amplitude during extension
    public static final int COOLDOWN_TIME = 30; // 冷却（tick） // Cooldown (tick)
    public static final int OBSIDIAN_LIFETIME = 60; // 黑曜石存活时间（tick） // Obsidian survival time (tick)

    // NBT keys （为了在 ItemStack 的 NBT 中存储状态） // NBT keys (used to store state in an ItemStack's NBT)
    private static final String TAG_SECOND_FORM = "SecondForm"; // 是否为第二形态 // Is it the second form?
    private static final String TAG_IS_EXTENDING = "IsExtending"; // 第二形态是否处于延伸中 // Is the second form in the process of extending?
    private static final String TAG_EXTEND_BLOCKS = "ExtendBlocks"; // 已延伸方块计数 // Extended block count
    private static final String TAG_MAX_BLOCKS = "MaxBlocks"; // 最大可延伸方块数 // Maximum Extendable Blocks
    private static final String TAG_CURSOR_X = "CursorX"; // 游标 X // Cursor X
    private static final String TAG_CURSOR_Y = "CursorY"; // 游标 Y // Cursor Y
    private static final String TAG_CURSOR_Z = "CursorZ"; // 游标 Z // Cursor Z
    private static final String TAG_POS_X = "PosX"; // 上次放置方块的中心 X // Center X of the last placed block
    private static final String TAG_POS_Y = "PosY"; // 上次放置方块的中心 Y（通常为中心或眼位） // The Y-coordinate of the last placed block's center (usually the center or eye level)
    private static final String TAG_POS_Z = "PosZ"; // 上次放置方块的中心 Z // Center Z of the last placed block
    private static final String TAG_REVERSE = "ReverseMode"; // 反向延伸开关（由客户端按键包写入） // Reverse extension switch (written by the client key packet)
    private static final String TAG_LAST_USE = "LastUseTime"; // 最近使用时间用于冷却 // Recent usage time for cooling
    private static final String TAG_COOLDOWN_TICKS = "CooldownTicks"; // 可视化冷却计数（未严格必要） // Visualized cooling count (not strictly necessary)
    public static final String TAG_DAMAGE_ENABLED = "DamageEnabled"; // 热键控制的伤害开关，默认视为 true（若无键）

    // 子状态
    private static final String TAG_SUB_STATE = "SubState";

    private static final String TAG_FRACTIONAL_LIFTED = "FractionalLifted";

    private static final float ARMOR_BYPASS_PERCENT = 0.3F; // 30% 固定穿甲

    public SacredObsidianItem(Properties properties) {
        super(Tiers.NETHERITE, properties.stacksTo(1)); // 物品最大堆叠为 1 // The maximum stack size for this item is 1
    }

    public InteractionResultHolder<ItemStack> handleRightClick(
            Level world, Player player, InteractionHand hand
    ) {
        ItemStack itemStack = player.getItemInHand(hand); // 获取玩家手中的 ItemStack // Get the ItemStack in the player's hand

        CompoundTag tag = itemStack.getOrCreateTag(); // 获取或创建 NBT // Get or create NBT

        // Shift + Right Click 切换形态（服务端检测） // Shift + Right Click to switch form (server-side detection)
        if (!world.isClientSide && player.isShiftKeyDown()) {
            boolean secondForm = tag.getBoolean(TAG_SECOND_FORM); // 读取当前形态 // Read current form
            tag.putBoolean(TAG_SECOND_FORM, !secondForm); // 切换形态 // Switch Form

            // 切换时终止延伸 // Terminate extension when switching
            tag.putBoolean(TAG_IS_EXTENDING, false);
            // 切换时清理 ReverseMode（防止残留） // Clear ReverseMode when switching (to prevent remnants)
            if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);

            if (!secondForm) {
                player.displayClientMessage(Component.translatable("message.sacred_obsidian.switch_to_second"), true); // 通知玩家切换到第二形态 // Notify the player to switch to the second form
            } else {
                player.displayClientMessage(Component.translatable("message.sacred_obsidian.switch_to_first"), true); // 通知玩家切换到第一形态 // Notify the player to switch to the first form
            }

            // 触发成就（advancement） // Trigger achievement (advancement)
            if (player instanceof ServerPlayer serverPlayer) {
                ResourceLocation id = new ResourceLocation("sacredobsidian", "switch_form"); // advancement 的 id // advancement's id
                Advancement adv = serverPlayer.server.getAdvancements().getAdvancement(id); // 取回 advancement // Retrieve advancement
                if (adv != null) {
                    AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(adv); // 取得进度 // Get progress
                    if (!progress.isDone()) {
                        for (String criteria : progress.getRemainingCriteria()) {
                            serverPlayer.getAdvancements().award(adv, criteria); // 直接发放所有未完成的条件（快速完成） // Directly grant all incomplete conditions (quick complete)
                        }
                    }
                }
            }

            return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide()); // 返回成功（区分客户端/服务端） // Return success (distinguish between client/server)
        }

        if (!world.isClientSide) {
            long currentTime = world.getGameTime(); // 世界时间（tick） // World Time (tick)
            long lastUse = tag.getLong(TAG_LAST_USE); // 上次使用时间 // Last time used

            if (currentTime - lastUse < COOLDOWN_TIME) {
                return InteractionResultHolder.fail(itemStack); // 冷却中则失败 // Fails if cooling
            }

            tag.putLong(TAG_LAST_USE, currentTime); // 写入新的使用时间 // Write the new usage time
            tag.putInt(TAG_COOLDOWN_TICKS, COOLDOWN_TIME); // 写入冷却显示值 // Write the cooldown display value

            if (!tag.getBoolean(TAG_SECOND_FORM)) {
                // 第一形态：一次性沿直线延伸 // Form 1: One-time straight-line extension
                Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(itemStack); // 取附魔 // Get enchantment
                int reachLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_REACH.get(), 0); // 读取伸长附魔等级 // Read the level of the Unbreaking enchantment
                int maxDistance = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL; // 计算可延伸最大距离 // Calculate the maximum extendable distance

                world.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F); // 播放声效 // Play sound effect

                BlockHitResult hitResult = rayTrace(world, player, maxDistance); // 射线检测目标 // Ray casting target
                BlockPos hitPos = hitResult.getBlockPos(); // 命中方块位置 // Hit block position

                extendObsidianPathAndDamage(world, player, hitPos, itemStack, maxDistance); // 执行一次性延伸并检测伤害 // Perform a one-time extension and detect damage

            } else {
                // 第二形态：进入持续延伸模式（在 inventoryTick 中处理延伸） // Second form: Enter continuous extension mode (handle extension in inventoryTick)
                Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(itemStack);
                int reachLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_REACH.get(), 0);
                int maxBlocks = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL; // 计算最大块数 // Calculate the maximum number of blocks

                if (tag.contains(TAG_SUB_STATE)) tag.remove(TAG_SUB_STATE);

                tag.putBoolean(TAG_IS_EXTENDING, true); // 标记为延伸中 // Marked as extending
                tag.putInt(TAG_SUB_STATE, 1); // 进入 LIFT
                tag.putInt(TAG_EXTEND_BLOCKS, 0); // 重置已延伸计数 // Reset extended count
                tag.putInt(TAG_MAX_BLOCKS, maxBlocks); // 写入最大块数 // Write maximum number of blocks

                // 初始化放置中心为玩家位置（会在 inventoryTick 中调整首块放置距离） // Initialize the placement center at the player's position (the distance for the first block placement will be adjusted in inventoryTick)
                tag.putDouble(TAG_POS_X, player.getX());
                tag.putDouble(TAG_POS_Y, player.getEyeY());
                tag.putDouble(TAG_POS_Z, player.getZ());

                // 清除 cursor（首次在 inventoryTick 中初始化） // Clear cursor (initialized for the first time in inventoryTick)
                tag.remove(TAG_CURSOR_X);
                tag.remove(TAG_CURSOR_Y);
                tag.remove(TAG_CURSOR_Z);

                // 清理 ReverseMode（防止遗留） // Clean up ReverseMode (to prevent leftover issues)
                if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);

                world.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        return handleRightClick(world, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        handleRightClick(context.getLevel(), player, context.getHand());
        return InteractionResult.CONSUME; // 吃掉原版交互
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(
            @NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand
    ) {
        handleRightClick(player.level(), player, hand);
        return InteractionResult.CONSUME; // 吃掉村民 / 狗 / 一切实体
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag(); // 获取或创建 NBT // Get or create NBT
        tag.putBoolean(TAG_IS_EXTENDING, false); // 停止任何正在进行的延伸 // Stop any ongoing extensions
        tag.remove(TAG_SUB_STATE);
        if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE); // 清理反向标志 // Clear reverse flag
        return true; // 允许扔出
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected); // 保留基类逻辑 // Retain base class logic

        // 只在服务端玩家上处理 // Only process on server-side players
        if (!(entity instanceof Player player) || world.isClientSide) return;
        if (!selected) return;

        // 玩家死亡、旁观者或实体移除时停止延伸 // Stop extending when the player dies, or when a spectator or entity is removed
        if (!player.isAlive() || player.isSpectator() || player.isRemoved()) {
            CompoundTag t = stack.getOrCreateTag();
            t.putBoolean(TAG_IS_EXTENDING, false);
            t.remove(TAG_SUB_STATE);
            if (t.contains(TAG_REVERSE)) t.remove(TAG_REVERSE);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag(); // 取得 ItemStack NBT // Get ItemStack NBT

        if (!tag.getBoolean(TAG_SECOND_FORM) || !tag.getBoolean(TAG_IS_EXTENDING)) return; // 仅在第二形态且延伸中才处理 // Only handle in the second form and during extension

        // ===== 子状态 =====
        int subState = tag.getInt(TAG_SUB_STATE);

        if (subState == 1) { // LIFT
            handleLift(world, player, stack, tag);
            return;
        }

        int blocks = tag.getInt(TAG_EXTEND_BLOCKS); // 已延伸方块数量 // Number of extended blocks
        int maxBlocks = tag.getInt(TAG_MAX_BLOCKS); // 最大允许的方块数量 // Maximum allowed number of blocks
        if (blocks >= maxBlocks) {
            tag.putBoolean(TAG_IS_EXTENDING, false);
            tag.remove(TAG_SUB_STATE);
            if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);
            return;
        }

        // ========== 调整参数（可按需调节） ==========
        final double STEP_DISTANCE = 1.0;           // 方块间隔（格） // Block spacing (cells)
        final double MAX_MOVE_PER_TICK = 0.85;      // 游标每 Tick 最大移动距离 // Maximum cursor movement per tick
        final double DESIRED_DISTANCE_MULT = blocks + 1; // 期望距离倍数（基于已放数量） // Expected distance multiplier (based on the number already placed)
        final double QUICK_ACCEPT_ALIGNMENT = 0.88; // 方向快速接受阈值 // Direction quick acceptance threshold
        // ===========================================

        Vec3 eye = player.getEyePosition(1.0F); // 玩家眼位 // Player's viewpoint
        Vec3 look = player.getLookAngle().normalize(); // 视线单位向量 // View direction unit vector

        // ReverseMode 由客户端按键按下/释放发包控制写入 ItemStack 的 NBT // ReverseMode is controlled by the client sending packets when keys are pressed/released to write NBT to the ItemStack
        boolean reverse = tag.getBoolean(TAG_REVERSE); // 读取反向标志 // Read reverse flag
        if (reverse) {
            look = look.scale(-1.0).normalize(); // 反向视线 // Reverse line of sight
        }

        Vec3 desired = eye.add(look.scale(DESIRED_DISTANCE_MULT * STEP_DISTANCE)); // 期望的游标位置 // Expected cursor position

        // 读取或初始化 cursor（游标） // Read or initialize the cursor
        Vec3 cursor;
        if (tag.contains(TAG_CURSOR_X)) {
            cursor = new Vec3(tag.getDouble(TAG_CURSOR_X), tag.getDouble(TAG_CURSOR_Y), tag.getDouble(TAG_CURSOR_Z));
        } else {
            // 首次设置 cursor 到 desired（避免瞬移） // Set the cursor to 'desired' for the first time (to avoid teleportation)
            cursor = desired;
            tag.putDouble(TAG_CURSOR_X, cursor.x);
            tag.putDouble(TAG_CURSOR_Y, cursor.y);
            tag.putDouble(TAG_CURSOR_Z, cursor.z);
        }

        // 平滑限速移动 cursor 朝 desired // Smoothly move cursor towards desired
        Vec3 delta = desired.subtract(cursor);
        if (delta.length() > 1e-6) {
            Vec3 move = delta.length() > MAX_MOVE_PER_TICK ? delta.normalize().scale(MAX_MOVE_PER_TICK) : delta;
            cursor = cursor.add(move);
            tag.putDouble(TAG_CURSOR_X, cursor.x);
            tag.putDouble(TAG_CURSOR_Y, cursor.y);
            tag.putDouble(TAG_CURSOR_Z, cursor.z);
        }

        // 如果尚未放置任何方块（blocks==0），放第一个方块时要确保不会放在玩家身上 // If no blocks have been placed yet (blocks==0), make sure the first block is not placed on the player
        if (blocks == 0) {
            double startDist = 1.5; // 起始距离，避免放在玩家格 // Starting distance, avoid placing on player grid
            BlockPos targetPos = BlockPos.containing(eye.add(look.scale(startDist)));

            int tries = 0;
            while (tries < 6) {
                Vec3 center = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
                if (!player.getBoundingBox().contains(center)) break; // 如果不在玩家包围盒内则可以放 // Can be placed if not within the player's bounding box
                startDist += 0.5; // 增加距离并重试 // Increase the distance and try again
                targetPos = BlockPos.containing(eye.add(look.scale(startDist)));
                tries++;
            }

            boolean canPlaceHere = canPlaceIgnoreOwn(world, targetPos); // 检查是否可放 // Check if it can be placed
            if (canPlaceHere) {
                placeObsidianAt(world, stack, tag, player, targetPos, look); // 放置首块 // Place the first block
            } else {
                tag.putBoolean(TAG_IS_EXTENDING, false); // 无法放则取消延伸 // Cancel extension if unable to place
                if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);
            }
            return;
        }

        // 上次已放方块中心 // Last time the block was placed at the center
        Vec3 lastCenter = new Vec3(tag.getDouble(TAG_POS_X), tag.getDouble(TAG_POS_Y), tag.getDouble(TAG_POS_Z));
        BlockPos lastPos = BlockPos.containing(lastCenter);

        // 游标距离阈值判断 // Cursor distance threshold check
        if (cursor.distanceTo(lastCenter) < STEP_DISTANCE - 1e-6) return; // 未到达阈值则不放 // Do not release if the threshold is not reached

        // 选择相邻格（允许穿过自己放的 obsidian） // Select adjacent tiles (allow passing through obsidian placed by yourself)
        BlockPos nextPos = chooseAdjacentTowardsCursor(world, lastPos, cursor, look, QUICK_ACCEPT_ALIGNMENT, player);
        if (nextPos == null) {
            tag.putBoolean(TAG_IS_EXTENDING, false);
            if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);
            return;
        }

        placeObsidianAt(world, stack, tag, player, nextPos, look); // 放置下一个方块 // Place the next block
    }

    /**
     * 一次性延伸（第一形态）的逻辑。 The logic of one-time extension (first form).
     * 从玩家到目标点之间沿直线放置黑曜石，并对命中的实体造成伤害。 Place obsidian in a straight line from the player to the target point, dealing damage to any entity hit.
     */
    private void extendObsidianPathAndDamage(Level world, Player player, BlockPos targetPos, ItemStack stack, int maxDistance) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean damageEnabled = !tag.contains(TAG_DAMAGE_ENABLED) || tag.getBoolean(TAG_DAMAGE_ENABLED);

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

    private void handleLift(Level world, Player player, ItemStack stack, CompoundTag tag) {

        int blocks = tag.getInt(TAG_EXTEND_BLOCKS);
        int maxBlocks = tag.getInt(TAG_MAX_BLOCKS);

        // ===== 延伸额度检查 =====
        if (blocks >= maxBlocks) {
            // 清理 fractional 标志与子状态，结束延伸
            if (tag.contains(TAG_FRACTIONAL_LIFTED)) tag.remove(TAG_FRACTIONAL_LIFTED);
            tag.putBoolean(TAG_IS_EXTENDING, false);
            tag.remove(TAG_SUB_STATE);
            return;
        }

        // ===== 视线必须明显朝下 =====
        if (player.getLookAngle().y > -0.6) {
            tag.putInt(TAG_SUB_STATE, 2);
            return;
        }

        // ===== 脚下检测 =====
        BlockPos groundPos = player.blockPosition().below();
        BlockState groundState = world.getBlockState(groundPos);

        if (groundState.isAir() || groundState.canBeReplaced()) {
            tag.putInt(TAG_SUB_STATE, 2);
            return;
        }

        // ===== 顶头检测 =====
        BlockPos headPos = player.blockPosition().above();
        if (HeadInspection(world, tag, headPos)) return;

        // ===== 初始放置位置 =====
        BlockPos placePos = player.blockPosition();

        // ===== 判断是否处于小数高度 =====
        double yFrac = player.getY() - Math.floor(player.getY());
        boolean isFractionalHeight = yFrac > 1e-4;

        // 读取是否已经做过“首块抬升”（会话内只允许一次）
        boolean alreadyFractionalLifted = tag.getBoolean(TAG_FRACTIONAL_LIFTED);

        boolean needDoubleLiftThisTick = false;

        // 只要玩家处于小数高度且脚下不可替换方块，并且本次延伸会话尚未执行过首块抬升
        if (isFractionalHeight && !groundState.canBeReplaced() && !alreadyFractionalLifted) {
            placePos = placePos.above(); // 放置提高一格（首块）
            needDoubleLiftThisTick = true;
            // 标记已做过首块抬升，直到本次延伸结束才清除
            tag.putBoolean(TAG_FRACTIONAL_LIFTED, true);
        }

        // ===== 防止覆盖不可替代方块 =====
        if (HeadInspection(world, tag, placePos)) return;

        // ===== 玩家传送高度 =====
        double liftHeight = needDoubleLiftThisTick ? 2.0 : 1.0;

        // 使用 teleportTo
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
                tag,
                player,
                placePos,
                new Vec3(0.0, 1.0, 0.0)
        );
    }

    private boolean HeadInspection(Level world, CompoundTag tag, BlockPos headPos) {
        BlockState headState = world.getBlockState(headPos);

        if (!headState.isAir() && !headState.canBeReplaced()) {
            // 清理 fractional 标志并结束延伸
            if (tag.contains(TAG_FRACTIONAL_LIFTED)) tag.remove(TAG_FRACTIONAL_LIFTED);
            tag.putBoolean(TAG_IS_EXTENDING, false);
            tag.remove(TAG_SUB_STATE);
            return true;
        }
        return false;
    }

    /** placeObsidianAt 保持原逻辑：放方块、加入 SacredObsidianData、命中检测、更新 NBT */
    private void placeObsidianAt(Level world, ItemStack stack, CompoundTag tag, Player player, BlockPos pos, Vec3 dirForUpdate) {
        if (!world.isLoaded(pos)) {
            // 不在加载区，安全地停止延伸 // Not in the loading area, safely stop extending
            tag.putBoolean(TAG_IS_EXTENDING, false);
            if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);
            return;
        }

        // 防止放到玩家身上（额外保险） // Prevent placing on the player (extra precaution)
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (player.getBoundingBox().contains(center)) {
            tag.putBoolean(TAG_IS_EXTENDING, false);
            if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);
            return;
        }

        world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3); // 直接放置黑曜石方块 // Directly place obsidian blocks

        // 如果你未来把 obsidian 換成自定义方块并注册了 BlockEntity，这里会获取到并可初始化 // If in the future you replace obsidian with a custom block and register a BlockEntity, it can be obtained and initialized here
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof SacredObsidianBlockEntity sbe) {
            sbe.onPlacedBy(player); // 注意：你需要在 SacredObsidianBlockEntity 中实现 onPlacedBy，否则此调用会编译报错 // Note: You need to implement onPlacedBy in SacredObsidianBlockEntity, otherwise this call will cause a compilation error
        }

        if (world instanceof ServerLevel serverLevel) {
            SacredObsidianData data = SacredObsidianData.get(serverLevel);
            data.putObsidian(pos, OBSIDIAN_LIFETIME);   // 记录黑曜石寿命并 setDirty // Record obsidian lifespan and setDirty
            data.setOwner(pos, player.getUUID()); // 记录归属 // Record Ownership
        }

        LivingEntity target = findTargetEntityAtPosition(world, pos, player);

        // 伤害开关，默认（未写入 NBT 时）视为开启
        boolean damageEnabled = !tag.contains(TAG_DAMAGE_ENABLED) || tag.getBoolean(TAG_DAMAGE_ENABLED);

        if (target != null && damageEnabled) {
            // 命中并且允许伤害 -> 造成伤害并结束延伸（原行为）
            applyObsidianDamage(world, player, target, stack);
            tag.putBoolean(TAG_IS_EXTENDING, false);
            if (tag.contains(TAG_REVERSE)) tag.remove(TAG_REVERSE);
            return; // 命中实体则停止延伸 // Stop extending when hitting an entity
        }

        Vec3 nextCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        tag.putDouble(TAG_POS_X, nextCenter.x); // 更新上次放置位置
        tag.putDouble(TAG_POS_Y, nextCenter.y);
        tag.putDouble(TAG_POS_Z, nextCenter.z);
        tag.putInt(TAG_EXTEND_BLOCKS, tag.getInt(TAG_EXTEND_BLOCKS) + 1); // 已放数量 +1 // Quantity already placed 1

        // 保留 DirN 写入以便调试/未来使用 // Keep writing DirN for debugging/future use
        tag.putDouble("DirX", dirForUpdate.x);
        tag.putDouble("DirY", dirForUpdate.y);
        tag.putDouble("DirZ", dirForUpdate.z);
    }

    private void applyObsidianDamage(
            Level world,
            Player attacker,
            LivingEntity target,
            ItemStack stack
    ) {

        if (!target.isAlive()) return;

        // =========================
        // 基础伤害计算
        // =========================
        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack); // 取附魔 // Get enchantment
        int powerLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_POWER.get(), 0); // 读取强力等级 // Read power level
        int shredderLevel = enchMap.getOrDefault(ModEnchantments.ARMOR_SHREDDER.get(), 0);

        float damagePerHit = OBSIDIAN_DAMAGE + powerLevel * 5.0F; // 计算伤害 // Calculate damage

        // ===== 普通伤害 =====
        DamageSource normalSrc = world.damageSources().playerAttack(attacker);
        target.hurt(normalSrc, damagePerHit);

        // =========================
        // 精细护甲损耗（方向判定）
        // =========================
        if (!world.isClientSide && target.getArmorValue() > 0) {

            // ===== 创造模式跳过护甲损耗 =====
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

                    // ===== 精确背刺判定 =====
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

                    // ===== 爆头加成 =====
                    if (isHeadshot && hitSlot == EquipmentSlot.HEAD) {
                        formula *= 1.2F;
                    }

                    // ===== 最终耐久伤害 =====
                    int damageToArmor = getDamageToArmor(target, maxDur, Math.round(formula));

                    // ===== 破损预判（关键优化）=====
                    int newDamage = hitArmor.getDamageValue() + damageToArmor;

                    boolean willBreak = newDamage >= hitArmor.getMaxDamage();

                    // ===== 粒子用复制 =====
                    ItemStack copyForParticle = hitArmor.copy();

                    // ===== 执行损耗 =====
                    hitArmor.hurtAndBreak(damageToArmor, target, e -> {

                        e.broadcastBreakEvent(hitSlot);

                    });

                    // ===== 手动补充破损特效（防止极端情况丢失）=====
                    if (willBreak && world instanceof ServerLevel server) {

                        BlockState particleState =
                                getArmorParticleState(copyForParticle);

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
        target.addEffect(new MobEffectInstance(
                ModEffects.IRRECONCILABLE_CRACK.get(),
                100,
                0
        ));

        // =========================
        // 击退
        // =========================
        int kbLevel = EnchantmentHelper.getTagEnchantmentLevel(
                Enchantments.KNOCKBACK,
                stack
        );

        if (kbLevel > 0) {
            float kbStrength = 0.5F * kbLevel;
            double dx = attacker.getX() - target.getX();
            double dz = attacker.getZ() - target.getZ();
            target.knockback(kbStrength, dx, dz);
        }

        // =========================
        // 火焰附加
        // =========================
        int faLevel = EnchantmentHelper.getTagEnchantmentLevel(
                Enchantments.FIRE_ASPECT,
                stack
        );

        if (faLevel > 0) {
            int seconds = 4 * faLevel;
            target.setSecondsOnFire(seconds);

            if (world instanceof ServerLevel server) {
                server.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(),
                        target.getY() + (target.getBbHeight() / 2.0),
                        target.getZ(),
                        10,
                        0.3,
                        0.5,
                        0.3,
                        0.01
                );
            }

            world.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS,
                    0.7F,
                    1.0F
            );
        }

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

        ArmorMaterial material = armor.getMaterial();

        if (material == ArmorMaterials.DIAMOND) {
            return Blocks.DIAMOND_BLOCK.defaultBlockState();
        }
        if (material == ArmorMaterials.NETHERITE) {
            return Blocks.NETHERITE_BLOCK.defaultBlockState();
        }
        if (material == ArmorMaterials.IRON) {
            return Blocks.IRON_BLOCK.defaultBlockState();
        }
        if (material == ArmorMaterials.GOLD) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (material == ArmorMaterials.LEATHER) {
            return Blocks.BROWN_WOOL.defaultBlockState();
        }

        return Blocks.STONE.defaultBlockState();
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
                        Sacredobsidian.CHANNEL.send(
                                PacketDistributor.ALL.noArg(),
                                new com.netpdr.sacredobsidian.client.entity.ClientSpawnObsidianEffectPacket(
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
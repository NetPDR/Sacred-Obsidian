package com.netpdrmod.weapon;

import com.netpdrmod.Netpdrmod;
import com.netpdrmod.blockentity.SacredObsidianBlockEntity;
import com.netpdrmod.client.ClientSpawnObsidianEffectPacket;
import com.netpdrmod.data.SacredObsidianData;
import com.netpdrmod.registry.ModEffect;
import com.netpdrmod.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SacredObsidianItem extends BaseItem {

    @Override
    protected String getDescriptionKey() {
        return "item.sacred_obsidian.tooltip"; // 为这个物品返回对应的描述键 // Return the corresponding description key for this item
    }

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
    private static final int EXTRA_PER_LEVEL   = 5;  // 每级多延伸 5 格 //Each level extends by 5 more tiles
    private static final float OBSIDIAN_DAMAGE = 25.0F;  // 黑曜石伤害 // Obsidian damage
    private static final double MAX_DIRECTION_CHANGE = 0.1;  // 随机方向变化幅度 // Random direction change amplitude
    public static final int COOLDOWN_TIME = 30;  // 冷却时间 (30 ticks = 1.5秒) // Cooldown time (30 ticks = 1.5 seconds)
    private static final int OBSIDIAN_LIFETIME = 60;  // 黑曜石的存在时间 (60 ticks = 3秒) // Obsidian's lifetime (60 ticks = 3 seconds)

    public SacredObsidianItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!world.isClientSide) {

            // 设置冷却时间 // Set cooldown time
            long currentTime = world.getGameTime();
            CompoundTag tag = itemStack.getOrCreateTag();
            long lastUse = tag.getLong("LastUseTime");

            if (currentTime - lastUse < COOLDOWN_TIME) {

                return InteractionResultHolder.fail(itemStack);
            }

            // 设置新的使用时间 // Set new usage time
            tag.putLong("LastUseTime", currentTime);
            tag.putInt("CooldownTicks", COOLDOWN_TIME);

            // 读取附魔等级，算出最大延伸距离 Read the enchantment level and calculate the maximum extension distance
            Map<Enchantment,Integer> enchMap = EnchantmentHelper.getEnchantments(itemStack);
            int reachLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_REACH.get(), 0);
            int maxDistance = BASE_MAX_DISTANCE + reachLevel * EXTRA_PER_LEVEL;

            // 播放铁砧声音 // Play anvil sound
            world.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);

            // 射线检测以获取命中位置 // Ray tracing to get hit position
            BlockHitResult hitResult = rayTrace(world, player, maxDistance);
            BlockPos hitPos = hitResult.getBlockPos();

            // 延伸黑曜石并处理目标实体 // Extend obsidian and handle target entity
            extendObsidianPathAndDamage(world, player, hitPos, itemStack, maxDistance);

        }

        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }

    /**
     * 扩展黑曜石路径并对目标实体造成伤害 Extend the obsidian path and deal damage to the target entity
     *
     * @param world     当前游戏世界 Current game world
     * @param player    当前使用物品的玩家 Player using the item
     * @param targetPos 射线检测命中的位置 Hit position from ray tracing
     * @param maxDistance 本次可以延伸的最大距离 The maximum distance that can be extended this time
     */
    private void extendObsidianPathAndDamage(Level world, Player player, BlockPos targetPos, ItemStack stack, int maxDistance) {

        // 先读取 Obsidian Power 等级 // Read the Obsidian Power grade first
        Map<Enchantment,Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        int powerLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_POWER.get(), 0);

        // 计算每次造成的伤害：基础 + 每级5点 // Calculate the damage dealt each time: base + 5 points per level
        float damagePerHit = OBSIDIAN_DAMAGE + powerLevel * 5.0F;

        Random random = new Random();
        Vec3 currentPos = player.position();
        Vec3 direction = Vec3.atCenterOf(targetPos).subtract(currentPos).normalize();
        SacredObsidianData data = SacredObsidianData.get((ServerLevel) world); // 获取数据存储 // Get data store
        data.setPlayerUuid(player.getUUID());  // 设置玩家的 UUID

        for (int i = 0; i < maxDistance; i++) {
            direction = applyRandomDirectionChange(direction, random);
            currentPos = currentPos.add(direction);
            BlockPos nextPos = BlockPos.containing(currentPos.x, currentPos.y, currentPos.z);

            BlockState blockState = world.getBlockState(nextPos);
            // 放置黑曜石并附加 BlockEntity // Place obsidian and attach BlockEntity
            if (world.isEmptyBlock(nextPos) || blockState.canBeReplaced()) {
                world.setBlock(nextPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                world.setBlockEntity(new SacredObsidianBlockEntity(nextPos, Blocks.OBSIDIAN.defaultBlockState()));
                data.getObsidianData().put(nextPos, OBSIDIAN_LIFETIME); // 直接存储到数据 // Directly store to data
            }

            LivingEntity target = findTargetEntityAtPosition(world, nextPos, player);
            if (target != null) {
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
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle().scale(maxDistance);
        Vec3 traceEnd = eyePosition.add(lookVector);
        ClipContext context = new ClipContext(eyePosition, traceEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return world.clip(context);
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
            Map<BlockPos, Integer> obsidianBlocks = data.getObsidianData();

            Iterator<Map.Entry<BlockPos, Integer>> iterator = obsidianBlocks.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = iterator.next();
                BlockPos pos = entry.getKey();
                int ticksLeft = entry.getValue();

                // 确保当前方块是 SacredObsidianItem 放置的黑曜石，并检查其剩余时间 // Ensure the current block is the obsidian placed by SacredObsidianItem, and check its remaining time
                BlockState blockState = world.getBlockState(pos);
                if (ticksLeft <= 0 && blockState.is(Blocks.OBSIDIAN)) {
                    // 移除黑曜石方块并播放挖掘音效 // Remove the obsidian block and play the digging sound
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    world.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // 获取玩家 UUID // Get the player's UUID
                    UUID playerUuid = data.getPlayerUuid();
                    Player player = serverLevel.getPlayerByUUID(playerUuid);

                    //if (player != null) {
                        //SacredObsidianEntity obsidianEntity = new SacredObsidianEntity(serverLevel, pos.getX(), pos.getY(), pos.getZ());
                        //obsidianEntity.setOwner(player);  // 设置玩家为拥有者 // Set the player as the owner
                        //serverLevel.addFreshEntity(obsidianEntity);  // 将黑曜石实体添加到世界中 // Add the obsidian entity to the world
                    //}

                    // 如果玩家存在，则生成黑曜石实体并设置其拥有者 // If the player exists, generate the Sacred Obsidian entity and set its owner
                    if (player != null && !world.isClientSide) {
                        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        Vec3 target = player.position().add(0, 1.0, 0);
                        Netpdrmod.CHANNEL.send(
                                PacketDistributor.ALL.noArg(),
                                new ClientSpawnObsidianEffectPacket(start, target, true)
                        );
                    }

                    // 通知客户端生成粒子效果 // Notify the client to generate particle effects
                    notifyClientForParticles(world, pos);

                    iterator.remove();  // 移除已处理的条目 // Remove the processed entry
                } else if (blockState.is(Blocks.OBSIDIAN)) {
                    // 每次 tick 只减少 1 // Decrease by 1 each tick
                    entry.setValue(ticksLeft - 1);
                } else {
                    // 如果方块不是黑曜石，直接移除记录，防止误删其他方块 // If the block is not obsidian, remove the record directly to prevent incorrect deletion of other blocks
                    iterator.remove();
                }
            }
            // 更新数据回保存 // Update data and save
            data.setObsidianData(obsidianBlocks);
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

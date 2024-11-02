package com.netpdrmod.weapon;

import com.netpdrmod.blockentity.SacredObsidianBlockEntity;
import com.netpdrmod.registry.ModEffect;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class SacredObsidianItem extends BaseItem {

    @Override
    protected String getDescriptionKey() {
        return "item.sacred_obsidian.tooltip"; // Return the corresponding description key for this sword
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        // If the block can be mined with a pickaxe, use the mining efficiency of the Netherite pickaxe
        //  mining speed
        float netheriteEfficiency = 9.0F;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? netheriteEfficiency : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        // Check if the block can be mined with a pickaxe; if so, return true
        return super.isCorrectToolForDrops(state);
    }

    private static final int MAX_DISTANCE = 15;  // Maximum extension distance
    private static final float OBSIDIAN_DAMAGE = 25.0F;  // Obsidian damage
    private static final double MAX_DIRECTION_CHANGE = 0.1;  // Random direction change amplitude
    private static final int COOLDOWN_TIME = 30;  // Cooldown time (30 ticks = 1.5 seconds)
    private static final int OBSIDIAN_LIFETIME = 60;  // Obsidian's lifetime (60 ticks = 3 seconds)

    // Store the placed obsidian blocks and their remaining time, with unique identifiers
    private static final Map<BlockPos, Integer> obsidianBlocksToRemove = new HashMap<>();
    // Used to mark obsidian placed by SacredObsidianItem

    public SacredObsidianItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            // Play anvil sound
            world.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.0F);

            // Ray tracing to get hit position
            BlockHitResult hitResult = rayTrace(world, player);
            BlockPos hitPos = hitResult.getBlockPos();

            // Extend obsidian and handle target entity
            extendObsidianPathAndDamage(world, player, hitPos);

            // Set cooldown time
            player.getCooldowns().addCooldown(this, COOLDOWN_TIME);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }

    /**
     * Extend the obsidian path and deal damage to the target entity
     *
     * @param world     Current game world
     * @param player    Player using the item
     * @param targetPos Hit position from ray tracing
     */
    private void extendObsidianPathAndDamage(Level world, Player player, BlockPos targetPos) {
        Random random = new Random();
        Vec3 currentPos = player.position();
        Vec3 direction = Vec3.atCenterOf(targetPos).subtract(currentPos).normalize();

        for (int i = 0; i < MAX_DISTANCE; i++) {
            direction = applyRandomDirectionChange(direction, random);
            currentPos = currentPos.add(direction);
            BlockPos nextPos = BlockPos.containing(currentPos.x, currentPos.y, currentPos.z);

            BlockState blockState = world.getBlockState(nextPos);
            // Place obsidian and attach BlockEntity
            if (world.isEmptyBlock(nextPos) || blockState.canBeReplaced()) {
                world.setBlock(nextPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                world.setBlockEntity(new SacredObsidianBlockEntity(nextPos, Blocks.OBSIDIAN.defaultBlockState()));
                obsidianBlocksToRemove.put(nextPos, OBSIDIAN_LIFETIME);
            }

            LivingEntity target = findTargetEntityAtPosition(world, nextPos, player);
            if (target != null) {
                DamageSource damageSource = world.damageSources().playerAttack(player);
                target.hurt(damageSource, OBSIDIAN_DAMAGE);
                target.addEffect(new MobEffectInstance(ModEffect.IRRECONCILABLE_CRACK.get(), 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                break;  // Stop extending
            }
        }
    }

    /**
     * Randomly change path direction
     *
     * @param direction Current direction vector
     * @param random    Random number generator
     * @return Changed direction vector
     */
    private Vec3 applyRandomDirectionChange(Vec3 direction, Random random) {
        double randomX = (random.nextDouble() - 0.5) * MAX_DIRECTION_CHANGE;
        double randomY = (random.nextDouble() - 0.5) * MAX_DIRECTION_CHANGE;
        double randomZ = (random.nextDouble() - 0.5) * MAX_DIRECTION_CHANGE;
        return direction.add(randomX, randomY, randomZ).normalize();
    }

    /**
     * Find entity at block position, excluding the player
     *
     * @param world  Current game world
     * @param pos    Block position
     * @param player Current player
     * @return Found entity, or null if none
     */
    @Nullable
    private LivingEntity findTargetEntityAtPosition(Level world, BlockPos pos, Player player) {
        return world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos)).stream()
                .filter(entity -> entity != player)  // Exclude the player
                .findFirst()
                .orElse(null);
    }

    /**
     * Ray trace, returning the hit block or entity
     *
     * @param world  Current game world
     * @param player Current player
     * @return The hit block
     */
    private BlockHitResult rayTrace(Level world, Player player) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle().scale(MAX_DISTANCE);
        Vec3 traceEnd = eyePosition.add(lookVector);
        ClipContext context = new ClipContext(eyePosition, traceEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return world.clip(context);
    }

    /**
     * In the tick method, remove obsidian and add particle effects.
     * Only remove obsidian blocks placed by SacredObsidianItem; it won't affect other blocks.
     *
     * @param world Current game world
     */
    public static void tick(Level world) {
        if (!world.isClientSide) {  // Ensure that blocks are only removed on the server side
            Iterator<Map.Entry<BlockPos, Integer>> iterator = obsidianBlocksToRemove.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = iterator.next();
                BlockPos pos = entry.getKey();
                int ticksLeft = entry.getValue();

                // Ensure the current block is the obsidian placed by SacredObsidianItem, and check its remaining time
                BlockState blockState = world.getBlockState(pos);
                if (ticksLeft <= 0 && blockState.is(Blocks.OBSIDIAN)) {
                    // Remove the obsidian block and play the digging sound
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    world.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // Notify the client to generate particle effects
                    notifyClientForParticles(world, pos);

                    iterator.remove();  // Remove the processed entry
                } else if (blockState.is(Blocks.OBSIDIAN)) {
                    // Decrease by 1 each tick
                    entry.setValue(ticksLeft - 1);
                } else {
                    // If the block is not obsidian, remove the record directly to prevent incorrect deletion of other blocks
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Notify the client to generate particle effects from the server side
     *
     * @param world Current game world
     * @param pos   Position for the particle effects
     */
    private static void notifyClientForParticles(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()), // Particle type
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, // Particle position
                    20, // Number of particles
                    0.5D, 0.5D, 0.5D, // Particle spread range
                    0.1D // Particle speed
            );
        }
    }
}

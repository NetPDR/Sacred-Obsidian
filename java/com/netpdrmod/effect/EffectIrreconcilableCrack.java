package com.netpdrmod.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class EffectIrreconcilableCrack extends BaseEffect {

    public EffectIrreconcilableCrack(MobEffectCategory type, int color, boolean isInstant) {
        super(type, color, isInstant);
    }

    // Buff隔多久生效一次 // How often the buff is applied
    @Override
    protected boolean canApplyEffect(int remainingTicks, int level) {
        return remainingTicks % 5 == 0;
    }

    // Buff在生物身上的效果 // Effect of the buff on the entity
    @Override
    public void applyEffectTick(@NotNull LivingEntity living, int amplified) {
        amplified++;
        Random ran = new Random();
        ran.nextInt(5);

        // 如果是玩家，加快其饥饿速度 // If the entity is a player, increase hunger exhaustion rate
        if (living instanceof Player) {
            ((Player) living).causeFoodExhaustion(2F * amplified);
        }

        // 每次受到1.0乘Buff等级的伤害 // Deals damage equal to 1.0 times the buff level
        living.hurt(living.damageSources().wither(), 1.0F * amplified);

        // 粒子效果 // Particle effect
        if (living.getCommandSenderWorld() instanceof ServerLevel world) {
            double x = living.getX();
            double y = living.getY();
            double z = living.getZ();
            int particleCount = 10 * amplified; // 粒子数量根据Buff等级调整 // Adjusts particle count based on buff level
            world.sendParticles(ParticleTypes.SCULK_SOUL, x, y + living.getBbHeight() * 0.5F, z, particleCount, living.getBbWidth() * 0.5, living.getBbHeight() * 0.5, living.getBbWidth() * 0.5, 0);
        }
    }

    @Override
    public boolean isBeneficial() {
        return false;
    }
}

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


    @Override
    protected void onEffectActive(@NotNull LivingEntity living, int amplified) {
        amplified++; // Buff等级从0开始，因此+1表示实际强度 // // Buff level starts from 0, so +1 represents the actual strength
        Random ran = new Random();

        // 如果是玩家，加快其饥饿速度 // // If it's a player, increase their hunger rate
        if (living instanceof Player) {
            ((Player) living).causeFoodExhaustion(2F * amplified);
        }

        // 每次受到 1.0 乘以 Buff 等级的伤害 // Take 1.0 damage multiplied by the Buff level each time
        living.hurt(living.damageSources().wither(), 1.0F * amplified);

        // 粒子效果
        if (living.getCommandSenderWorld() instanceof ServerLevel world) {
            double x = living.getX();
            double y = living.getY();
            double z = living.getZ();
            int particleCount = 10 * amplified; // 根据Buff等级调整粒子数量 // Particle effect
            world.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    x,
                    y + living.getBbHeight() * 0.5F,
                    z,
                    particleCount,
                    living.getBbWidth() * 0.5,
                    living.getBbHeight() * 0.5,
                    living.getBbWidth() * 0.5,
                    0
            );
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingTicks, int level) {
        // 每5个 tick 应用一次效果 // Apply the effect every 5 ticks
        return remainingTicks % 5 == 0;
    }

    @Override
    public boolean isBeneficial() {
        return false; // 确定该效果是有害的 // This effect is harmful
    }
}

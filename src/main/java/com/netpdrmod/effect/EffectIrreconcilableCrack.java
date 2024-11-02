package com.netpdrmod.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class EffectIrreconcilableCrack extends BaseEffect{

    public EffectIrreconcilableCrack(MobEffectCategory type, int color, boolean isInstant) {
        super(type, color, isInstant);
    }

    //buff隔多久生效一次
    @Override
    protected boolean canApplyEffect(int remainingTicks, int level) {
        return remainingTicks % 5 == 0;
    }

    //buff在生物身上的效果
    @Override
    public void applyEffectTick(@NotNull LivingEntity living, int amplified) {
        amplified++;
        Random ran = new Random();
        ran.nextInt(5);
        //如果是玩家的话，就加快其饥饿速度
        if (living instanceof Player)
            ((Player) living).causeFoodExhaustion(2F * amplified);
        //生物每次受到1.0乘buff等级这么多的伤害
        living.hurt(living.damageSources().wither(), 1.0F * amplified);
        //粒子效果
        if (living.getCommandSenderWorld() instanceof ServerLevel world) {
            double x = living.getX();
            double y = living.getY();
            double z = living.getZ();
            int particleCount = 10 * amplified; // 粒子数量根据buff等级来调整
            world.sendParticles(ParticleTypes.SCULK_SOUL, x, y + living.getBbHeight() * 0.5F, z, particleCount, living.getBbWidth() * 0.5, living.getBbHeight() * 0.5, living.getBbWidth() * 0.5, 0);
        }
    }
    @Override
    public boolean isBeneficial() {
        return false;
    }
}


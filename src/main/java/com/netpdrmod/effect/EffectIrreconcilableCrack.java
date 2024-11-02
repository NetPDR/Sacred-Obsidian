package com.netpdrmod.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EffectIrreconcilableCrack extends BaseEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(EffectIrreconcilableCrack.class);

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

        // 设置发光效果 // Set glowing effect
        setGlowing(living, true);

        // 粒子效果 // Particle effect
        if (living.getCommandSenderWorld() instanceof ServerLevel world) {
            double x = living.getX();
            double y = living.getY();
            double z = living.getZ();
            int particleCount = 10 * amplified; // 粒子数量根据Buff等级调整 // Adjusts particle count based on buff level
            world.sendParticles(ParticleTypes.SCULK_SOUL, x, y + living.getBbHeight() * 0.5F, z, particleCount, living.getBbWidth() * 0.5, living.getBbHeight() * 0.5, living.getBbWidth() * 0.5, 0);
        }

        // 检查效果是否即将结束，移除发光效果 // Check if the effect is about to end and remove glowing effect
        var effect = living.getEffect(this);
        if (effect != null) {
            int duration = effect.getDuration();
            if (duration <= 1) {
                setGlowing(living, false);
            }
        }
    }

    // 说实话，是否用事件监听会更好些? // Should event listening be more suitable here?
    // 使用反射设置发光效果 // Use reflection to set the glowing effect
    private void setGlowing(LivingEntity living, boolean glowing) {
        try {
            Method setGlowingMethod = LivingEntity.class.getSuperclass().getDeclaredMethod("setSharedFlag", int.class, boolean.class);
            setGlowingMethod.setAccessible(true);
            setGlowingMethod.invoke(living, 6, glowing); // 6 是发光效果的标识位 // 6 is the flag index for the glowing effect
        } catch (Exception e) {
            LOGGER.error("Failed to set glowing for entity: {}", living, e);
        }
    }

    @Override
    public boolean isBeneficial() {
        return false;
    }
}

package com.netpdr.sacredobsidian.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public abstract class BaseEffect extends MobEffect {

    private final boolean instant;

    protected BaseEffect(MobEffectCategory category, int color, boolean instant) {
        super(category, color);
        this.instant = instant;
    }

    /* ================= Instant 判定 ================= */

    @Override
    public boolean isInstantenous() {
        return false;
    }

    /* ================= 持续效果 Tick 判定 ================= */

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingTicks, int amplifier) {
        if (instant) return false; // Instant 不走 tick
        return canApplyEffect(remainingTicks, amplifier);
    }

    protected boolean canApplyEffect(int remainingTicks, int amplifier) {
        return true;
    }

    /* ================= Tick 生效 ================= */

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        // 子类实现
        return true;
    }
}
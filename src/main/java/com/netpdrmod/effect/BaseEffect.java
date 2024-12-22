package com.netpdrmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * BaseEffect: 一个用于自定义药水效果的基类。//A base class for custom potion effects.
 */
public abstract class BaseEffect extends MobEffect {

    private final boolean isInstant;

    @Nullable
    private SoundEvent customSoundOnApply;
    private boolean hasCustomSound = false;

    public BaseEffect(MobEffectCategory category, int color, boolean isInstant) {
        super(category, color);
        this.isInstant = isInstant;
    }

    public boolean isInstantenous() {
        return this.isInstant;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        this.onEffectActive(entity, amplifier);
        // 如果效果需要继续生效，返回 true；否则返回 false // Return true if the effect should continue; otherwise, return false
        return true;
    }

    /**
     * 定义子类需要实现的效果逻辑。 // Defines the effect logic that subclasses need to implement.
     *
     * @param entity 目标实体 // The target entity
     * @param amplifier 药水等级 // The potion level
     */
    protected abstract void onEffectActive(LivingEntity entity, int amplifier);

    public BaseEffect withCustomSound(SoundEvent sound) {
        this.customSoundOnApply = sound;
        this.hasCustomSound = true;
        return this;
    }

    @Override
    public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        if (this.hasCustomSound && this.customSoundOnApply != null) {
            entity.getCommandSenderWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    this.customSoundOnApply, entity.getSoundSource(), 1.0F, 1.0F);
        }
    }
}
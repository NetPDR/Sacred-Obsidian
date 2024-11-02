package com.netpdrmod.registry;

import com.netpdrmod.effect.EffectIrreconcilableCrack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffect
{
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"netpdrmod");
    public static RegistryObject<MobEffect> IRRECONCILABLE_CRACK = EFFECTS.register("irreconcilable_crack",()->
    {
        //这个效果生效时，就将玩家的速度降低25%
        return new EffectIrreconcilableCrack(MobEffectCategory.HARMFUL, 0x000033, false)
                .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                        "7107DE5E-7CE8-4030-940E-514C1F160890", -0.25F, AttributeModifier.Operation.MULTIPLY_TOTAL);
    });

}

package com.netpdr.sacredobsidian.registry;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.effect.EffectIrreconcilableCrack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册 MobEffect 的安全实现（NeoForge 1.21.1）。
 * 关键点：
 * - DeferredRegister.register(...) 的返回类型是 DeferredHolder，不要把它当 Holder 或实例使用。
 * - register 的 supplier 必须返回具体的 MobEffect 实例（不要返回 Holder）。
 * - 添加 attribute modifier 时直接使用 Attributes.MOVEMENT_SPEED（具体 Attribute）
 *   而不是试图在注册期创建或强转 Holder。
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Sacredobsidian.MODID);

    // DeferredHolder —— 正确匹配 DeferredRegister.register 的返回
    public static final DeferredHolder<MobEffect, MobEffect> IRRECONCILABLE_CRACK =
            EFFECTS.register("irreconcilable_crack", () -> {
                // 返回具体的效果实例（不要返回 Holder）
                MobEffect effect = new EffectIrreconcilableCrack(MobEffectCategory.HARMFUL, 0x000033, false);

                // 使用具体的 Attribute（Attributes.MOVEMENT_SPEED），不要把 Holder 强转为 Attribute
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "irreconcilable_crack");

                // 在 MobEffect 上注册一个 attribute modifier（API 接口接受 Attribute / ResourceLocation / UUID 形式）
                // 这里用 AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL 与你原先一致
                effect.addAttributeModifier(
                        Attributes.MOVEMENT_SPEED, // 直接使用 Attribute 实例
                        modifierId,
                        -0.25D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );

                return effect;
            });
}
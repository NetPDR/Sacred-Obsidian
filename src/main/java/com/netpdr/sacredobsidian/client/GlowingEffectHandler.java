package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(
        modid = Sacredobsidian.MODID,
        bus = EventBusSubscriber.Bus.MOD
)
public class GlowingEffectHandler {

    private static final ResourceKey<MobEffect> IRRECONCILABLE_CRACK =
            ResourceKey.create(
                    Registries.MOB_EFFECT,
                    ResourceLocation.fromNamespaceAndPath(
                            Sacredobsidian.MODID,
                            "irreconcilable_crack"
                    )
            );

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Holder<MobEffect> effectHolder =
                level.registryAccess()
                        .registryOrThrow(Registries.MOB_EFFECT)
                        .getHolder(IRRECONCILABLE_CRACK)
                        .orElse(null);

        if (effectHolder == null) return;

        AABB box = new AABB(
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY
        );

        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            boolean shouldGlow = living.hasEffect(effectHolder);

            if (living.isCurrentlyGlowing() != shouldGlow) {
                living.setGlowingTag(shouldGlow);
            }
        }
    }
}
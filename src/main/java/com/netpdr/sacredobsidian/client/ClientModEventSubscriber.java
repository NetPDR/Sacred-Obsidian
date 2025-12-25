package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "sacredobsidian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEventSubscriber {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

    }

    @Mod.EventBusSubscriber
    public static class GlowingEffectHandler {

        @SubscribeEvent
       public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();

            if (entity.level().isClientSide) return;

            boolean shouldGlow =
                    entity.hasEffect(ModEffects.IRRECONCILABLE_CRACK.get());

            if (entity.isCurrentlyGlowing() != shouldGlow) {
                entity.setGlowingTag(shouldGlow);
            }
        }
   }
}
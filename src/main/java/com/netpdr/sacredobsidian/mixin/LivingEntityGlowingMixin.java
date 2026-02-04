package com.netpdr.sacredobsidian.mixin;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGlowingMixin {

    @Unique
    private static final ResourceKey<MobEffect> IRRECONCILABLE_CRACK =
            ResourceKey.create(
                    Registries.MOB_EFFECT,
                    ResourceLocation.fromNamespaceAndPath(
                            Sacredobsidian.MODID,
                            "irreconcilable_crack"
                    )
            );

    @Inject(
            method = "isCurrentlyGlowing",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sacredobsidian$forceGlowing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity living = (LivingEntity) (Object) this;

        Holder<MobEffect> holder =
                living.getCommandSenderWorld()
                        .registryAccess()
                        .registryOrThrow(Registries.MOB_EFFECT)
                        .getHolder(IRRECONCILABLE_CRACK)
                        .orElse(null);

        if (holder != null && living.hasEffect(holder)) {
            cir.setReturnValue(true);
        }
    }
}
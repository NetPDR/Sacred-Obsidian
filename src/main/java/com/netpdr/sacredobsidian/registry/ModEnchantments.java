package com.netpdr.sacredobsidian.registry;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ModEnchantments {
    public static final String MODID = Sacredobsidian.MODID;

    public static final ResourceKey<Enchantment> OBSIDIAN_POWER =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, "obsidian_power"));

    public static final ResourceKey<Enchantment> OBSIDIAN_REACH =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, "obsidian_reach"));

    private ModEnchantments() { /* util class */ }

    public static Optional<Holder.Reference<Enchantment>> getHolder(Level level, ResourceKey<Enchantment> key) {
        try {
            var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            return registry.getHolder(key);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static int getEnchantmentLevel(ItemStack stack, Level level, ResourceKey<Enchantment> enchantKey) {
        try {
            return getHolder(level, enchantKey)
                    .map(holder -> EnchantmentHelper.getTagEnchantmentLevel(holder, stack))
                    .orElse(0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static int getEnchantmentLevel(ItemStack stack, Level level, String enchantId) {
        return getEnchantmentLevel(stack, level,
                ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, enchantId)));
    }
}
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

/**
 * NeoForge / 1.21.x 版的 ModEnchantments 辅助类（不再注册 Enchantment 实例）。
 * 责任：
 *  - 为项目中使用到的 datapack 附魔提供 ResourceKey 常量（方便在代码里引用）
 *  - 提供运行时从 ItemStack/Level 读取等级与 Holder 的小工具方法
 * 说明：
 *  - 附魔实体由 datapack JSON（data/<modid>/enchantment/*.json）定义并注册到 Registries.ENCHANTMENT。
 *  - 如果你不再需要 DataComponent 注册，可以删除 register() 方法与对 ModComponents.REGISTRAR 的静态导入。
 */
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

    /**
     * 兼容便利重载：用字符串 enchantId（不带 modid 时默认使用本 mod 的 namespace）。
     * 这个方法保留以便在代码其他处用短字符串调用，如果你确实不需要可以删除它。
     */
    @SuppressWarnings("unused") // 有时候被 IDE 报 unused，保留以便外部按 id 调用
    public static int getEnchantmentLevel(ItemStack stack, Level level, String enchantId) {
        return getEnchantmentLevel(stack, level,
                ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, enchantId)));
    }
}
package com.netpdr.sacredobsidian.weapon;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

import static com.netpdr.sacredobsidian.weapon.SacredObsidianItem.*;

public abstract  class BaseItem extends TieredItem {

    public BaseItem(Tier tier, Properties props){
        super(tier, props);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        appendHoverTextWithLevel(stack, tooltip, flag);
    }

    protected void appendHoverTextWithLevel(@NotNull ItemStack stack, @NotNull List<Component> tooltip, @NotNull TooltipFlag ignoredFlag) {
        // 读取附魔等级（与实际伤害计算保持一致） // Read enchantment level (consistent with actual damage calculation)
        int powerLevel = getEnchantmentLevel(stack, null, Sacredobsidian.MODID + ":obsidian_power");

        // 计算数值（与类内逻辑一致） // Calculate numerical values (consistent with intra class logic)
        double damage = OBSIDIAN_DAMAGE + powerLevel * 5.0;
        double markSeconds = 100.0 / 20.0;         // 在代码里对实体加了 100 ticks 的效果 // The effect of adding 100 ticks to entities in the code
        double obsidianSeconds = (double) OBSIDIAN_LIFETIME / 20.0;
        double cooldownSeconds = (double) COOLDOWN_TIME / 20.0;

        // 格式化：例如 5.0 -> "5"；1.5 保留一位小数 -> "1.5" // Format: For example, 5.0->"5"; 1.5 Keep one decimal place ->"1.5"
        DecimalFormat df = new DecimalFormat("0.#");
        String dmgStr = df.format(damage);
        String markStr = df.format(markSeconds);
        String obsStr = df.format(obsidianSeconds);
        String cdStr = df.format(cooldownSeconds);

        // 把数值做成带颜色的 Component（等价于 §6 ... §r） // Make the values into colored components (equivalent to § 6.) … §r）
        Component dmgComp = Component.literal(dmgStr).withStyle(ChatFormatting.GOLD);
        Component markComp = Component.literal(markStr).withStyle(ChatFormatting.GOLD);
        Component obsComp = Component.literal(obsStr).withStyle(ChatFormatting.GOLD);
        Component cdComp = Component.literal(cdStr).withStyle(ChatFormatting.GOLD);

        // 使用 lang 中的占位符（见下方 lang 示例） // Use placeholders in lang (see lang example below)
        Component line = Component.translatable("item.sacred_obsidian.tooltip", dmgComp, markComp, obsComp, cdComp);
        tooltip.add(line);
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        // 采掘速度 // mining speed
        float netheriteEfficiency = 25.0F;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? netheriteEfficiency : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, BlockState state) {
        // 最简单且兼容的判断：是否为可被镐挖掘的方块
        // 可替换为更严格的检测（例如 TierSortingRegistry.isCorrectTierForDrops(getTier(), state)）
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /** 允许在附魔台中附魔 Allow enchanting in the Enchanting Station */
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return false;
    }

    public int getEnchantmentLevel(@NotNull ItemStack stack, @Nullable Level level, @NotNull String enchantId) {
        // 1) 优先路径：如果提供了 Level（registry 可用），使用 holder + EnchantmentHelper.getTagEnchantmentLevel
        if (level != null) {
            try {
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(enchantId));
                var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                Optional<Holder.Reference<Enchantment>> holderOpt = registry.getHolder(key);
                if (holderOpt.isPresent()) {
                    Holder.Reference<Enchantment> holder = holderOpt.get();
                    return EnchantmentHelper.getTagEnchantmentLevel(holder, stack);
                }
            } catch (Exception ignored) {
                // 解析或 registry 不可用 -> 回退到下方遍历方案
            }
        }

        // 2) 回退路径：没有 Level（例如 tooltip 场景）或上面路径失败时，通过 EnchantmentHelper.runIterationOnItem 遍历 stack 的附魔
        //    runIterationOnItem 会为每个附魔提供一个 Holder<Enchantment>（兼容 data-component 模型），因此我们可以从 Holder 中尝试取得 registry key / id。
        final int[] found = new int[]{0}; // 用数组捕获 lambda 内的结果
        final boolean[] matched = new boolean[]{false};

        try {
            EnchantmentHelper.runIterationOnItem(stack, (holder, lvl) -> {
                if (matched[0]) return; // 已找到则跳过
                // Holder 中通常能解出对应的 registry key（ResourceKey<Enchantment>）
                try {
                    // unwrapKey() 是 Holder 的常见方法 —— 如果你的 Holder API 是不同名称（如 key() / unwrapKey()），请对应替换
                    holder.unwrapKey().ifPresent(resourceKey -> {
                        ResourceLocation loc = resourceKey.location(); // ResourceLocation（namespace:path）
                        String idStr = loc.toString();      // "namespace:path"
                        String pathOnly = loc.getPath();   // "path"
                        // 宽松比较：完全匹配 namespace:path，或只匹配 path（以便传 "knockback" 这样的短名）
                        if (idStr.equals(enchantId) || pathOnly.equals(enchantId)) {
                            found[0] = lvl;
                            matched[0] = true;
                        }
                    });
                } catch (Throwable t) {
                    // 某些 Holder 实现或方法名不同会触发异常 —— 忽略单个 entry，继续查找其它 entry
                }
            });
        } catch (Throwable ignored) {
            // 如果 runIterationOnItem 本身因版本差异不可用，这里会抛异常并被捕获。
            // 最终返回 0（说明未找到）
        }

        if (matched[0]) return found[0];

        // 3) 最后兜底：如果上面都找不到，返回 0（附魔不存在 / 未注册 / stack 上没有）
        return 0;
    }
}



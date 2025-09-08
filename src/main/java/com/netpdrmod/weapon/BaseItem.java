package com.netpdrmod.weapon;

import com.netpdrmod.registry.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import static com.netpdrmod.weapon.SacredObsidianItem.*;

public abstract  class BaseItem extends Item{

    public BaseItem(Properties props){
        super(props);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        // 不调用 super.appendHoverText(...)，我们自己生成完整的动态描述行 // Do not call super. appendHoverText (...), we will generate a complete dynamic description line ourselves

        // 读取附魔等级（与实际伤害计算保持一致） // Read enchantment level (consistent with actual damage calculation)
        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        int powerLevel = enchMap.getOrDefault(ModEnchantments.OBSIDIAN_POWER.get(), 0);

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

        // 把数值做成带颜色的 Component（等价于 §6 ... §r） // Make the values into colored components (equivalent to § 6.) .. §r）
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
    public boolean isCorrectToolForDrops(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /** 允许在附魔台中附魔 Allow enchanting in the Enchanting Station */
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    /**
     * @deprecated Mojang 标记为 deprecated，但1.20.1里这是唯一能控制附魔强度的方法。 // Mojang is marked as deprecated, but in 1.20.1, this is the only method that can control the strength of enchantments.
     */
    @SuppressWarnings("deprecation")
    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment ench) {
        return ench == Enchantments.BLOCK_EFFICIENCY
                || ench == Enchantments.BLOCK_FORTUNE
                || ench == Enchantments.SILK_TOUCH
                || ench == Enchantments.KNOCKBACK
                || ench == Enchantments.FIRE_ASPECT
                || ench == ModEnchantments.OBSIDIAN_REACH.get()
                || ench == ModEnchantments.OBSIDIAN_POWER.get();
    }
}



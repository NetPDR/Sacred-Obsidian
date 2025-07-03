package com.netpdrmod.weapon;

import com.netpdrmod.registry.ModEnchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public abstract  class BaseItem extends Item{

    public BaseItem(Properties props){
        super(props);
    }
    protected abstract String getDescriptionKey();

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionKey()));
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

    /** 允许在附魔台中附魔 */
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    /**
     * @deprecated Mojang 标记为 deprecated，但1.20.1里这是唯一能控制附魔强度的方法。
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



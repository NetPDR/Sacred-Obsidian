package com.netpdrmod.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;
import com.netpdrmod.weapon.SacredObsidianItem;
import org.jetbrains.annotations.NotNull;

public class ObsidianReachEnchantment extends Enchantment {

    public ObsidianReachEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot... slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 15;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    /** 只允许作用在 SacredObsidianItem 上 */
    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SacredObsidianItem;
    }

    /** 允许附魔台附魔 */
    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack) {
        return canEnchant(stack);
    }

    /** 允许附魔书合成 */
    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }
}
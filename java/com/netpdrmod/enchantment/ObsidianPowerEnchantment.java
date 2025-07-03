package com.netpdrmod.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import com.netpdrmod.weapon.SacredObsidianItem;
import org.jetbrains.annotations.NotNull;

public class ObsidianPowerEnchantment extends Enchantment {

    public ObsidianPowerEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot... slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    /** 只允许给 SacredObsidianItem 附魔 */
    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SacredObsidianItem;
    }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack) {
        return canEnchant(stack);
    }

    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }
}
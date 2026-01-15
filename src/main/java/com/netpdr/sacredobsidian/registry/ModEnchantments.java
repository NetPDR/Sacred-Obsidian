package com.netpdr.sacredobsidian.registry;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.enchantment.ObsidianPowerEnchantment;
import com.netpdr.sacredobsidian.enchantment.ObsidianReachEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Sacredobsidian.MODID);

    public static final RegistryObject<Enchantment> OBSIDIAN_POWER =
            ENCHANTMENTS.register("obsidian_power",
                    () -> new ObsidianPowerEnchantment(
                            Enchantment.Rarity.RARE,
                            EnchantmentCategory.BREAKABLE,
                            EquipmentSlot.MAINHAND
                    )
            );

    public static final RegistryObject<Enchantment> OBSIDIAN_REACH =
            ENCHANTMENTS.register("obsidian_reach",
                    () -> new ObsidianReachEnchantment(
                            Enchantment.Rarity.UNCOMMON,
                            EnchantmentCategory.BREAKABLE,
                            EquipmentSlot.MAINHAND
                    )
            );

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }
}
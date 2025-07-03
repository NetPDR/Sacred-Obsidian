package com.netpdrmod.registry;

import com.netpdrmod.enchantment.ObsidianPowerEnchantment;
import com.netpdrmod.enchantment.ObsidianReachEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.netpdrmod.Netpdrmod.MODID;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

    public static final RegistryObject<Enchantment> OBSIDIAN_REACH =
            ENCHANTMENTS.register("obsidian_reach",
                    () -> new ObsidianReachEnchantment(
                            Enchantment.Rarity.UNCOMMON,
                            EnchantmentCategory.BREAKABLE,
                            EquipmentSlot.MAINHAND
                    )
            );

    public static final RegistryObject<Enchantment> OBSIDIAN_POWER =
            ENCHANTMENTS.register("obsidian_power",
                    () -> new ObsidianPowerEnchantment(
                            Enchantment.Rarity.RARE,
                            EnchantmentCategory.BREAKABLE,
                            EquipmentSlot.MAINHAND
                    )
            );

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }
}
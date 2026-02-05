package com.netpdr.sacredobsidian.datagen;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.registry.ModEnchantments;
import com.netpdr.sacredobsidian.registry.ModItems;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantmentBootstrap {

    public static final RegistrySetBuilder BUILDER =
            new RegistrySetBuilder()
                    .add(
                            net.minecraft.core.registries.Registries.ENCHANTMENT,
                            ModEnchantmentBootstrap::bootstrap
                    );

    private static void bootstrap(BootstrapContext<Enchantment> ctx) {

        HolderSet<Item> sacredObsidian =
                HolderSet.direct(ModItems.SACRED_OBSIDIAN.get().builtInRegistryHolder());

        /* ================= Obsidian Power ================= */

        Enchantment.EnchantmentDefinition obsidianPowerDef =
                Enchantment.definition(
                        sacredObsidian,                       // supported items
                        5,                                    // weight
                        5,                                    // max level
                        Enchantment.dynamicCost(5, 10),       // min cost
                        Enchantment.dynamicCost(25, 10),      // max cost
                        1,                                    // anvil cost
                        EquipmentSlotGroup.MAINHAND
                );

        ctx.register(
                ModEnchantments.OBSIDIAN_POWER,
                new Enchantment(
                        Component.translatable(
                                "enchantment." + Sacredobsidian.MODID + ".obsidian_power"
                        ),
                        obsidianPowerDef,
                        HolderSet.direct(),
                        net.minecraft.core.component.DataComponentMap.EMPTY
                )
        );

        /* ================= Obsidian Reach ================= */

        Enchantment.EnchantmentDefinition obsidianReachDef =
                Enchantment.definition(
                        sacredObsidian,
                        10,
                        3,
                        Enchantment.dynamicCost(10, 15),
                        Enchantment.dynamicCost(60, 15),
                        1,
                        EquipmentSlotGroup.MAINHAND
                );

        ctx.register(
                ModEnchantments.OBSIDIAN_REACH,
                new Enchantment(
                        Component.translatable(
                                "enchantment." + Sacredobsidian.MODID + ".obsidian_reach"
                        ),
                        obsidianReachDef,
                        HolderSet.direct(),
                        net.minecraft.core.component.DataComponentMap.EMPTY
                )
        );
    }

    private ModEnchantmentBootstrap() {}
}
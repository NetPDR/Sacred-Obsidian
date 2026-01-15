package com.netpdr.sacredobsidian.registry;


import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.item.AncientTomeItem;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Sacredobsidian.MODID);

    public static final RegistryObject<Item> SACRED_OBSIDIAN = ITEMS.register("sacred_obsidian",
            () -> new SacredObsidianItem(new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ANCIENT_TOME_OBSIDIAN_POWER =
            ITEMS.register("ancient_tome_obsidian_power",
                    () -> new AncientTomeItem(ModEnchantments.OBSIDIAN_POWER, new Item.Properties().fireResistant().stacksTo(1)));

    public static final RegistryObject<Item> ANCIENT_TOME_OBSIDIAN_REACH =
            ITEMS.register("ancient_tome_obsidian_reach",
                    () -> new AncientTomeItem(ModEnchantments.OBSIDIAN_REACH, new Item.Properties().fireResistant().stacksTo(1)));

}

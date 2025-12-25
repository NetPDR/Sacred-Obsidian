package com.netpdr.sacredobsidian.registry;


import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.weapon.SacredObsidianItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Sacredobsidian.MODID);

    public static final RegistryObject<Item> SACRED_OBSIDIAN = ITEMS.register("sacred_obsidian",
            () -> new SacredObsidianItem(new Item.Properties().fireResistant()));
}

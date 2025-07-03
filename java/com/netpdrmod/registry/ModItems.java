package com.netpdrmod.registry;


import com.netpdrmod.Netpdrmod;
import com.netpdrmod.weapon.SacredObsidianItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Netpdrmod.MODID);

    public static final RegistryObject<Item> SACRED_OBSIDIAN = ITEMS.register("sacred_obsidian",
            () -> new SacredObsidianItem(new Item.Properties().fireResistant()));
}

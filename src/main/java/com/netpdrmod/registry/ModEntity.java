package com.netpdrmod.registry;

import com.netpdrmod.Netpdrmod;
import com.netpdrmod.entity.SacredObsidianEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntity {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "netpdrmod");

    public static final RegistryObject<EntityType<SacredObsidianEntity>> SACRED_OBSIDIAN_ITEM_ENTITY = ENTITY_TYPES.register("sacred_obsidian_item_entity",
            () -> EntityType.Builder.<SacredObsidianEntity>of(SacredObsidianEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f) // 设置与普通掉落物相同的碰撞盒大小 // Set the collision box size to be the same as a regular item drop
                    .build(Netpdrmod.MODID + "sacred_obsidian_item_entity")
    );
}
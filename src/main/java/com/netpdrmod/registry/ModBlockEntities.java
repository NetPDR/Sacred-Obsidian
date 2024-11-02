package com.netpdrmod.registry;

import com.netpdrmod.blockentity.SacredObsidianBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    // Create a DeferredRegister to registry BlockEntityType
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "netpdr");

    // Registry SacredObsidianBlockEntity
    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<SacredObsidianBlockEntity>> SACRED_OBSIDIAN_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("sacred_obsidian_block_entity",
                    () -> BlockEntityType.Builder.of(SacredObsidianBlockEntity::new, Blocks.OBSIDIAN).build(null));
}

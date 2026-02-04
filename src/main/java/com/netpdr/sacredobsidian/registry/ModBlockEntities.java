package com.netpdr.sacredobsidian.registry;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.blockentity.SacredObsidianBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    // Create a DeferredRegister to registry BlockEntityType
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Sacredobsidian.MODID);

    // Registry SacredObsidianBlockEntity
    @SuppressWarnings("ConstantConditions")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SacredObsidianBlockEntity>> SACRED_OBSIDIAN_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("sacred_obsidian_block_entity",
                    () -> BlockEntityType.Builder.of(SacredObsidianBlockEntity::new, Blocks.OBSIDIAN).build(null));
}

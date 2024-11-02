package com.netpdrmod.blockentity;


import com.netpdrmod.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class SacredObsidianBlockEntity extends BlockEntity {

    public SacredObsidianBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SACRED_OBSIDIAN_BLOCK_ENTITY.get(), pos, state);
    }
}
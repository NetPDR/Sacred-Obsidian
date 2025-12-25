package com.netpdr.sacredobsidian.blockentity;


import com.netpdr.sacredobsidian.registry.ModBlockEntities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class SacredObsidianBlockEntity extends BlockEntity {
    public SacredObsidianBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SACRED_OBSIDIAN_BLOCK_ENTITY.get(), pos, state);
    }

    public void onPlacedBy(Player player) {
        // TODO: initialize owner/props
    }
}
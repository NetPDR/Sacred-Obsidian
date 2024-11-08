package com.netpdrmod.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public abstract  class BaseItem extends Item{
    public BaseItem(Properties props){
        super(props);
    }
    protected abstract String getDescriptionKey();

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionKey()));
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        // 采掘速度 // mining speed
        float netheriteEfficiency = 25.0F;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? netheriteEfficiency : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }
}



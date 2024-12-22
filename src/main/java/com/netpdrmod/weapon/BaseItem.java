package com.netpdrmod.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public abstract class BaseItem extends Item {
    public BaseItem(Properties props) {
        super(props);
    }

    // 抽象方法，子类需提供描述信息的 key // Abstract method, subclasses need to provide the description key
    protected abstract String getDescriptionKey();

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // 添加描述文本 // Add description text
        tooltip.add(Component.translatable(getDescriptionKey()));
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        // 根据方块标签确定采掘速度 // Determine the digging speed based on block tags
        float baseEfficiency = 25.0F; // 默认效率 // Default efficiency
        // 使用更高的效率值来破坏可以用镐采集的方块 // Use higher efficiency to break blocks that can be mined with a pickaxe
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            baseEfficiency = 50.0F; // 对应镐的效率值 // Efficiency value for pickaxe
        }
        return baseEfficiency; // 返回当前效率 // Return the current efficiency
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        // 允许执行默认的采掘动作 // Allow performing the default mining action
        return ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(toolAction);
    }

}

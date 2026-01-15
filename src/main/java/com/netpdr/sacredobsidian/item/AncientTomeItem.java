package com.netpdr.sacredobsidian.item;

import com.netpdr.sacredobsidian.common.AncientTomeCommon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Ancient Tome item bound to a specific enchantment (RegistryObject<Enchantment>).
 * Tooltip is translatable and shows fine-grained, colored components.
 */
public class AncientTomeItem extends Item {
    private final RegistryObject<Enchantment> targetEnchantment;

    public AncientTomeItem(RegistryObject<Enchantment> targetEnchantment, Properties props) {
        super(props);
        this.targetEnchantment = targetEnchantment;
    }

    /**
     * 返回目标附魔（注册完成后可用） Return to target enchantment (available after registration)
     */
    public Enchantment getTargetEnchantment() {
        return targetEnchantment == null ? null : targetEnchantment.get();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);

        Enchantment target = getTargetEnchantment();
        if (target == null) {
            tooltip.add(Component.translatable("item.sacredobsidian.ancient_tome.unconfigured")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // (1) 显示目标附魔名（附魔自己的 fullname Component，带金色） // (1) Display the target enchantment name (enchant your own fullname component, in gold)
        Component enchName = target.getFullname(1).copy().withStyle(ChatFormatting.GOLD);
        tooltip.add(Component.translatable("item.sacredobsidian.ancient_tome.target", enchName));

        // (2) 显示 +1 与最大值（细粒度着色） // (2) Show 1 and the maximum value (fine-grained coloring)
        Component plusOne = Component.literal("+1").withStyle(ChatFormatting.GREEN);
        Component maxLvl = Component.literal(String.valueOf(AncientTomeCommon.MAX_OVERLEVEL)).withStyle(ChatFormatting.YELLOW);
        tooltip.add(Component.translatable("item.sacredobsidian.ancient_tome.action", plusOne, maxLvl));

        // (3) 显示经验消耗（灰色） // (3) Show experience consumption (gray)
        tooltip.add(Component.translatable("item.sacredobsidian.ancient_tome.xp_cost", AncientTomeCommon.XP_COST)
                .withStyle(ChatFormatting.GRAY));

        // (4) 如果 MAX_OVERLEVEL 超过 vanilla 的 max level，显示警告（暗青斜体） // (4) If MAX_OVERLEVEL exceeds the vanilla max level, display a warning (dark cyan italic)
        int vanillaMax = target.getMaxLevel();
        if (vanillaMax < AncientTomeCommon.MAX_OVERLEVEL) {
            Component vanillaComp = Component.literal(String.valueOf(vanillaMax)).withStyle(ChatFormatting.AQUA);
            Component overComp = Component.literal(String.valueOf(AncientTomeCommon.MAX_OVERLEVEL)).withStyle(ChatFormatting.DARK_AQUA);
            tooltip.add(Component.translatable("item.sacredobsidian.ancient_tome.over_vanilla_warning", vanillaComp, overComp)
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }

        // (5) 在客户端展示动态提示：若应用到玩家当前主手物，将变为 N 级（颜色依据是否超过 vanilla） // (5) Display dynamic prompts on the client: If applied to the player's current main-hand item, it will become N level (color depends on whether it exceeds vanilla)
        //    使用 DistExecutor 以避免在服务器端类加载 client-only 类（Minecraft） // Use DistExecutor to avoid loading client-only classes on the server side (Minecraft)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack held = mc.player.getMainHandItem();
            if (held.isEmpty()) return;

            int currentLevel = EnchantmentHelper.getTagEnchantmentLevel(target, held);
            int wouldLevel = Math.min(currentLevel + 1, AncientTomeCommon.MAX_OVERLEVEL);

            // 数字着色：若将要超过 vanilla max 则用醒目颜色（红色），否则绿色 // Number coloring: Use a striking color (red) if it exceeds the vanilla max, otherwise green
            ChatFormatting numberColor = wouldLevel > vanillaMax ? ChatFormatting.DARK_RED : ChatFormatting.GREEN;
            Component willBeNumber = Component.literal(String.valueOf(wouldLevel)).withStyle(numberColor, ChatFormatting.BOLD);

            // 我们使用 translatable key 并把数字作为参数插入（参数保持为 Component，以保留样式） // We use a translatable key and insert numbers as parameters (the parameters remain as Components to preserve styling)
            Component willBeLine = Component.translatable("item.sacredobsidian.ancient_tome.will_be", willBeNumber);
            // 在客户端线程添加到 tooltip // Add to the tooltip on the client thread
            tooltip.add(willBeLine);
        });

        // (6) 最后一行：使用提示（灰色斜体） // (6) Last line: use tips (gray italic)
        tooltip.add(Component.translatable("item.sacredobsidian.ancient_tome.usage_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
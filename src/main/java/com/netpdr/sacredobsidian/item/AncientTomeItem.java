package com.netpdr.sacredobsidian.item;

import com.netpdr.sacredobsidian.common.AncientTomeCommon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Ancient Tome item bound to a specific enchantment.
 * NeoForge 1.21.x / datapack-enchantment safe implementation.
 */
public class AncientTomeItem extends Item {

    private final ResourceKey<Enchantment> targetEnchantment;

    public AncientTomeItem(
            ResourceKey<Enchantment> enchantmentKey,
            Properties properties
    ) {
        super(properties);
        this.targetEnchantment = enchantmentKey;
    }

    public ResourceKey<Enchantment> getTargetEnchantmentKey() {
        return targetEnchantment;
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull TooltipContext context,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (targetEnchantment == null) {
            tooltip.add(Component.translatable(
                    "item.sacredobsidian.ancient_tome.unconfigured"
            ).withStyle(ChatFormatting.RED));
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var registry = mc.level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);

        var optHolder = registry.getHolder(targetEnchantment);
        if (optHolder.isEmpty()) return;

        Holder<Enchantment> holder = optHolder.get();
        Enchantment enchantment = holder.value();

        /* (1) 目标附魔名 */
        Component enchName =
                Enchantment.getFullname(holder, 1)
                        .copy()
                        .withStyle(ChatFormatting.GOLD);

        tooltip.add(Component.translatable(
                "item.sacredobsidian.ancient_tome.target",
                enchName
        ));

        /* (2) +1 → 最大等级 */
        tooltip.add(Component.translatable(
                "item.sacredobsidian.ancient_tome.action",
                Component.literal("+1").withStyle(ChatFormatting.GREEN),
                Component.literal(String.valueOf(AncientTomeCommon.MAX_OVERLEVEL))
                        .withStyle(ChatFormatting.YELLOW)
        ));

        /* (3) XP 消耗 */
        tooltip.add(Component.translatable(
                "item.sacredobsidian.ancient_tome.xp_cost",
                AncientTomeCommon.XP_COST
        ).withStyle(ChatFormatting.GRAY));

        /* (4) 超过原版上限警告 */
        int vanillaMax = enchantment.getMaxLevel();
        if (vanillaMax < AncientTomeCommon.MAX_OVERLEVEL) {
            tooltip.add(Component.translatable(
                    "item.sacredobsidian.ancient_tome.over_vanilla_warning",
                    Component.literal(String.valueOf(vanillaMax))
                            .withStyle(ChatFormatting.AQUA),
                    Component.literal(String.valueOf(AncientTomeCommon.MAX_OVERLEVEL))
                            .withStyle(ChatFormatting.DARK_AQUA)
            ).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }

        /* (5) 客户端动态等级预览 */
        if (mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            if (!held.isEmpty()) {

                int currentLevel =
                        EnchantmentHelper.getTagEnchantmentLevel(holder, held);

                int wouldLevel = Math.min(
                        currentLevel + 1,
                        AncientTomeCommon.MAX_OVERLEVEL
                );

                ChatFormatting color =
                        wouldLevel > vanillaMax
                                ? ChatFormatting.DARK_RED
                                : ChatFormatting.GREEN;

                tooltip.add(Component.translatable(
                        "item.sacredobsidian.ancient_tome.will_be",
                        Component.literal(String.valueOf(wouldLevel))
                                .withStyle(color, ChatFormatting.BOLD)
                ));
            }
        }

        /* (6) 使用提示 */
        tooltip.add(Component.translatable(
                "item.sacredobsidian.ancient_tome.usage_hint"
        ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}

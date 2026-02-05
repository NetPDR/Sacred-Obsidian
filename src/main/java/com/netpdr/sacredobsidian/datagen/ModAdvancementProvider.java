package com.netpdr.sacredobsidian.datagen;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModAdvancementProvider implements AdvancementSubProvider {

    @Override
    public void generate(
            HolderLookup.@NotNull Provider provider,
            @NotNull Consumer<AdvancementHolder> output
    ) {

        /* ================= Root ================= */

        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        ModItems.SACRED_OBSIDIAN.get(),
                        Component.translatable("advancement.sacredobsidian.root.title"),
                        Component.translatable("advancement.sacredobsidian.root.description"),
                        ResourceLocation.withDefaultNamespace("textures/block/obsidian.png"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false
                )
                .addCriterion(
                        "impossible",
                        new Criterion<>(
                                CriteriaTriggers.IMPOSSIBLE,
                                new ImpossibleTrigger.TriggerInstance()
                        )
                )
                .save(
                        output,
                        Sacredobsidian.MODID + ":sacredobsidian_root"
                );

        /* ================= Get Sacred Obsidian ================= */

        AdvancementHolder getSacredObsidian = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        ModItems.SACRED_OBSIDIAN.get(),
                        Component.translatable("advancement.sacredobsidian.get_sacred_obsidian.title"),
                        Component.translatable("advancement.sacredobsidian.get_sacred_obsidian.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )

                .addCriterion(
                        "got_obsidian",
                        InventoryChangeTrigger.TriggerInstance.hasItems(
                                ItemPredicate.Builder.item()
                                        .of(ModItems.SACRED_OBSIDIAN.get())
                                        .withCount(MinMaxBounds.Ints.atLeast(1))
                                        .build()
                        )
                )

                .requirements(AdvancementRequirements.Strategy.AND)
                .save(
                        output,
                        Sacredobsidian.MODID + ":get_sacred_obsidian"
                );

        /* ================= Switch Form ================= */

        Advancement.Builder.advancement()
                .parent(getSacredObsidian)
                .display(
                        ModItems.SACRED_OBSIDIAN.get(),
                        Component.translatable("advancement.sacredobsidian.switch_form.title"),
                        Component.translatable("advancement.sacredobsidian.switch_form.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion(
                        "switch_form",
                        new Criterion<>(
                                CriteriaTriggers.IMPOSSIBLE,
                                new ImpossibleTrigger.TriggerInstance()
                        )
                )
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(
                        output,
                        Sacredobsidian.MODID + ":switch_form"
                );

        /* ================= Use Ancient Tome ================= */

        Advancement.Builder.advancement()
                .parent(getSacredObsidian)
                .display(
                        ModItems.ANCIENT_TOME_OBSIDIAN_POWER.get(),
                        Component.translatable("advancement.sacredobsidian.use_ancient_tome.title"),
                        Component.translatable("advancement.sacredobsidian.use_ancient_tome.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion(
                        "used_ancient_tome",
                        new Criterion<>(
                                CriteriaTriggers.IMPOSSIBLE,
                                new ImpossibleTrigger.TriggerInstance()
                        )
                )
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(
                        output,
                        Sacredobsidian.MODID + ":use_ancient_tome"
                );
    }
}
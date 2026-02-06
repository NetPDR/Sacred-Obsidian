package com.netpdr.sacredobsidian.datagen;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModEnchantmentTagProvider extends EnchantmentTagsProvider {

    public ModEnchantmentTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookup
    ) {
        super(output, lookup);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {

        tag(EnchantmentTags.IN_ENCHANTING_TABLE)
                .addOptional(ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "obsidian_power"))
                .addOptional(ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "obsidian_reach"))
                .addOptional(ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "obsidian_enhanced"));

        tag(EnchantmentTags.NON_TREASURE)
                .addOptional(ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "obsidian_power"))
                .addOptional(ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "obsidian_reach"))
                .addOptional(ResourceLocation.fromNamespaceAndPath(Sacredobsidian.MODID, "obsidian_enhanced"));
    }    @Override
    public @NotNull String getName() {
        return "Sacred Obsidian Enchantment Tags";
    }
}
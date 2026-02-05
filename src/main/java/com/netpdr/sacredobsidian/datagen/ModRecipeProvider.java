package com.netpdr.sacredobsidian.datagen;

import com.netpdr.sacredobsidian.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.SACRED_OBSIDIAN.get()
                )
                .pattern("#$#")
                .pattern("$%$")
                .pattern("#$#")
                .define('#', Items.OBSIDIAN)
                .define('$', Items.NETHERITE_INGOT)
                .define('%', Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(recipeOutput);

        ItemStack strongStrengthPotion = new ItemStack(Items.POTION);
        strongStrengthPotion.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.STRONG_STRENGTH)
        );

        Ingredient strengthPotionIngredient =
                Ingredient.of(strongStrengthPotion);

        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.ANCIENT_TOME_OBSIDIAN_POWER.get()
                )
                .pattern("#$#")
                .pattern("^%^")
                .pattern("#^#")
                .define('#', Items.LAPIS_LAZULI)
                .define('$', Items.BOOK)
                .define('%', Items.NETHER_STAR)
                .define('^', strengthPotionIngredient)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.ANCIENT_TOME_OBSIDIAN_REACH.get()
                )
                .pattern("#$#")
                .pattern("^%^")
                .pattern("#^#")
                .define('#', Items.LAPIS_LAZULI)
                .define('$', Items.BOOK)
                .define('%', Items.NETHER_STAR)
                .define('^', Items.OBSIDIAN)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(recipeOutput);
    }
}
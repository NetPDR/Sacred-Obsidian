package com.netpdr.sacredobsidian.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Datagen provider that emits:
 *  - data/<modid>/enchantments/obsidian_enhanced.json
 * 说明：
 *  - 这里只生成 Efficiency 效果（挖掘速度）。
 *  - 不再生成 Fortune / apply_bonus 相关内容。
 */
public class ModEnchantmentEffectProvider implements DataProvider {

    private final PackOutput output;

    public ModEnchantmentEffectProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        return generateObsidianEnhanced(cache);
    }

    // -------------- Enchantment JSON ----------------
    private CompletableFuture<?> generateObsidianEnhanced(CachedOutput cache) {
        JsonObject root = new JsonObject();

        // description (translation key)
        JsonObject description = new JsonObject();
        description.addProperty("translate", "enchantment." + Sacredobsidian.MODID + ".obsidian_enhanced");
        root.add("description", description);

        // supported items
        root.addProperty("supported_items", Sacredobsidian.MODID + ":sacred_obsidian");

        // primary_items (optional)
        JsonArray primary = new JsonArray();
        primary.add(Sacredobsidian.MODID + ":sacred_obsidian");
        root.add("primary_items", primary);

        // weight / max_level
        root.addProperty("weight", 10);  // 与原版 Efficiency 相同
        root.addProperty("max_level", 5);

        // min_cost / max_cost
        JsonObject minCost = new JsonObject();
        minCost.addProperty("base", 1);
        minCost.addProperty("per_level_above_first", 10);
        root.add("min_cost", minCost);

        JsonObject maxCost = new JsonObject();
        maxCost.addProperty("base", 51);
        maxCost.addProperty("per_level_above_first", 10);
        root.add("max_cost", maxCost);

        // anvil cost + slots
        root.addProperty("anvil_cost", 1);
        JsonArray slots = new JsonArray();
        slots.add("mainhand");
        root.add("slots", slots);

        /*
         * effects: 只增加挖掘速度
         */
        JsonObject effects = getEffects();

        root.add("effects", effects);

        // 写入数据包
        Path outPath = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(Sacredobsidian.MODID + "/enchantment/obsidian_enhanced.json");

        return DataProvider.saveStable(cache, root, outPath);
    }

    private static @NotNull JsonObject getEffects() {
        JsonObject effects = new JsonObject();
        JsonArray attributesArr = new JsonArray();
        JsonObject attributesEntry = new JsonObject();

        // 挖掘速度线性增长
        JsonObject amount = new JsonObject();
        amount.addProperty("type", "minecraft:levels_squared");
        amount.addProperty("added", 1.0);
        attributesEntry.add("amount", amount);

        attributesEntry.addProperty("attribute", "minecraft:player.mining_efficiency");
        attributesEntry.addProperty("id", Sacredobsidian.MODID + ":enchantment.obsidian_enhanced");
        attributesEntry.addProperty("operation", "add_value");

        attributesArr.add(attributesEntry);
        effects.add("minecraft:attributes", attributesArr);
        return effects;
    }

    @Override
    public @NotNull String getName() {
        return "Sacred Obsidian Enchantment Datagen (Enhanced Only)";
    }
}
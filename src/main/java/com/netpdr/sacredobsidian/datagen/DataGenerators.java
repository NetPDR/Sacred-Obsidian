package com.netpdr.sacredobsidian.datagen;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.core.HolderLookup;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        // 从事件中取得 lookup provider（CompletableFuture<HolderLookup.Provider>）
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        if (event.includeServer()) {

            /* Recipes */
            generator.addProvider(
                    true,
                    new ModRecipeProvider(output, lookup)
            );

            /* Advancements */
            generator.addProvider(
                    true,
                    new net.minecraft.data.advancements.AdvancementProvider(
                            output,
                            lookup,
                            java.util.List.of(new ModAdvancementProvider())
                    )
            );

            /* Enchantments */
            generator.addProvider(
                    true,
                    new DatapackBuiltinEntriesProvider(
                            output,
                            lookup,
                            ModEnchantmentBootstrap.BUILDER,
                            Set.of(Sacredobsidian.MODID)
                    )
            );

            /* Enchantment tags */
            generator.addProvider(
                    true,
                    new ModEnchantmentTagProvider(output, lookup)
            );

            /* Enchantment effects */
            generator.addProvider(
                    true,
                    new ModEnchantmentEffectProvider(output)
            );
        }
    }
}
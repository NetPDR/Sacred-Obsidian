package com.netpdr.sacredobsidian.registry;

import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Sacredobsidian.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NETPDR_CREATIVE_MODE_TAB =
            CREATIVE_MODE_TABS.register("example_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.netpdr_creative_mode_tab"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> ModItems.SACRED_OBSIDIAN.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.SACRED_OBSIDIAN.get());
                                output.accept(ModItems.ANCIENT_TOME_OBSIDIAN_POWER.get());
                                output.accept(ModItems.ANCIENT_TOME_OBSIDIAN_REACH.get());
                            })
                            .build()
            );
}
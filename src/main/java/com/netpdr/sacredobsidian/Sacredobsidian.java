package com.netpdr.sacredobsidian;

import com.mojang.logging.LogUtils;
import com.netpdr.sacredobsidian.client.entity.ClientSpawnObsidianEffectPacket;
import com.netpdr.sacredobsidian.data.SacredObsidianDataComponents;
import com.netpdr.sacredobsidian.network.ReverseModePayload;
import com.netpdr.sacredobsidian.network.ToggleDamagePayload;
import com.netpdr.sacredobsidian.network.VanillaInteractPayload;
import com.netpdr.sacredobsidian.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

import static com.netpdr.sacredobsidian.weapon.SacredObsidianItem.tickAllDimensions;

@Mod(Sacredobsidian.MODID)
public class Sacredobsidian {

    public static final String MODID = "sacredobsidian";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Sacredobsidian(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModTabs.CREATIVE_MODE_TABS.register(modEventBus);

        SacredObsidianDataComponents.DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(Sacredobsidian::registerPayloads);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(MODID)
                .playToServer(
                        ReverseModePayload.TYPE,
                        ReverseModePayload.CODEC,
                        (payload, context) ->
                                context.enqueueWork(() -> {
                                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                                        ReverseModePayload.handle(payload, sp);
                                    }
                                })
                )
                .playToServer(
                        ToggleDamagePayload.TYPE,
                        ToggleDamagePayload.CODEC,
                        (payload, context) ->
                                context.enqueueWork(() -> {
                                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                                        ToggleDamagePayload.handle(payload, sp);
                                    }
                                })
                )
                .playToServer(
                        VanillaInteractPayload.TYPE,
                        VanillaInteractPayload.CODEC,
                        (payload, context) ->
                                context.enqueueWork(() -> {
                                    if (context.player() instanceof ServerPlayer sp) {
                                        VanillaInteractPayload.handle(payload, sp);
                                    }
                                })
                )
                .playToClient(
                        ClientSpawnObsidianEffectPacket.TYPE,
                        ClientSpawnObsidianEffectPacket.CODEC,
                        ClientSpawnObsidianEffectPacket::handle
                );

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            tickAllDimensions(serverLevel);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
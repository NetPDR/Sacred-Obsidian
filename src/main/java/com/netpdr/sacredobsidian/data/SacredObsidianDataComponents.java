package com.netpdr.sacredobsidian.data;

import com.mojang.serialization.Codec;
import com.netpdr.sacredobsidian.Sacredobsidian;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sacred Obsidian Item DataComponents
 * 设计原则：
 * - ItemStack 状态唯一来源（无 NBT）
 * - 所有字段 persistent（支持存档）
 * - Vec3 原子化（不拆 XYZ）
 */
public class SacredObsidianDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Sacredobsidian.MODID);

    /* =========================
     *   形态 & 行为状态
     * ========================= */

    /** 是否为第二形态 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SECOND_FORM =
            DATA_COMPONENTS.register("second_form",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build());

    /** 第二形态是否正在延伸 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_EXTENDING =
            DATA_COMPONENTS.register("is_extending",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build());

    /** 反向延伸（由客户端按键控制） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> REVERSE_MODE =
            DATA_COMPONENTS.register("reverse_mode",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build());

    /** 是否允许造成伤害（快捷键控制，默认 true） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DAMAGE_ENABLED =
            DATA_COMPONENTS.register("damage_enabled",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build());

    /* =========================
     *   延伸进度相关
     * ========================= */

    /** 已延伸方块数量 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EXTEND_BLOCKS =
            DATA_COMPONENTS.register("extend_blocks",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .build());

    /** 最大可延伸方块数量 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAX_BLOCKS =
            DATA_COMPONENTS.register("max_blocks",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .build());

    /* =========================
     *   空间状态（Vec3 原子态）
     * ========================= */

    /** 延伸游标（平滑跟随视线） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Vec3>> CURSOR =
            DATA_COMPONENTS.register("cursor",
                    () -> DataComponentType.<Vec3>builder()
                            .persistent(Vec3.CODEC)
                            .build());

    /** 上一次成功放置的方块中心 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Vec3>> LAST_POS =
            DATA_COMPONENTS.register("last_pos",
                    () -> DataComponentType.<Vec3>builder()
                            .persistent(Vec3.CODEC)
                            .build());

    /* =========================
     *   冷却系统
     * ========================= */

    /**
     * 冷却结束 tick（world.getGameTime）
     * 设计为「结束时间」而不是「上次使用时间」：
     * - 避免回滚问题
     * - 判断逻辑更直观
     * - 不需要额外 CooldownTicks
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_USE_TICK =
            DATA_COMPONENTS.register("last_use_tick",
                    () -> DataComponentType.<Long>builder()
                            .persistent(Codec.LONG)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COOLDOWN_TICKS =
            DATA_COMPONENTS.register("cooldown_ticks",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .build());

    // dirForUpdate: reserved for future directional effects
    @SuppressWarnings("unused")
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Vec3>> EXTEND_DIR =
            DATA_COMPONENTS.register("extend_dir",
                    () -> DataComponentType.<Vec3>builder()
                            .persistent(Vec3.CODEC)
                            .build());
}
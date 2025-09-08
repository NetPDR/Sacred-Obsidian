package com.netpdrmod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SacredObsidianData extends SavedData {
    private static final String DATA_NAME = "sacred_obsidian_data";

    // 方块位置 -> 剩余 tick（内部存储） // Block position -> remaining ticks (internal storage)
    private final Map<BlockPos, Integer> obsidianData = new HashMap<>();

    // 方块位置 -> 玩家 UUID（拥有者） // Square position -> Player UUID (owner)
    private final Map<BlockPos, UUID> obsidianOwners = new HashMap<>();

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound) {
        // 保存寿命数据 // Preserve lifespan data
        ListTag obsidianList = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : obsidianData.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getKey().asLong());
            entryTag.putInt("ticksLeft", entry.getValue());
            obsidianList.add(entryTag);
        }
        compound.put("obsidianData", obsidianList);

        // 保存归属数据 // Preserve lifespan data
        ListTag ownerList = new ListTag();
        for (Map.Entry<BlockPos, UUID> entry : obsidianOwners.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getKey().asLong());
            entryTag.putUUID("owner", entry.getValue());
            ownerList.add(entryTag);
        }
        compound.put("obsidianOwners", ownerList);

        return compound;
    }

    public static SacredObsidianData load(CompoundTag compound) {
        SacredObsidianData data = new SacredObsidianData();

        // 加载寿命数据 // Load lifespan data
        if (compound.contains("obsidianData", Tag.TAG_LIST)) {
            ListTag obsidianList = compound.getList("obsidianData", Tag.TAG_COMPOUND);
            for (Tag t : obsidianList) {
                if (!(t instanceof CompoundTag entryTag)) continue;
                BlockPos pos = BlockPos.of(entryTag.getLong("pos"));
                int ticksLeft = entryTag.getInt("ticksLeft");
                data.obsidianData.put(pos, ticksLeft);
            }
        }

        // 加载归属数据（UUID 存储为 int array 在 NBT 中，但 getUUID/getLong 等方法会处理） // Load attribution data (UUID is stored as an int array in NBT, but methods such as getUUID/getLong will handle it)
        if (compound.contains("obsidianOwners", Tag.TAG_LIST)) {
            ListTag ownerList = compound.getList("obsidianOwners", Tag.TAG_COMPOUND);
            for (Tag t : ownerList) {
                if (!(t instanceof CompoundTag entryTag)) continue;
                BlockPos pos = BlockPos.of(entryTag.getLong("pos"));
                // 检查并读取 UUID（NBT 的 UUID 是 int array） // Check and read UUID (NBT UUID is int array)
                if (entryTag.contains("owner", Tag.TAG_INT_ARRAY)) {
                    UUID uuid = entryTag.getUUID("owner");
                    data.obsidianOwners.put(pos, uuid);
                }
            }
        }

        return data;
    }

    public static SacredObsidianData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SacredObsidianData::load, SacredObsidianData::new, DATA_NAME);
    }

    // --- 寿命数据访问/修改（请优先使用下面封装方法） Lifetime data access/modification (please prioritize using the following encapsulation method)---
    /**
     * 返回内部 map 的只读视图。若需要修改，请使用 putObsidian/removeObsidian // Return a read-only view of the internal map. If modifications are required, please use putObsidian/removeObsidian
     */
    public Map<BlockPos, Integer> getObsidianData() {
        return Collections.unmodifiableMap(obsidianData);
    }

    /**
     * 将整个 map 设为给定值（覆盖），并标记为脏。谨慎使用。 // Set the entire map to a given value (overwrite) and mark it as dirty. Use with caution.
     */
    @SuppressWarnings("unused")
    @Deprecated
    public void setObsidianData(Map<BlockPos, Integer> data) {
        this.obsidianData.clear();
        this.obsidianData.putAll(data);
        setDirty();
    }

    /**
     * 添加或更新一个 obsidian 条目，并标记数据为脏。 // Add or update an observational entry and mark the data as dirty.
     */
    public void putObsidian(BlockPos pos, int ticksLeft) {
        this.obsidianData.put(pos, ticksLeft);
        setDirty();
    }

    /**
     * 移除一个 obsidian 条目（如果存在），并标记为脏。 // Remove an Obsidian entry (if present) and mark it as dirty.
     */
    public void removeObsidian(BlockPos pos) {
        if (this.obsidianData.remove(pos) != null) {
            setDirty();
        }
    }

    // --- 单个方块的拥有者数据 Owner data of a single block ---
    public Optional<UUID> getOwner(BlockPos pos) {
        return Optional.ofNullable(obsidianOwners.get(pos));
    }

    public void setOwner(BlockPos pos, UUID uuid) {
        obsidianOwners.put(pos, uuid);
        setDirty();
    }

    public void removeOwner(BlockPos pos) {
        if (obsidianOwners.remove(pos) != null) {
            setDirty();
        }
    }

    /**
     * 清理所有指定玩家 UUID 的 owner 记录（用于玩家死亡时调用） // Clean up all owner records for specified player UUIDs (to be called upon player death)
     */
    public void removeAllOwnersFor(UUID uuid) {
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, UUID>> it = obsidianOwners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, UUID> e = it.next();
            if (uuid.equals(e.getValue())) {
                it.remove();
                changed = true;
            }
        }
        if (changed) setDirty();
    }
}
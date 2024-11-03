package com.netpdrmod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SacredObsidianData extends SavedData {
    private static final String DATA_NAME = "sacred_obsidian_data";
    private Map<BlockPos, Integer> obsidianData = new HashMap<>();

    // 存储当前所有的黑曜石数据到 NBT // Save the current obsidian data to NBT
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound) {
        ListTag listTag = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : obsidianData.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getKey().asLong());
            entryTag.putInt("ticksLeft", entry.getValue());
            listTag.add(entryTag);
        }
        compound.put("obsidianData", listTag);
        return compound;
    }

    // 从 NBT 中加载黑曜石数据 // Load obsidian data from NBT
    public static SacredObsidianData load(CompoundTag compound) {
        SacredObsidianData data = new SacredObsidianData();
        ListTag listTag = compound.getList("obsidianData", Tag.TAG_COMPOUND);
        for (Tag tag : listTag) {
            CompoundTag entryTag = (CompoundTag) tag;
            BlockPos pos = BlockPos.of(entryTag.getLong("pos"));
            int ticksLeft = entryTag.getInt("ticksLeft");
            data.obsidianData.put(pos, ticksLeft);
        }
        return data;
    }

    // 获取或创建数据 // Get or create data
    public static SacredObsidianData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SacredObsidianData::load, SacredObsidianData::new, DATA_NAME);
    }

    public void setObsidianData(Map<BlockPos, Integer> data) {
        this.obsidianData = data;
        setDirty(); // 标记数据已修改 // Mark data as modified
    }

    public Map<BlockPos, Integer> getObsidianData() {
        return obsidianData;
    }
}
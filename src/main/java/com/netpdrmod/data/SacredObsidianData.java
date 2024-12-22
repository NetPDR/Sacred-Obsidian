package com.netpdrmod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SacredObsidianData extends SavedData {
    private static final String DATA_NAME = "sacred_obsidian_data";
    private Map<BlockPos, Integer> obsidianData = new HashMap<>();
    private UUID playerUuid;  // 存储玩家的 UUID // Stores the player's UUID

    // 存储当前所有的黑曜石数据到 NBT // Save all current obsidian data to NBT
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound, HolderLookup.@NotNull Provider provider) {
        ListTag listTag = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : obsidianData.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getKey().asLong());
            entryTag.putInt("ticksLeft", entry.getValue());
            listTag.add(entryTag);
        }
        compound.put("obsidianData", listTag);

        // 保存玩家的 UUID // Save the player's UUID
        if (playerUuid != null) {
            compound.putUUID("playerUuid", playerUuid);
        }

        return compound;
    }

    // 从 NBT 中加载黑曜石数据 // Load obsidian data from NBT
    public static SacredObsidianData load(CompoundTag compound, HolderLookup.Provider provider) {
        SacredObsidianData data = new SacredObsidianData();
        ListTag listTag = compound.getList("obsidianData", Tag.TAG_COMPOUND);
        for (Tag tag : listTag) {
            CompoundTag entryTag = (CompoundTag) tag;
            BlockPos pos = BlockPos.of(entryTag.getLong("pos"));
            int ticksLeft = entryTag.getInt("ticksLeft");
            data.obsidianData.put(pos, ticksLeft);
        }

        // 读取玩家的 UUID，使用 CompoundTag 的 getUUID 方法 // Read the player's UUID using the getUUID method of CompoundTag
        if (compound.contains("playerUuid")) {
            data.playerUuid = compound.getUUID("playerUuid");
        }

        return data;
    }

    // 获取或创建数据 // Get or create data
    public static SacredObsidianData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SacredObsidianData::new, SacredObsidianData::load, DataFixTypes.OPTIONS), DATA_NAME
        );
    }

    public void setObsidianData(Map<BlockPos, Integer> data) {
        this.obsidianData = data;
        setDirty(); // 标记数据已修改 // Mark data as modified
    }

    public Map<BlockPos, Integer> getObsidianData() {
        return obsidianData;
    }

    // 获取玩家 UUID // Get the player's UUID
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    // 设置玩家 UUID // Set the player's UUID
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
        setDirty();  // 标记数据已修改 // Mark data as modified
    }
}

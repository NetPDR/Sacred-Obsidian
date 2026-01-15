package com.netpdr.sacredobsidian.client;

import com.netpdr.sacredobsidian.Sacredobsidian;
import com.netpdr.sacredobsidian.common.AncientTomeCommon;
import com.netpdr.sacredobsidian.item.AncientTomeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Sacredobsidian.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AncientTomeAnvilHandler {

    // Pending map：记录 player UUID -> Pending 数据（并发安全） // Pending map: Records player UUID -> Pending data (concurrent safe)
    public static final Map<UUID, PendingAncientTome> PENDING = new ConcurrentHashMap<>();

    // Pending 结构（简单封装）
    public static class PendingAncientTome {
        public final ItemStack expectedResultSnapshot; // 我们在 AnvilUpdateEvent 时设置的 result item 快照 // The result item snapshot we set during the AnvilUpdateEvent
        public final ItemStack rightSlotSnapshot;      // 右槽（古卷）当时的快照 // Right slot (ancient scroll) snapshot at the time
        public final long timestamp;                   // 时间戳（ms），可用于清理过期条目 // Timestamp (ms), can be used to clean up expired entries

        public PendingAncientTome(ItemStack result, ItemStack right) {
            this.expectedResultSnapshot = result.copy();
            this.rightSlotSnapshot = right.copy();
            this.timestamp = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) return;
        if (!(right.getItem() instanceof AncientTomeItem tome)) return;

        Enchantment target = tome.getTargetEnchantment();
        if (target == null) return;

        ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(target);
        if (key == null) return;
        String targetId = key.toString();

        int currentLevel = EnchantmentHelper.getTagEnchantmentLevel(target, left);
        if (currentLevel >= AncientTomeCommon.MAX_OVERLEVEL) {
            return;
        }

        int newLevel = Math.min(currentLevel + 1, AncientTomeCommon.MAX_OVERLEVEL);

        ItemStack result = left.copy();
        CompoundTag root = result.getOrCreateTag();

        // 修改 Enchantments（物品自身） // Modify Enchantments (Item Itself)
        ListTag enchList = root.getList("Enchantments", Tag.TAG_COMPOUND);
        boolean found = false;
        for (int i = 0; i < enchList.size(); i++) {
            CompoundTag comp = enchList.getCompound(i);
            String id = comp.getString("id");
            if (id.equals(targetId)) {
                comp.putShort("lvl", (short) newLevel);
                found = true;
                break;
            }
        }
        if (!found) {
            CompoundTag comp = new CompoundTag();
            comp.putString("id", targetId);
            comp.putShort("lvl", (short) newLevel);
            enchList.add(comp);
        }
        root.put("Enchantments", enchList);

        // 兼容：StoredEnchantments（附魔书） // Compatible with: StoredEnchantments (Enchanted Books)
        ListTag stored = root.getList("StoredEnchantments", Tag.TAG_COMPOUND);
        boolean storedFound = false;
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag comp = stored.getCompound(i);
            String id = comp.getString("id");
            if (id.equals(targetId)) {
                comp.putShort("lvl", (short) newLevel);
                storedFound = true;
                break;
            }
        }
        if (!storedFound && left.getItem() == Items.ENCHANTED_BOOK) {
            CompoundTag comp = new CompoundTag();
            comp.putString("id", targetId);
            comp.putShort("lvl", (short) newLevel);
            stored.add(comp);
            root.put("StoredEnchantments", stored);
        } else {
            if (!stored.isEmpty()) root.put("StoredEnchantments", stored);
        }

        result.setTag(root);

        // 设置输出与消耗 // Set Output and Consumption
        event.setOutput(result);
        event.setCost(AncientTomeCommon.XP_COST);
        event.setMaterialCost(1);

        if (event.getPlayer() instanceof ServerPlayer sp) {
            // 记录期望的 result 与右槽（古卷）快照 // Record the expected result and the snapshot of the right slot (ancient scroll)
            PendingAncientTome p = new PendingAncientTome(result, right);
            PENDING.put(sp.getUUID(), p);
        }
    }
}
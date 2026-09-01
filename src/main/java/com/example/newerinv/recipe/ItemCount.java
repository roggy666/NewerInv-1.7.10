package com.example.newerinv.recipe;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemCount {

    private final Map<Integer, Integer> counts;

    private ItemCount(Map<Integer, Integer> counts) {
        this.counts = counts;
    }

    public static ItemCount empty() {
        return new ItemCount(new HashMap<Integer, Integer>());
    }

    public static ItemCount of(InventoryPlayer inv) {
        ItemCount c = empty();
        if (inv == null || inv.mainInventory == null) {
            return c;
        }
        for (ItemStack st : inv.mainInventory) {
            if (st == null || st.getItem() == null) {
                continue;
            }
            c.add(key(st.getItem(), st.getItemDamage()), st.stackSize);
        }
        return c;
    }

    public ItemCount copy() {
        return new ItemCount(new HashMap<Integer, Integer>(counts));
    }

    public void add(int key, int n) {
        Integer cur = counts.get(key);
        counts.put(key, (cur == null ? 0 : cur) + n);
    }

    public int get(int key) {
        Integer cur = counts.get(key);
        return cur == null ? 0 : cur;
    }

    public void remove(int key, int n) {
        int left = get(key) - n;
        if (left > 0) {
            counts.put(key, left);
        } else {
            counts.remove(key);
        }
    }

    public int anyMetaOf(int itemId) {
        int id = itemId & 0xFFFF;
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > 0 && (e.getKey() >>> 16) == id) {
                return e.getKey();
            }
        }
        return -1;
    }

    public static int key(Item item, int meta) {
        int id = Item.getIdFromItem(item) & 0xFFFF;
        return (id << 16) | (meta & 0xFFFF);
    }
}

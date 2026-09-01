package com.example.newerinv.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class Ingredient {

    public static final Ingredient EMPTY = new Ingredient(new ItemStack[0]);

    private final ItemStack[] alternatives;

    private Ingredient(ItemStack[] alternatives) {
        this.alternatives = alternatives;
    }

    public boolean isEmpty() {
        return alternatives.length == 0;
    }

    public ItemStack[] alternatives() {
        return alternatives;
    }

    public ItemStack first() {
        return alternatives.length == 0 ? null : alternatives[0];
    }

    public boolean accepts(ItemStack held) {
        if (held == null || held.getItem() == null) {
            return isEmpty();
        }
        for (ItemStack alt : alternatives) {
            if (alt == null || alt.getItem() == null) {
                continue;
            }
            if (alt.getItem() == held.getItem()
                    && (alt.getItemDamage() == OreDictionary.WILDCARD_VALUE
                        || alt.getItemDamage() == held.getItemDamage())) {
                return true;
            }
        }
        return false;
    }

    public static Ingredient fromStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return new Ingredient(new ItemStack[] { copy });
    }

    public static Ingredient fromList(List<?> list) {
        if (list == null || list.isEmpty()) {
            return EMPTY;
        }
        List<ItemStack> out = new ArrayList<ItemStack>(list.size());
        for (Object o : list) {
            if (o instanceof ItemStack) {
                ItemStack copy = ((ItemStack) o).copy();
                copy.stackSize = 1;
                out.add(copy);
            }
        }
        return out.isEmpty() ? EMPTY : new Ingredient(out.toArray(new ItemStack[out.size()]));
    }

    public static Ingredient fromObject(Object o) {
        if (o == null) {
            return EMPTY;
        }
        if (o instanceof ItemStack) {
            return fromStack((ItemStack) o);
        }
        if (o instanceof List) {
            return fromList((List<?>) o);
        }
        return EMPTY;
    }
}

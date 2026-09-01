package com.example.newerinv.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RecipeIndex {

    private RecipeIndex() {}

    private static final Logger LOG = LogManager.getLogger("NewerInv");

    private static final List<GridRecipe> ALL = new ArrayList<GridRecipe>();
    private static volatile boolean built = false;

    public static synchronized void build() {
        if (built) {
            return;
        }
        built = true;

        List<?> list;
        try {
            list = CraftingManager.getInstance().getRecipeList();
        } catch (Throwable t) {
            LOG.error("NewerInv: cannot read CraftingManager recipe list", t);
            return;
        }

        int total = 0;
        int decoded = 0;
        int errored = 0;
        Map<String, Integer> skipped = new TreeMap<String, Integer>();

        for (Object o : list) {
            if (!(o instanceof IRecipe)) {
                continue;
            }
            total++;
            GridRecipe r = null;
            try {
                r = RecipeExtractor.extract((IRecipe) o);
            } catch (Throwable t) {
                errored++;
            }
            if (r != null) {
                ALL.add(r);
                decoded++;
            } else {
                String k = o.getClass().getName();
                Integer c = skipped.get(k);
                skipped.put(k, c == null ? 1 : c + 1);
            }
        }

        LOG.info("NewerInv recipe index: {} crafting recipes, {} decoded, {} undecodable ({} threw)",
                total, decoded, total - decoded, errored);
        for (Map.Entry<String, Integer> e : skipped.entrySet()) {
            LOG.info("  undecodable  {} x{}", e.getKey(), e.getValue());
        }
    }

    public static List<GridRecipe> all() {
        return ALL;
    }

    public static List<GridRecipe> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<GridRecipe>(ALL);
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<GridRecipe> out = new ArrayList<GridRecipe>();
        for (GridRecipe r : ALL) {
            if (r.outputName().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(r);
            }
        }
        return out;
    }

    public static List<GridRecipe> filterCraftable(InventoryPlayer inv) {
        ItemCount base = ItemCount.of(inv);
        List<GridRecipe> out = new ArrayList<GridRecipe>();
        for (GridRecipe r : ALL) {
            if (canCraftConsuming(r, base.copy())) {
                out.add(r);
            }
        }
        return out;
    }

    public static List<GridRecipe> query(String text, InventoryPlayer inv, boolean craftableOnly) {
        List<GridRecipe> base = search(text);
        if (!craftableOnly || inv == null) {
            return base;
        }
        ItemCount count = ItemCount.of(inv);
        List<GridRecipe> out = new ArrayList<GridRecipe>();
        for (GridRecipe r : base) {
            if (canCraftConsuming(r, count.copy())) {
                out.add(r);
            }
        }
        return out;
    }

    public static boolean canCraft(GridRecipe recipe, InventoryPlayer inv) {
        return canCraftConsuming(recipe, ItemCount.of(inv));
    }

    private static boolean canCraftConsuming(GridRecipe recipe, ItemCount working) {
        for (Ingredient ing : recipe.slots) {
            if (ing.isEmpty()) {
                continue;
            }
            if (!consumeOne(working, ing)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeOne(ItemCount working, Ingredient ing) {
        for (ItemStack alt : ing.alternatives()) {
            if (alt == null || alt.getItem() == null) {
                continue;
            }
            if (alt.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                int k = working.anyMetaOf(Item.getIdFromItem(alt.getItem()));
                if (k != -1) {
                    working.remove(k, 1);
                    return true;
                }
            } else {
                int k = ItemCount.key(alt.getItem(), alt.getItemDamage());
                if (working.get(k) > 0) {
                    working.remove(k, 1);
                    return true;
                }
            }
        }
        return false;
    }
}

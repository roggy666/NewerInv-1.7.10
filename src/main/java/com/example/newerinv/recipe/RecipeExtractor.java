package com.example.newerinv.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public final class RecipeExtractor {

    private RecipeExtractor() {}

    public static GridRecipe extract(IRecipe recipe) {
        if (recipe == null) {
            return null;
        }
        ItemStack out;
        try {
            out = recipe.getRecipeOutput();
        } catch (Throwable t) {
            return null;
        }
        if (out == null || out.getItem() == null) {
            return null;
        }

        if (recipe instanceof ShapedRecipes) {
            return fromShaped((ShapedRecipes) recipe, out);
        }
        if (recipe instanceof ShapelessRecipes) {
            return fromShapeless((ShapelessRecipes) recipe, out);
        }
        if (recipe instanceof ShapedOreRecipe) {
            return fromShapedOre((ShapedOreRecipe) recipe, out);
        }
        if (recipe instanceof ShapelessOreRecipe) {
            return fromShapelessOre((ShapelessOreRecipe) recipe, out);
        }
        return null;
    }

    private static GridRecipe fromShaped(ShapedRecipes recipe, ItemStack out) {
        int w = (Integer) Reflect.get(recipe, ShapedRecipes.class, "recipeWidth", "field_77576_b", "a");
        int h = (Integer) Reflect.get(recipe, ShapedRecipes.class, "recipeHeight", "field_77577_c", "b");
        ItemStack[] items = (ItemStack[]) Reflect.get(recipe, ShapedRecipes.class, "recipeItems", "field_77574_d", "c");

        Ingredient[] slots = new Ingredient[w * h];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = Ingredient.fromStack(i < items.length ? items[i] : null);
        }
        return new GridRecipe(out.copy(), w, h, slots, false, recipe, "shaped");
    }

    private static GridRecipe fromShapeless(ShapelessRecipes recipe, ItemStack out) {
        List<?> items = (List<?>) Reflect.get(recipe, ShapelessRecipes.class, "recipeItems", "field_77579_b", "b");
        return packLoose(items, out, recipe, "shapeless", false);
    }

    private static GridRecipe fromShapedOre(ShapedOreRecipe recipe, ItemStack out) {
        int w = (Integer) Reflect.get(recipe, ShapedOreRecipe.class, "width");
        int h = (Integer) Reflect.get(recipe, ShapedOreRecipe.class, "height");
        Object[] input = (Object[]) Reflect.get(recipe, ShapedOreRecipe.class, "input");

        Ingredient[] slots = new Ingredient[w * h];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = Ingredient.fromObject(i < input.length ? input[i] : null);
        }
        return new GridRecipe(out.copy(), w, h, slots, false, recipe, "shapedOre");
    }

    private static GridRecipe fromShapelessOre(ShapelessOreRecipe recipe, ItemStack out) {
        List<?> input = (List<?>) Reflect.get(recipe, ShapelessOreRecipe.class, "input");
        return packLoose(input, out, recipe, "shapelessOre", true);
    }

    private static GridRecipe packLoose(List<?> items, ItemStack out, IRecipe source, String kind, boolean ore) {
        int n = items == null ? 0 : items.size();
        int w = Math.max(1, Math.min(3, n));
        int h = Math.max(1, (n + 2) / 3);
        Ingredient[] slots = new Ingredient[w * h];
        for (int i = 0; i < slots.length; i++) {
            if (i < n) {
                slots[i] = ore ? Ingredient.fromObject(items.get(i)) : Ingredient.fromStack((ItemStack) items.get(i));
            } else {
                slots[i] = Ingredient.EMPTY;
            }
        }
        return new GridRecipe(out.copy(), w, h, slots, true, source, kind);
    }
}

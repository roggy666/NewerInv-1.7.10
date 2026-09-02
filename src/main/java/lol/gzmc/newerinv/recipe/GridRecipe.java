package lol.gzmc.newerinv.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public final class GridRecipe {

    public final ItemStack output;
    public final int width;
    public final int height;
    public final Ingredient[] slots;
    public final boolean shapeless;
    public final IRecipe source;
    public final String kind;

    public GridRecipe(ItemStack output, int width, int height, Ingredient[] slots,
                      boolean shapeless, IRecipe source, String kind) {
        this.output = output;
        this.width = width;
        this.height = height;
        this.slots = slots;
        this.shapeless = shapeless;
        this.source = source;
        this.kind = kind;
    }

    public int ingredientCount() {
        int n = 0;
        for (Ingredient ing : slots) {
            if (!ing.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public boolean fits2x2() {
        if (shapeless) {
            return ingredientCount() <= 4;
        }
        return width <= 2 && height <= 2;
    }

    public String outputName() {
        if (output == null || output.getItem() == null) {
            return "";
        }
        try {
            return output.getDisplayName();
        } catch (Throwable t) {
            return String.valueOf(output.getUnlocalizedName());
        }
    }
}

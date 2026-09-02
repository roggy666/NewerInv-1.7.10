package lol.gzmc.newerinv.craft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lol.gzmc.newerinv.recipe.GridRecipe;
import lol.gzmc.newerinv.recipe.Ingredient;
import lol.gzmc.newerinv.recipe.ItemCount;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class GridFiller {

    private GridFiller() {}

    public static boolean place(GridRecipe recipe, boolean craftMax) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || recipe == null) {
            return false;
        }
        Container container = player.openContainer;
        GridLayout layout = GridLayout.detect(container);
        if (layout == null) {
            return false;
        }
        if (!recipe.shapeless && (recipe.width > layout.gridWidth || recipe.height > layout.gridWidth)) {
            return false;
        }
        if (cursor(player) != null) {
            return false;
        }
        if (recipe.shapeless && recipe.ingredientCount() > layout.gridSlots.size()) {
            return false;
        }

        PlayerControllerMP ctrl = mc.playerController;
        int win = container.windowId;

        if (!clearGrid(ctrl, container, layout, win, player)) {
            return false;
        }

        Map<Integer, ItemStack> plan = buildPlan(recipe, layout, player);
        if (plan == null || plan.isEmpty()) {
            return false;
        }

        int rounds = craftMax ? clamp(maxRounds(plan, player), 1, 64) : 1;

        for (ItemStack want : distinct(plan.values())) {
            List<Integer> targets = targetsFor(plan, want);
            int need = targets.size() * rounds;
            int placed = 0;
            int guard = 0;

            while (placed < need && guard++ < 128) {
                int src = findInvSlot(container, layout, want);
                if (src < 0) {
                    break;
                }
                ctrl.windowClick(win, src, 0, 0, player);
                if (cursor(player) == null) {
                    break;
                }

                int progressed = 0;
                for (int i = 0; i < targets.size() && placed < need; i++) {
                    ItemStack cur = cursor(player);
                    if (cur == null) {
                        break;
                    }
                    int had = cur.stackSize;
                    ctrl.windowClick(win, targets.get(i), 1, 0, player);
                    ItemStack after = cursor(player);
                    if (after == null || after.stackSize < had) {
                        placed++;
                        progressed++;
                    } else {
                        break;
                    }
                }

                if (cursor(player) != null) {
                    ctrl.windowClick(win, src, 0, 0, player);
                }
                if (cursor(player) != null) {
                    stashCursor(ctrl, container, layout, win, player);
                }
                if (progressed == 0) {
                    break;
                }
            }
        }

        if (cursor(player) != null) {
            stashCursor(ctrl, container, layout, win, player);
        }
        return true;
    }

    public static boolean clearGrid(PlayerControllerMP ctrl, Container container, GridLayout layout, int win, EntityPlayer player) {
        for (int gridSlot : layout.gridSlots) {
            if (container.getSlot(gridSlot).getHasStack()) {
                ctrl.windowClick(win, gridSlot, 0, 1, player);
            }
        }
        for (int gridSlot : layout.gridSlots) {
            if (container.getSlot(gridSlot).getHasStack()) {
                ctrl.windowClick(win, gridSlot, 0, 0, player);
                if (cursor(player) != null) {
                    ctrl.windowClick(win, -999, 0, 0, player);
                }
            }
        }
        if (cursor(player) != null) {
            ctrl.windowClick(win, -999, 0, 0, player);
        }
        return true;
    }

    private static void stashCursor(PlayerControllerMP ctrl, Container container, GridLayout layout, int win, EntityPlayer player) {
        ItemStack c = cursor(player);
        if (c == null) {
            return;
        }
        for (int s : layout.invSlots) {
            ItemStack st = container.getSlot(s).getStack();
            if (st != null && st.getItem() == c.getItem() && st.getItemDamage() == c.getItemDamage()
                    && st.stackSize < st.getMaxStackSize()) {
                ctrl.windowClick(win, s, 0, 0, player);
                if (cursor(player) == null) {
                    return;
                }
            }
        }
        int empty = findEmptyInvSlot(container, layout);
        if (empty >= 0) {
            ctrl.windowClick(win, empty, 0, 0, player);
            if (cursor(player) == null) {
                return;
            }
        }
        for (int g : layout.gridSlots) {
            if (!container.getSlot(g).getHasStack()) {
                ctrl.windowClick(win, g, 0, 0, player);
                if (cursor(player) == null) {
                    return;
                }
            }
        }
    }

    private static Map<Integer, ItemStack> buildPlan(GridRecipe recipe, GridLayout layout, EntityPlayer player) {
        ItemCount avail = ItemCount.of(player.inventory);
        LinkedHashMap<Integer, ItemStack> plan = new LinkedHashMap<Integer, ItemStack>();
        int loose = 0;
        for (int ri = 0; ri < recipe.slots.length; ri++) {
            Ingredient ing = recipe.slots[ri];
            if (ing.isEmpty()) {
                continue;
            }
            int gridSlot;
            if (recipe.shapeless) {
                gridSlot = layout.gridSlots.get(loose++);
            } else {
                gridSlot = layout.gridSlotAt(ri % recipe.width, ri / recipe.width);
            }
            if (gridSlot < 0) {
                return null;
            }
            ItemStack chosen = choose(ing, avail);
            if (chosen == null) {
                return null;
            }
            plan.put(gridSlot, chosen);
            avail.remove(ItemCount.key(chosen.getItem(), chosen.getItemDamage()), 1);
        }
        return plan;
    }

    private static ItemStack choose(Ingredient ing, ItemCount avail) {
        ItemStack best = null;
        int bestCount = 0;
        for (ItemStack alt : ing.alternatives()) {
            if (alt == null || alt.getItem() == null) {
                continue;
            }
            if (alt.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                int key = avail.anyMetaOf(Item.getIdFromItem(alt.getItem()));
                if (key < 0) {
                    continue;
                }
                int count = avail.get(key);
                if (count > bestCount) {
                    bestCount = count;
                    best = new ItemStack(alt.getItem(), 1, key & 0xFFFF);
                }
            } else {
                int count = avail.get(ItemCount.key(alt.getItem(), alt.getItemDamage()));
                if (count > bestCount) {
                    bestCount = count;
                    best = new ItemStack(alt.getItem(), 1, alt.getItemDamage());
                }
            }
        }
        return best;
    }

    private static int maxRounds(Map<Integer, ItemStack> plan, EntityPlayer player) {
        ItemCount avail = ItemCount.of(player.inventory);
        int rounds = 64;
        for (ItemStack want : distinct(plan.values())) {
            int uses = targetsFor(plan, want).size();
            if (uses == 0) {
                continue;
            }
            int have = avail.get(ItemCount.key(want.getItem(), want.getItemDamage()));
            rounds = Math.min(rounds, have / uses);
        }
        return rounds;
    }

    private static List<ItemStack> distinct(Iterable<ItemStack> stacks) {
        Set<Integer> seen = new LinkedHashSet<Integer>();
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (ItemStack s : stacks) {
            if (seen.add(ItemCount.key(s.getItem(), s.getItemDamage()))) {
                out.add(s);
            }
        }
        return out;
    }

    private static List<Integer> targetsFor(Map<Integer, ItemStack> plan, ItemStack want) {
        List<Integer> out = new ArrayList<Integer>();
        for (Map.Entry<Integer, ItemStack> e : plan.entrySet()) {
            ItemStack s = e.getValue();
            if (s.getItem() == want.getItem() && s.getItemDamage() == want.getItemDamage()) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private static int findInvSlot(Container container, GridLayout layout, ItemStack want) {
        for (int slotNumber : layout.invSlots) {
            Slot slot = container.getSlot(slotNumber);
            ItemStack st = slot.getStack();
            if (st != null && st.stackSize > 0
                    && st.getItem() == want.getItem()
                    && st.getItemDamage() == want.getItemDamage()) {
                return slotNumber;
            }
        }
        return -1;
    }

    private static int findEmptyInvSlot(Container container, GridLayout layout) {
        for (int slotNumber : layout.invSlots) {
            if (!container.getSlot(slotNumber).getHasStack()) {
                return slotNumber;
            }
        }
        return -1;
    }

    public static boolean craftAll() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return false;
        }
        Container container = player.openContainer;
        GridLayout layout = GridLayout.detect(container);
        if (layout == null || !container.getSlot(layout.resultSlot).getHasStack()) {
            return false;
        }
        mc.playerController.windowClick(container.windowId, layout.resultSlot, 0, 1, player);
        return true;
    }

    private static ItemStack cursor(EntityPlayer player) {
        return player.inventory.getItemStack();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

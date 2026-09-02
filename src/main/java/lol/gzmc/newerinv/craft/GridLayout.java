package lol.gzmc.newerinv.craft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

public final class GridLayout {

    public final int resultSlot;
    public final List<Integer> gridSlots;
    public final List<Integer> invSlots;
    public final int gridWidth;

    private GridLayout(int resultSlot, List<Integer> gridSlots, List<Integer> invSlots, int gridWidth) {
        this.resultSlot = resultSlot;
        this.gridSlots = gridSlots;
        this.invSlots = invSlots;
        this.gridWidth = gridWidth;
    }

    public static GridLayout detect(Container container) {
        if (container == null || container.inventorySlots == null) {
            return null;
        }

        int result = -1;
        List<Integer> grid = new ArrayList<Integer>();
        List<Integer> inv = new ArrayList<Integer>();

        for (Object o : container.inventorySlots) {
            Slot slot = (Slot) o;
            if (slot instanceof SlotCrafting) {
                result = slot.slotNumber;
            } else if (slot.inventory instanceof InventoryCrafting) {
                grid.add(slot.slotNumber);
            } else if (slot.inventory instanceof InventoryPlayer) {
                inv.add(slot.slotNumber);
            }
        }

        if (result < 0 || grid.isEmpty() || inv.isEmpty()) {
            return null;
        }

        int width = grid.size() >= 9 ? 3 : 2;
        return new GridLayout(result, grid, inv, width);
    }

    public int gridSlotAt(int col, int row) {
        int index = row * gridWidth + col;
        return index >= 0 && index < gridSlots.size() ? gridSlots.get(index) : -1;
    }
}

package com.example.newerinv.craft;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.newerinv.recipe.GridRecipe;
import com.example.newerinv.recipe.Ingredient;
import com.example.newerinv.recipe.ItemCount;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.opengl.GL11;

public class GhostRecipe {

    public static final int RED_TINT = 0x4CE55555;
    public static final int GHOST_WASH = 0x808B8B8B;

    public final GridRecipe recipe;
    public final Map<Slot, ItemStack> ghostSlots = new LinkedHashMap<Slot, ItemStack>();
    public Slot resultSlot;
    public ItemStack resultItem;

    public GhostRecipe(GridRecipe recipe, GridLayout layout, Container container) {
        this.recipe = recipe;
        if (layout.resultSlot >= 0 && layout.resultSlot < container.inventorySlots.size()) {
            this.resultSlot = container.getSlot(layout.resultSlot);
            this.resultItem = recipe.output;
        }

        int loose = 0;
        for (int ri = 0; ri < recipe.slots.length; ri++) {
            Ingredient ing = recipe.slots[ri];
            if (ing.isEmpty()) {
                continue;
            }
            int slotNum = recipe.shapeless
                    ? (loose < layout.gridSlots.size() ? layout.gridSlots.get(loose++) : -1)
                    : layout.gridSlotAt(ri % recipe.width, ri / recipe.width);
            if (slotNum >= 0 && slotNum < container.inventorySlots.size()) {
                Slot s = container.getSlot(slotNum);
                ItemStack display = ing.first();
                if (display != null) {
                    ItemStack copy = display.copy();
                    if (copy.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                        copy.setItemDamage(0);
                    }
                    ghostSlots.put(s, copy);
                }
            }
        }
    }

    public void draw(Minecraft mc, FontRenderer font, RenderItem itemRender, int guiLeft, int guiTop) {
        EntityPlayer player = mc.thePlayer;
        ItemCount avail = player != null ? ItemCount.of(player.inventory) : null;

        for (Map.Entry<Slot, ItemStack> entry : ghostSlots.entrySet()) {
            Slot slot = entry.getKey();
            ItemStack stack = entry.getValue();
            if (slot.getHasStack() || stack == null) {
                continue;
            }
            int x = guiLeft + slot.xDisplayPosition;
            int y = guiTop + slot.yDisplayPosition;

            boolean has = false;
            if (avail != null) {
                int key = ItemCount.key(stack.getItem(), stack.getItemDamage());
                int count = avail.get(key);
                if (count > 0) {
                    has = true;
                    avail.remove(key, 1);
                }
            }

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            if (!has) {
                Gui.drawRect(x, y, x + 16, y + 16, RED_TINT);
            } else {
                Gui.drawRect(x, y, x + 16, y + 16, 0x258B8B8B);
            }

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), stack, x, y);
            RenderHelper.disableStandardItemLighting();

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            Gui.drawRect(x, y, x + 16, y + 16, GHOST_WASH);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        if (resultSlot != null && !resultSlot.getHasStack() && resultItem != null) {
            int rx = guiLeft + resultSlot.xDisplayPosition;
            int ry = guiTop + resultSlot.yDisplayPosition;

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            Gui.drawRect(rx - 4, ry - 4, rx + 20, ry + 20, RED_TINT);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), resultItem, rx, ry);
            RenderHelper.disableStandardItemLighting();

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            Gui.drawRect(rx, ry, rx + 16, ry + 16, GHOST_WASH);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    public ItemStack getHoveredGhostItem(int guiLeft, int guiTop, int mouseX, int mouseY) {
        for (Map.Entry<Slot, ItemStack> entry : ghostSlots.entrySet()) {
            Slot slot = entry.getKey();
            if (!slot.getHasStack()) {
                int x = guiLeft + slot.xDisplayPosition;
                int y = guiTop + slot.yDisplayPosition;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    return entry.getValue();
                }
            }
        }
        if (resultSlot != null && !resultSlot.getHasStack() && resultItem != null) {
            int rx = guiLeft + resultSlot.xDisplayPosition;
            int ry = guiTop + resultSlot.yDisplayPosition;
            if (mouseX >= rx - 4 && mouseX < rx + 20 && mouseY >= ry - 4 && mouseY < ry + 20) {
                return resultItem;
            }
        }
        return null;
    }
}

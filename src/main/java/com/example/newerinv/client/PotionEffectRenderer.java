package com.example.newerinv.client;

import java.lang.reflect.Field;
import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class PotionEffectRenderer {

    private static final ResourceLocation inventoryGuiTextures =
            new ResourceLocation("textures/gui/container/inventory.png");

    public static void disableVanillaPotionEffects(GuiScreen gui) {
        if (gui instanceof InventoryEffectRenderer) {
            try {
                Field f = InventoryEffectRenderer.class.getDeclaredField("field_147045_u");
                f.setAccessible(true);
                f.setBoolean(gui, false);
            } catch (Throwable t) {
                try {
                    Field f = InventoryEffectRenderer.class.getDeclaredField("hasActivePotionEffects");
                    f.setAccessible(true);
                    f.setBoolean(gui, false);
                } catch (Throwable ignored) {}
            }
        }
    }

    public static int getXSize(GuiContainer container) {
        try {
            Field f = GuiContainer.class.getDeclaredField("field_146999_f");
            f.setAccessible(true);
            return f.getInt(container);
        } catch (Throwable t) {
            try {
                Field f = GuiContainer.class.getDeclaredField("xSize");
                f.setAccessible(true);
                return f.getInt(container);
            } catch (Throwable ignored) {}
        }
        return 176;
    }

    public static int getGuiLeft(GuiContainer container) {
        try {
            Field f = GuiContainer.class.getDeclaredField("field_147003_i");
            f.setAccessible(true);
            return f.getInt(container);
        } catch (Throwable t) {
            try {
                Field f = GuiContainer.class.getDeclaredField("guiLeft");
                f.setAccessible(true);
                return f.getInt(container);
            } catch (Throwable ignored) {}
        }
        return (container.width - 176) / 2;
    }

    public static void setGuiLeft(GuiContainer container, int left) {
        try {
            Field f = GuiContainer.class.getDeclaredField("field_147003_i");
            f.setAccessible(true);
            f.setInt(container, left);
        } catch (Throwable t) {
            try {
                Field f = GuiContainer.class.getDeclaredField("guiLeft");
                f.setAccessible(true);
                f.setInt(container, left);
            } catch (Throwable ignored) {}
        }
    }

    public static int getGuiTop(GuiContainer container) {
        try {
            Field f = GuiContainer.class.getDeclaredField("field_147009_r");
            f.setAccessible(true);
            return f.getInt(container);
        } catch (Throwable t) {
            try {
                Field f = GuiContainer.class.getDeclaredField("guiTop");
                f.setAccessible(true);
                return f.getInt(container);
            } catch (Throwable ignored) {}
        }
        return (container.height - 166) / 2;
    }

    @SuppressWarnings("unchecked")
    public static void renderCleanPotionEffects(GuiContainer container, int extraXOffset, boolean shiftForBook) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        int guiLeft = getGuiLeft(container);
        int guiTop = getGuiTop(container);

        int startX = guiLeft + extraXOffset - 124;
        if (shiftForBook && BookPanel.isOpen()) {
            startX = guiLeft + extraXOffset - 147 - 124;
        }
        int startY = guiTop;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);

        int spacing = 33;
        if (effects.size() > 5) {
            spacing = 132 / (effects.size() - 1);
        }

        FontRenderer font = mc.fontRenderer;

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) {
                continue;
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(inventoryGuiTextures);

            container.drawTexturedModalRect(startX, startY, 0, 166, 120, 32);

            if (potion.hasStatusIcon()) {
                int iconIndex = potion.getStatusIconIndex();
                container.drawTexturedModalRect(startX + 6, startY + 7, (iconIndex % 8) * 18, 198 + (iconIndex / 8) * 18, 18, 18);
            }

            String name = I18n.format(potion.getName());
            if (effect.getAmplifier() == 1) {
                name = name + " " + I18n.format("enchantment.level.2");
            } else if (effect.getAmplifier() == 2) {
                name = name + " " + I18n.format("enchantment.level.3");
            } else if (effect.getAmplifier() == 3) {
                name = name + " " + I18n.format("enchantment.level.4");
            }

            font.drawStringWithShadow(name, startX + 10 + 18, startY + 6, 16777215);
            String duration = Potion.getDurationString(effect);
            font.drawStringWithShadow(duration, startX + 10 + 18, startY + 6 + 10, 8355711);

            startY += spacing;
        }
    }
}

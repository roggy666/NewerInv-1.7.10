package com.example.newerinv.client;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class GuiInventoryBook extends GuiInventory implements BookHost {

    private final BookPanel panel = new BookPanel();
    private static final ResourceLocation inventoryGuiTextures =
            new ResourceLocation("textures/gui/container/inventory.png");

    public GuiInventoryBook(EntityPlayer player) {
        super(player);
    }

    public int getSlotXOffset() {
        if (this.inventorySlots != null && this.inventorySlots.inventorySlots != null) {
            for (Object obj : this.inventorySlots.inventorySlots) {
                if (obj instanceof Slot) {
                    Slot s = (Slot) obj;
                    if (s.inventory == this.mc.thePlayer.inventory && s.getSlotIndex() == 9) {
                        return s.xDisplayPosition - 8;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public void initGui() {
        super.initGui();
        disableVanillaPotionRendering();
        updateScreenPosition();
        int offset = getSlotXOffset();
        panel.init(this, 104 + offset, 61);
    }

    private void disableVanillaPotionRendering() {
        try {
            Field f = InventoryEffectRenderer.class.getDeclaredField("field_147045_u");
            f.setAccessible(true);
            f.setBoolean(this, false);
        } catch (Throwable t) {
            try {
                Field f = InventoryEffectRenderer.class.getDeclaredField("hasActivePotionEffects");
                f.setAccessible(true);
                f.setBoolean(this, false);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void updateScreenPosition() {
        if (BookPanel.isOpen()) {
            this.guiLeft = (this.width - 147 - this.xSize) / 2 + 147;
        } else {
            this.guiLeft = (this.width - this.xSize) / 2;
        }
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(inventoryGuiTextures);
        int offset = getSlotXOffset();
        this.drawTexturedModalRect(this.guiLeft + offset, this.guiTop, 0, 0, this.xSize, this.ySize);
        func_147046_a(this.guiLeft + 51 + offset, this.guiTop + 75, 30, (float)(this.guiLeft + 51 + offset) - (float)mouseX, (float)(this.guiTop + 75 - 50) - (float)mouseY, this.mc.thePlayer);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCleanPotionEffects();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        panel.draw(this, mouseX, mouseY);
    }

    @SuppressWarnings("unchecked")
    private void drawCleanPotionEffects() {
        Collection<PotionEffect> effects = this.mc.thePlayer.getActivePotionEffects();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        int offset = getSlotXOffset();
        int startX = this.guiLeft + offset - 124;
        if (BookPanel.isOpen()) {
            startX = this.guiLeft + offset - 147 - 124;
        }
        int startY = this.guiTop;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);

        int spacing = 33;
        if (effects.size() > 5) {
            spacing = 132 / (effects.size() - 1);
        }

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) {
                continue;
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(inventoryGuiTextures);

            this.drawTexturedModalRect(startX, startY, 0, 166, 115, 32);
            this.drawTexturedModalRect(startX + 115, startY, 135, 166, 5, 32);

            if (potion.hasStatusIcon()) {
                int iconIndex = potion.getStatusIconIndex();
                this.drawTexturedModalRect(startX + 6, startY + 7, (iconIndex % 8) * 18, 198 + (iconIndex / 8) * 18, 18, 18);
            }

            String name = I18n.format(potion.getName());
            if (effect.getAmplifier() == 1) {
                name = name + " " + I18n.format("enchantment.level.2");
            } else if (effect.getAmplifier() == 2) {
                name = name + " " + I18n.format("enchantment.level.3");
            } else if (effect.getAmplifier() == 3) {
                name = name + " " + I18n.format("enchantment.level.4");
            }

            this.fontRendererObj.drawStringWithShadow(name, startX + 10 + 18, startY + 6, 16777215);
            String duration = Potion.getDurationString(effect);
            this.fontRendererObj.drawStringWithShadow(duration, startX + 10 + 18, startY + 6 + 10, 8355711);

            startY += spacing;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (panel.keyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (panel.mouseClicked(this, mouseX, mouseY, mouseButton)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() {
        if (panel.handleWheel(this)) {
            return;
        }
        super.handleMouseInput();
    }

    @Override
    public int screenWidth() {
        return this.width;
    }

    @Override
    public int screenHeight() {
        return this.height;
    }

    @Override
    public int anchorLeft() {
        return this.guiLeft + getSlotXOffset();
    }

    @Override
    public int anchorTop() {
        return this.guiTop;
    }

    @Override
    public int containerWidth() {
        return this.xSize;
    }

    @Override
    public int containerHeight() {
        return this.ySize;
    }

    @Override
    public FontRenderer font() {
        return this.fontRendererObj;
    }

    @Override
    public void renderTooltipLines(List<String> lines, int x, int y) {
        this.func_146283_a(lines, x, y);
    }

    @Override
    public void renderItemTooltip(ItemStack stack, int x, int y) {
        this.renderToolTip(stack, x, y);
    }
}

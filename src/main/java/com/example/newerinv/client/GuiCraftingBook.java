package com.example.newerinv.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class GuiCraftingBook extends GuiCrafting implements BookHost {

    private final BookPanel panel = new BookPanel();

    /**
     * Third-party button -> the X it was created with, captured once, relative to a vanilla-centered
     * GUI. Every frame it is re-placed at anchorLeft() + that offset so it follows the container when
     * the book pushes it aside. Absolute, so repeated open/close never drifts. Mirrors
     * GuiInventoryBook.
     */
    private final Map<GuiButton, Integer> buttonOriginOffset = new HashMap<GuiButton, Integer>();

    public GuiCraftingBook(InventoryPlayer playerInv, World world) {
        super(playerInv, world, 0, 0, 0);
    }

    private int vanillaCenterLeft() {
        return (this.width - this.xSize) / 2;
    }

    private boolean isTabButton(GuiButton btn) {
        if (btn == null) {
            return false;
        }
        String name = btn.getClass().getName();
        return name.contains("Tab") || name.contains("tab");
    }

    private void trackButtons(List buttons) {
        if (buttons == null) {
            return;
        }
        int base = vanillaCenterLeft();
        for (Object obj : buttons) {
            if (obj instanceof GuiButton) {
                GuiButton btn = (GuiButton) obj;
                if (isTabButton(btn)) {
                    continue;
                }
                if (!buttonOriginOffset.containsKey(btn)) {
                    buttonOriginOffset.put(btn, btn.xPosition - base);
                }
            }
        }
    }

    private void layoutButtons() {
        if (this.buttonList == null || buttonOriginOffset.isEmpty()) {
            return;
        }
        int anchor = anchorLeft();
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                Integer off = buttonOriginOffset.get(obj);
                if (off != null) {
                    ((GuiButton) obj).xPosition = anchor + off;
                }
            }
        }
    }

    public void adjustPostButtons(List buttonList) {
        trackButtons(buttonList);
        layoutButtons();
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonOriginOffset.clear();
        updateScreenPosition();
        trackButtons(this.buttonList);
        layoutButtons();
        panel.init(this, 5, 35);
    }

    @Override
    public void updateScreenPosition() {
        if (BookPanel.isOpen()) {
            this.guiLeft = (this.width - 147 - this.xSize) / 2 + 147;
        } else {
            this.guiLeft = (this.width - this.xSize) / 2;
        }
        this.guiTop = (this.height - this.ySize) / 2;

        layoutButtons();
    }

    private static final net.minecraft.util.ResourceLocation craftingTableGuiTextures =
            new net.minecraft.util.ResourceLocation("textures/gui/container/crafting_table.png");

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // After any per-frame guiLeft override, before GuiScreen draws the button list.
        layoutButtons();

        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(craftingTableGuiTextures);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.crafting"), 29, 6, 4210752);
        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
        panel.draw(this, mouseX, mouseY);
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
        return this.guiLeft;
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


package lol.gzmc.newerinv.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class GuiInventoryBook extends GuiInventory implements BookHost {

    private final BookPanel panel = new BookPanel();
    private static final ResourceLocation inventoryGuiTextures =
            new ResourceLocation("textures/gui/container/inventory.png");

    /**
     * Third-party button -> the X it was created with (captured once, before we move it), stored
     * relative to a vanilla-centered GUI. Every frame the button is re-placed at
     * anchorLeft() + originOffset, so it tracks the container wherever it actually renders --
     * including when NEI's LayoutManager.onPreDraw forces guiLeft back to screen center each frame.
     */
    private final Map<GuiButton, Integer> buttonOriginOffset = new HashMap<GuiButton, Integer>();

    public GuiInventoryBook(EntityPlayer player) {
        super(player);
    }

    private int getSlotXOffset() {
        if (this.inventorySlots != null && this.inventorySlots.inventorySlots != null && this.mc != null && this.mc.thePlayer != null) {
            for (Object obj : this.inventorySlots.inventorySlots) {
                if (obj instanceof Slot) {
                    Slot slot = (Slot) obj;
                    if (slot.inventory == this.mc.thePlayer.inventory && slot.getSlotIndex() == 0) {
                        return slot.xDisplayPosition - 8;
                    }
                }
            }
        }
        return 0;
    }

    private Slot findOffhandSlot() {
        if (this.inventorySlots != null && this.inventorySlots.inventorySlots != null) {
            for (Object obj : this.inventorySlots.inventorySlots) {
                if (obj instanceof Slot) {
                    Slot slot = (Slot) obj;
                    String name = slot.getClass().getName();
                    if (name.contains("Backhand") || name.contains("Offhand") || name.contains("OffHand")) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    public void updateTabRegistry() {
        try {
            Class<?> reg = Class.forName("tconstruct.client.tabs.TabRegistry");
            Class<?> vanillaTab = Class.forName("tconstruct.client.tabs.InventoryTabVanilla");
            Method m = reg.getMethod("updateTabValues", int.class, int.class, Class.class);
            int cornerX = this.guiLeft + getSlotXOffset();
            int cornerY = this.guiTop;
            m.invoke(null, cornerX, cornerY, vanillaTab);
        } catch (Throwable ignored) {
        }
    }

    private boolean isTabButton(GuiButton btn) {
        if (btn == null) {
            return false;
        }
        String name = btn.getClass().getName();
        return name.contains("Tab") || name.contains("tab");
    }

    private int vanillaCenterLeft() {
        return (this.width - this.xSize) / 2;
    }

    @Override
    public void initGui() {
        super.initGui();
        NeiCompat.registerDrawHandler();
        buttonOriginOffset.clear();
        PotionEffectRenderer.disableVanillaPotionEffects(this);
        updateScreenPosition();
        trackButtons(this.buttonList);
        layoutButtons();
        updateTabRegistry();
        panel.init(this, 104, 61);
    }

    public void adjustPostButtons(List buttonList) {
        trackButtons(buttonList);
        layoutButtons();
        updateTabRegistry();
    }

    /**
     * Record each new third-party button's origin X once, relative to a vanilla-centered GUI (the
     * same assumption the old delta shift made). Tab buttons are left to updateTabRegistry().
     */
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

    /**
     * Re-place every tracked button at the live anchor plus its captured offset. Called every frame
     * from drawGuiContainerBackgroundLayer (after NEI's guiLeft reset, before the button list is
     * drawn) and whenever our own layout changes. Absolute, not incremental, so repeated
     * open/close of the book never accumulates drift.
     */
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

    /**
     * The guiLeft/guiTop we want. Also invoked (via NeiCompat's draw handler) from NEI's onPreDraw
     * pass, after NEI's LayoutManager re-centers guiLeft and before vanilla GuiContainer.drawScreen
     * snapshots it, so the inventory still slides aside for the book under NEI.
     */
    void applyDesiredPosition() {
        int offX = getSlotXOffset();
        if (BookPanel.isOpen()) {
            this.guiLeft = (this.width - 147 - (this.xSize + offX)) / 2 + 147;
        } else {
            this.guiLeft = (this.width - (this.xSize + offX)) / 2;
        }
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void updateScreenPosition() {
        applyDesiredPosition();
        layoutButtons();
        updateTabRegistry();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(I18n.format("container.crafting"), 86 + getSlotXOffset(), 16, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // Runs after NEI's per-frame guiLeft reset (LayoutManager.onPreDraw) but before GuiScreen
        // draws the button list, so third-party buttons render aligned with the real container.
        layoutButtons();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(inventoryGuiTextures);
        int offX = getSlotXOffset();
        this.drawTexturedModalRect(this.guiLeft + offX, this.guiTop, 0, 0, this.xSize, this.ySize);
        func_147046_a(this.guiLeft + offX + 51, this.guiTop + 75, 30, (float)(this.guiLeft + offX + 51) - (float)mouseX, (float)(this.guiTop + 75 - 50) - (float)mouseY, this.mc.thePlayer);

        Slot offhandSlot = findOffhandSlot();
        if (offhandSlot != null) {
            int sx = this.guiLeft + offhandSlot.xDisplayPosition - 2;
            int sy = this.guiTop + offhandSlot.yDisplayPosition - 2;
            boolean rendered = false;
            try {
                Class<?> helper = Class.forName("xonin.backhand.client.utils.BackhandRenderHelper");
                Method m = helper.getMethod("drawItemStackSlot", int.class, int.class);
                m.invoke(null, sx, sy);
                rendered = true;
            } catch (Throwable ignored) {
            }
            if (!rendered) {
                this.mc.getTextureManager().bindTexture(inventoryGuiTextures);
                this.drawTexturedModalRect(sx + 1, sy + 1, 7, 83, 18, 18);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        PotionEffectRenderer.renderCleanPotionEffects(this, 0, true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
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

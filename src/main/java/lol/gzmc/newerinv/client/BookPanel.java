package lol.gzmc.newerinv.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lol.gzmc.newerinv.assets.NewerTextures;
import lol.gzmc.newerinv.config.Config;
import lol.gzmc.newerinv.craft.GhostRecipe;
import lol.gzmc.newerinv.craft.GridFiller;
import lol.gzmc.newerinv.craft.GridLayout;
import lol.gzmc.newerinv.recipe.GridRecipe;
import lol.gzmc.newerinv.recipe.RecipeIndex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class BookPanel extends Gui {

    public static final int PANEL_WIDTH = 147;
    public static final int PANEL_HEIGHT = 166;
    /** Keep the panel body on-screen when the host GUI stays centered (e.g. NEI holding guiLeft at center). */
    private static final int MIN_BOOK_LEFT = 2;
    public static final int PAGE_SIZE = 20;
    public static final int COLS = 5;
    public static final int ROWS = 4;

    private static boolean open = Config.openByDefault;
    private static boolean craftableOnly = false;
    private static int activeTab = 0;
    private static String search = "";

    private final RenderItem itemRender = new RenderItem();

    private int bookX;
    private int bookY;
    private int toggleBtnXOffset;
    private int toggleBtnYOffset;

    private int page = 0;
    private GuiTextField searchField;
    private List<GridRecipe> results = new ArrayList<GridRecipe>();
    private boolean dirty = true;
    private GridRecipe lastPlaced = null;
    private GhostRecipe ghostRecipe = null;

    public static boolean isOpen() {
        return open;
    }

    public static void setOpen(boolean state) {
        open = state;
    }

    public void init(BookHost host, int toggleXOffset, int toggleYOffset) {
        this.toggleBtnXOffset = toggleXOffset;
        this.toggleBtnYOffset = toggleYOffset;

        this.bookX = Math.max(MIN_BOOK_LEFT, host.anchorLeft() - PANEL_WIDTH);
        this.bookY = host.anchorTop();

        Keyboard.enableRepeatEvents(true);
        this.searchField = new GuiTextField(host.font(), bookX + 28, bookY + 16, 75, 9);
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        this.searchField.setText(search);

        this.ghostRecipe = null;
        this.dirty = true;
    }

    public boolean mouseClicked(BookHost host, int mx, int my, int button) {
        int toggleX = host.anchorLeft() + toggleBtnXOffset;
        int toggleY = host.anchorTop() + toggleBtnYOffset;
        if (mx >= toggleX && mx < toggleX + 20 && my >= toggleY && my < toggleY + 18) {
            open = !open;
            ghostRecipe = null;
            playClickSound();
            host.updateScreenPosition();
            init(host, toggleBtnXOffset, toggleBtnYOffset);
            return true;
        }

        if (!open) {
            ghostRecipe = null;
            return false;
        }

        if (!inBook(mx, my)) {
            ghostRecipe = null;
        }

        for (int i = 0; i < 4; i++) {
            int tabY = bookY + 3 + i * 27;
            int tabX = (i == activeTab) ? (bookX - 30) : (bookX - 28);
            int tabW = (i == activeTab) ? 35 : 30;
            int tabH = 26;
            if (mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + tabH) {
                if (activeTab != i) {
                    activeTab = i;
                    page = 0;
                    dirty = true;
                    ghostRecipe = null;
                    playClickSound();
                }
                return true;
            }
        }

        int filterX = bookX + 110;
        int filterY = bookY + 14;
        if (mx >= filterX && mx < filterX + 26 && my >= filterY && my < filterY + 16) {
            craftableOnly = !craftableOnly;
            page = 0;
            dirty = true;
            ghostRecipe = null;
            playClickSound();
            return true;
        }

        int boxX = bookX + 25;
        int boxY = bookY + 13;
        int boxW = 81;
        int boxH = 14;
        if (mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH) {
            if (searchField != null) {
                if (button == 1) {
                    searchField.setText("");
                    search = "";
                    page = 0;
                    dirty = true;
                    ghostRecipe = null;
                }
                searchField.setFocused(true);
            }
            return true;
        } else if (searchField != null && searchField.isFocused()) {
            searchField.setFocused(false);
        }

        int prevX = bookX + 38;
        int prevY = bookY + 137;
        if (page > 0 && mx >= prevX && mx < prevX + 12 && my >= prevY && my < prevY + 17) {
            page--;
            ghostRecipe = null;
            playClickSound();
            return true;
        }

        int nextX = bookX + 93;
        int nextY = bookY + 137;
        int totalPages = maxPage() + 1;
        if (page < totalPages - 1 && mx >= nextX && mx < nextX + 12 && my >= nextY && my < nextY + 17) {
            page++;
            ghostRecipe = null;
            playClickSound();
            return true;
        }

        int slotIdx = slotAt(mx, my);
        if (slotIdx >= 0 && slotIdx < results.size()) {
            activate(results.get(slotIdx), button);
            return true;
        }

        return inBook(mx, my);
    }

    private void activate(GridRecipe r, int button) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p == null) {
            return;
        }
        Container container = p.openContainer;
        GridLayout layout = GridLayout.detect(container);
        if (layout == null) {
            return;
        }
        if (!r.shapeless && (r.width > layout.gridWidth || r.height > layout.gridWidth)) {
            return;
        }

        boolean canCraft = RecipeIndex.canCraft(r, p.inventory);

        if (canCraft) {
            ghostRecipe = null;
            boolean max = Config.craftMaxOnShift && (button == 1 || shiftDown());
            if (r == lastPlaced && GridFiller.craftAll()) {
                playClickSound();
                return;
            }
            if (GridFiller.place(r, max)) {
                lastPlaced = r;
                playClickSound();
            }
        } else {
            GridFiller.clearGrid(mc.playerController, container, layout, container.windowId, p);

            if (ghostRecipe != null && ghostRecipe.recipe == r) {
                ghostRecipe = null;
            } else {
                ghostRecipe = new GhostRecipe(r, layout, container);
                playClickSound();
            }
        }
        dirty = true;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!open || searchField == null) {
            return false;
        }
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            search = searchField.getText();
            page = 0;
            dirty = true;
            ghostRecipe = null;
            return true;
        }
        return searchField.isFocused();
    }

    public boolean handleWheel(BookHost host) {
        if (!open) {
            return false;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int mx = Mouse.getEventX() * host.screenWidth() / mc.displayWidth;
        int my = host.screenHeight() - Mouse.getEventY() * host.screenHeight() / mc.displayHeight - 1;
        if (!inBook(mx, my)) {
            return false;
        }
        int totalPages = maxPage() + 1;
        if (wheel > 0 && page > 0) {
            page--;
            return true;
        } else if (wheel < 0 && page < totalPages - 1) {
            page++;
            return true;
        }
        return false;
    }

    public void draw(BookHost host, int mouseX, int mouseY) {
        FontRenderer font = host.font();
        Minecraft mc = Minecraft.getMinecraft();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        RenderHelper.disableStandardItemLighting();
        GL11.glEnable(GL11.GL_BLEND);
        net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        int toggleX = host.anchorLeft() + toggleBtnXOffset;
        int toggleY = host.anchorTop() + toggleBtnYOffset;
        boolean toggleHovered = (mouseX >= toggleX && mouseX < toggleX + 20 && mouseY >= toggleY && mouseY < toggleY + 18);

        mc.getTextureManager().bindTexture(NewerTextures.RECIPE_BOOK);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(toggleX, toggleY, 0, toggleHovered ? 186 : 168, 20, 18);

        List<String> tipLines = null;
        ItemStack tipStack = null;

        if (toggleHovered) {
            tipLines = one(StatCollector.translateToLocal(open ? "newerinv.book.hide" : "newerinv.book.show"));
        }

        if (!open) {
            if (tipLines != null) {
                host.renderTooltipLines(tipLines, mouseX, mouseY);
            }
            return;
        }

        if (dirty) {
            refilter();
        }

        bookX = Math.max(MIN_BOOK_LEFT, host.anchorLeft() - PANEL_WIDTH);
        bookY = host.anchorTop();
        if (searchField != null) {
            searchField.xPosition = bookX + 28;
            searchField.yPosition = bookY + 16;
        }

        for (int i = 0; i < 4; i++) {
            int tabY = bookY + 3 + i * 27;
            boolean selected = (i == activeTab);
            int tabX = selected ? (bookX - 30) : (bookX - 28);
            int tabW = selected ? 35 : 30;
            int tabH = 26;

            mc.getTextureManager().bindTexture(NewerTextures.RECIPE_BOOK);
            GL11.glEnable(GL11.GL_BLEND);
            net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(tabX, tabY, selected ? 188 : 153, 2, tabW, tabH);

            RenderHelper.enableGUIStandardItemLighting();
            drawTabIcon(i, font, tabX, tabY);
            RenderHelper.disableStandardItemLighting();

            if (mouseX >= tabX && mouseX < tabX + tabW && mouseY >= tabY && mouseY < tabY + tabH) {
                tipLines = one(StatCollector.translateToLocal(getTabKey(i)));
            }
        }

        mc.getTextureManager().bindTexture(NewerTextures.RECIPE_BOOK);
        GL11.glEnable(GL11.GL_BLEND);
        net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(bookX, bookY, 1, 1, PANEL_WIDTH, PANEL_HEIGHT);

        if (searchField != null) {
            int boxX = bookX + 25;
            int boxY = bookY + 13;
            int boxW = 81;
            int boxH = 14;
            int borderColor = searchField.isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;

            drawRect(boxX, boxY, boxX + boxW, boxY + boxH, borderColor);
            drawRect(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1, 0xFF000000);

            if (searchField.getText().isEmpty() && !searchField.isFocused()) {
                font.drawString("§o" + StatCollector.translateToLocal("newerinv.search"), boxX + 3, boxY + 3, 0x808080);
            } else {
                searchField.drawTextBox();
            }
        }

        int filterX = bookX + 110;
        int filterY = bookY + 14;
        boolean filterHovered = (mouseX >= filterX && mouseX < filterX + 26 && mouseY >= filterY && mouseY < filterY + 16);
        mc.getTextureManager().bindTexture(NewerTextures.RECIPE_BOOK);
        GL11.glEnable(GL11.GL_BLEND);
        net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(filterX, filterY, craftableOnly ? 180 : 152, filterHovered ? 58 : 41, 26, 16);

        if (filterHovered) {
            tipLines = one(StatCollector.translateToLocal(craftableOnly ? "newerinv.filter.craftable" : "newerinv.filter.all"));
        }

        EntityPlayer player = mc.thePlayer;
        int hoveredSlot = -1;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int absIdx = page * PAGE_SIZE + i;
            if (absIdx >= results.size()) {
                break;
            }
            int col = i % COLS;
            int row = i / COLS;
            int slotX = bookX + 11 + col * 25;
            int slotY = bookY + 33 + row * 25;

            GridRecipe r = results.get(absIdx);
            boolean canCraft = (player != null && RecipeIndex.canCraft(r, player.inventory));

            mc.getTextureManager().bindTexture(NewerTextures.RECIPE_BOOK);
            GL11.glEnable(GL11.GL_BLEND);
            net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (!canCraft) {
                drawTexturedModalRect(slotX, slotY, 54, 206, 25, 25);
            } else {
                drawTexturedModalRect(slotX, slotY, 29, 206, 25, 25);
            }

            ItemStack out = r.output;
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), out, slotX + 4, slotY + 4);
            itemRender.renderItemOverlayIntoGUI(font, mc.getTextureManager(), out, slotX + 4, slotY + 4,
                    out.stackSize > 1 ? String.valueOf(out.stackSize) : null);
            RenderHelper.disableStandardItemLighting();

            if (mouseX >= slotX && mouseX < slotX + 25 && mouseY >= slotY && mouseY < slotY + 25) {
                hoveredSlot = absIdx;
            }
        }

        int totalPages = Math.max(1, (results.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int prevX = bookX + 38;
        int prevY = bookY + 137;
        boolean prevHovered = (page > 0 && mouseX >= prevX && mouseX < prevX + 12 && mouseY >= prevY && mouseY < prevY + 17);

        int nextX = bookX + 93;
        int nextY = bookY + 137;
        boolean nextHovered = (page < totalPages - 1 && mouseX >= nextX && mouseX < nextX + 12 && mouseY >= nextY && mouseY < nextY + 17);

        mc.getTextureManager().bindTexture(NewerTextures.RECIPE_BOOK);
        GL11.glEnable(GL11.GL_BLEND);
        net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (page > 0) {
            drawTexturedModalRect(prevX, prevY, 15, prevHovered ? 226 : 208, 12, 17);
        }
        if (page < totalPages - 1) {
            drawTexturedModalRect(nextX, nextY, 1, nextHovered ? 226 : 208, 12, 17);
        }

        if (totalPages > 1) {
            String pageStr = (page + 1) + " / " + totalPages;
            int strW = font.getStringWidth(pageStr);
            font.drawString(pageStr, bookX + 71 - strW / 2, bookY + 141, 0xFFFFFF);
        }

        if (ghostRecipe != null) {
            ghostRecipe.draw(mc, font, itemRender, host.anchorLeft(), host.anchorTop());
            ItemStack ghostTip = ghostRecipe.getHoveredGhostItem(host.anchorLeft(), host.anchorTop(), mouseX, mouseY);
            if (ghostTip != null && tipStack == null && tipLines == null) {
                tipStack = ghostTip;
            }
        }

        if (hoveredSlot >= 0 && hoveredSlot < results.size()) {
            tipStack = results.get(hoveredSlot).output;
        }

        if (tipStack != null) {
            host.renderItemTooltip(tipStack, mouseX, mouseY);
        } else if (tipLines != null) {
            host.renderTooltipLines(tipLines, mouseX, mouseY);
        }
    }

    private void drawTabIcon(int tabIdx, FontRenderer font, int tabX, int tabY) {
        Minecraft mc = Minecraft.getMinecraft();
        switch (tabIdx) {
            case 0:
                itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), new ItemStack(Items.compass), tabX + 9, tabY + 5);
                break;
            case 1:
                itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), new ItemStack(Blocks.brick_block), tabX + 9, tabY + 5);
                break;
            case 2:
                itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), new ItemStack(Items.iron_axe), tabX + 9, tabY + 5);
                break;
            case 3:
                itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), new ItemStack(Items.lava_bucket), tabX + 6, tabY + 5);
                itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), new ItemStack(Items.apple), tabX + 13, tabY + 6);
                break;
        }
    }

    private String getTabKey(int tabIdx) {
        switch (tabIdx) {
            case 0: return "newerinv.tab.all";
            case 1: return "newerinv.tab.blocks";
            case 2: return "newerinv.tab.tools";
            case 3: return "newerinv.tab.misc";
            default: return "newerinv.tab.all";
        }
    }

    private void refilter() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        List<GridRecipe> base = RecipeIndex.query(search, p != null ? p.inventory : null, craftableOnly);

        boolean filter2x2 = false;
        if (!Config.show3x3In2x2Inventory && p != null && p.openContainer != null) {
            GridLayout layout = GridLayout.detect(p.openContainer);
            if (layout != null && layout.gridWidth <= 2) {
                filter2x2 = true;
            }
        }

        List<GridRecipe> f = new ArrayList<GridRecipe>();
        for (GridRecipe r : base) {
            if (filter2x2 && !r.fits2x2()) {
                continue;
            }
            if (activeTab == 0) {
                f.add(r);
            } else {
                Item it = (r.output != null) ? r.output.getItem() : null;
                if (it != null && matchesCategory(activeTab, it.getCreativeTab())) {
                    f.add(r);
                }
            }
        }
        results = f;
        page = Math.min(page, maxPage());
        dirty = false;
    }

    private boolean matchesCategory(int tabIdx, CreativeTabs tab) {
        if (tab == null) {
            return tabIdx == 3;
        }
        switch (tabIdx) {
            case 1:
                return tab == CreativeTabs.tabBlock || tab == CreativeTabs.tabDecorations;
            case 2:
                return tab == CreativeTabs.tabTools || tab == CreativeTabs.tabCombat || tab == CreativeTabs.tabTransport;
            case 3:
                return tab == CreativeTabs.tabMisc || tab == CreativeTabs.tabFood
                        || tab == CreativeTabs.tabRedstone || tab == CreativeTabs.tabMaterials
                        || tab == CreativeTabs.tabBrewing;
            default:
                return true;
        }
    }

    private int maxPage() {
        return Math.max(0, (results.size() - 1) / PAGE_SIZE);
    }

    private boolean inBook(int mx, int my) {
        return mx >= (bookX - 30) && mx < (bookX + PANEL_WIDTH) && my >= bookY && my < (bookY + PANEL_HEIGHT);
    }

    private int slotAt(int mx, int my) {
        int startX = bookX + 11;
        int startY = bookY + 33;
        if (mx < startX || my < startY) {
            return -1;
        }
        int col = (mx - startX) / 25;
        int row = (my - startY) / 25;
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return -1;
        }
        return page * PAGE_SIZE + row * COLS + col;
    }

    private void playClickSound() {
        Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }

    private static List<String> one(String s) {
        return new ArrayList<String>(Arrays.asList(s));
    }

    private static boolean shiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }
}

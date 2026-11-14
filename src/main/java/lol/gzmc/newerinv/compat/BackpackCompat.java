package lol.gzmc.newerinv.compat;

import net.minecraft.client.gui.GuiButton;

import cpw.mods.fml.common.Loader;

/**
 * Optional Backpack (modid {@code Backpack}) integration.
 *
 * <p>Backpack hangs its inventory tabs off {@code GuiInventory} in {@code GuiInventoryTabHandler},
 * and places them at a hardcoded vanilla layout -- {@code (width - 176) / 2} and
 * {@code (height - 166) / 2} -- instead of reading guiLeft/guiTop off the screen it is decorating.
 * It also does that once, in {@code InitGuiEvent.Post}. So the tabs sit where a centered vanilla
 * inventory would be, wherever ours actually renders.
 *
 * <p>Those tabs are ordinary GuiButtons and are not registered with TConstruct's {@code TabRegistry},
 * so nothing else moves them either: our own tab filter matches "tab" in the class name and defers
 * to TabRegistry, which is not necessarily loaded. The fix is to let them through to the generic
 * per-frame offset tracking that already carries every other third-party button.
 *
 * <p>The class is resolved reflectively and gated on the mod actually being loaded, so without
 * Backpack this never matches a button and behaviour is exactly what it was before.
 */
public final class BackpackCompat {

    private BackpackCompat() {}

    private static final String MODID = "Backpack";
    private static final String TAB_CLASS = "de.eydamos.backpack.gui.tab.AbstractInventoryTab";

    private static boolean resolved = false;
    private static Class<?> tabClass = null;

    private static Class<?> tabClass() {
        if (!resolved) {
            resolved = true;
            try {
                if (Loader.isModLoaded(MODID)) {
                    tabClass = Class.forName(TAB_CLASS);
                }
            } catch (Throwable ignored) {
                // Backpack absent, or a fork moved the class - stay a no-op.
            }
        }
        return tabClass;
    }

    /**
     * True only for Backpack's own inventory tabs, and only while Backpack is installed. Both tab
     * types ({@code InventoryTabVanilla}, {@code InventoryTabBackpack}) extend the same base class.
     */
    public static boolean isBackpackTab(GuiButton btn) {
        if (btn == null) {
            return false;
        }
        Class<?> cls = tabClass();
        return cls != null && cls.isInstance(btn);
    }
}

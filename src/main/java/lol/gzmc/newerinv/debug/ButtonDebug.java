package lol.gzmc.newerinv.debug;

import java.util.List;

import lol.gzmc.newerinv.client.BookHost;
import lol.gzmc.newerinv.config.Config;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * Diagnostic dump of a book screen's button list, off unless {@code debugButtons} is set in the
 * config. Prints where every third-party button actually sits next to where a vanilla-centered GUI
 * would have put it, which is what mods that hardcode {@code (width - 176) / 2} assume. A button
 * whose X matches the vanilla column but not our anchor is one nobody is repositioning.
 */
public final class ButtonDebug {

    private ButtonDebug() {}

    public static void dump(String where, GuiScreen gui, BookHost host, List<?> buttons) {
        if (!Config.debugButtons) {
            return;
        }
        int anchorLeft = host.anchorLeft();
        int anchorTop = host.anchorTop();
        int xSize = host.containerWidth();
        int ySize = host.containerHeight();
        int vanillaLeft = (host.screenWidth() - xSize) / 2;
        int vanillaTop = (host.screenHeight() - ySize) / 2;

        FMLLog.info(
                "[NewerInv/debug] %s on %s: screen=%dx%d size=%dx%d anchor=(%d,%d) vanilla=(%d,%d) dx=%d dy=%d",
                where,
                gui.getClass().getName(),
                host.screenWidth(),
                host.screenHeight(),
                xSize,
                ySize,
                anchorLeft,
                anchorTop,
                vanillaLeft,
                vanillaTop,
                anchorLeft - vanillaLeft,
                anchorTop - vanillaTop);

        if (buttons == null || buttons.isEmpty()) {
            FMLLog.info("[NewerInv/debug]   (no buttons)");
            return;
        }
        for (Object obj : buttons) {
            if (!(obj instanceof GuiButton)) {
                continue;
            }
            GuiButton btn = (GuiButton) obj;
            FMLLog.info(
                    "[NewerInv/debug]   %s id=%d pos=(%d,%d) size=%dx%d enabled=%s visible=%s relAnchor=(%d,%d) relVanilla=(%d,%d)",
                    btn.getClass().getName(),
                    btn.id,
                    btn.xPosition,
                    btn.yPosition,
                    btn.width,
                    btn.height,
                    btn.enabled,
                    btn.visible,
                    btn.xPosition - anchorLeft,
                    btn.yPosition - anchorTop,
                    btn.xPosition - vanillaLeft,
                    btn.yPosition - vanillaTop);
        }
    }
}

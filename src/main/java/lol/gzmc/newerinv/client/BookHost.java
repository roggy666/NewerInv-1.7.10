package lol.gzmc.newerinv.client;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;

public interface BookHost {

    int screenWidth();

    int screenHeight();

    int anchorLeft();

    int anchorTop();

    int containerWidth();

    int containerHeight();

    FontRenderer font();

    void renderTooltipLines(List<String> lines, int x, int y);

    void renderItemTooltip(net.minecraft.item.ItemStack stack, int x, int y);

    void updateScreenPosition();
}

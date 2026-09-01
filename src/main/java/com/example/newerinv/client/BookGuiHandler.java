package com.example.newerinv.client;

import com.example.newerinv.config.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

public class BookGuiHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!Config.enabled || event.gui == null) {
            return;
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }

        Class<?> cls = event.gui.getClass();
        if (cls == GuiInventory.class) {
            event.gui = new GuiInventoryBook(player);
        } else if (cls == GuiCrafting.class) {
            event.gui = new GuiCraftingBook(player.inventory, Minecraft.getMinecraft().theWorld);
        }
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        PotionEffectRenderer.disableVanillaPotionEffects(event.gui);
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.gui instanceof GuiContainerCreative) {
            GuiContainerCreative creative = (GuiContainerCreative) event.gui;
            if (creative.func_147056_g() == CreativeTabs.tabInventory.getTabIndex()) {
                PotionEffectRenderer.renderCleanPotionEffects(creative, 0, false);
            }
        }
    }
}

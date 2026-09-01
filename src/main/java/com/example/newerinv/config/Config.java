package com.example.newerinv.config;

import java.io.File;

import com.example.newerinv.NewerInv;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean enabled = true;
    public static boolean openByDefault = true;
    public static boolean craftMaxOnShift = true;
    public static boolean show3x3In2x2Inventory = false;

    private static Configuration cfg;

    public static void load(File file) {
        cfg = new Configuration(file);
        sync();
    }

    public static void sync() {
        if (cfg == null) {
            return;
        }
        enabled = cfg.getBoolean("enabled", "general", enabled,
                "Master switch for the recipe book panel");
        openByDefault = cfg.getBoolean("openByDefault", "general", openByDefault,
                "Panel starts open when a crafting screen is shown");
        craftMaxOnShift = cfg.getBoolean("craftMaxOnShift", "general", craftMaxOnShift,
                "Shift or right click on a recipe fills the grid for as many crafts as possible");
        show3x3In2x2Inventory = cfg.getBoolean("show3x3In2x2Inventory", "general", show3x3In2x2Inventory,
                "Show recipes requiring a 3x3 crafting grid in the 2x2 player inventory recipe book");

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (NewerInv.MODID.equals(event.modID)) {
            sync();
        }
    }
}

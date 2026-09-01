package com.example.newerinv;

import com.example.newerinv.client.BookGuiHandler;
import com.example.newerinv.config.Config;
import com.example.newerinv.recipe.RecipeIndex;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
        modid = NewerInv.MODID,
        name = "NewerInv",
        version = "1.0.0",
        acceptedMinecraftVersions = "[1.7.10]"
)
public class NewerInv {

    public static final String MODID = "newerinv";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
        FMLCommonHandler.instance().bus().register(new Config());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new BookGuiHandler());
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        RecipeIndex.build();
    }
}

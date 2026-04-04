package com.stebars.cantswimupwaterfalls;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(CantSwimUpWaterfallsMod.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CantSwimUpWaterfallsMod {
    public static final String MOD_ID = "cantswimupwaterfalls";

    public CantSwimUpWaterfallsMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }
}
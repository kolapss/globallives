package com.kolaps.globallives;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        LifeRootMod.EtherealPage.setRegistryName(LifeRootMod.MOD_ID, "etherealpage");
        LifeRootMod.EternalScroll.setRegistryName(LifeRootMod.MOD_ID, "eternalscroll");
        event.getRegistry().registerAll(LifeRootMod.EtherealPage, LifeRootMod.EternalScroll);
    }
}

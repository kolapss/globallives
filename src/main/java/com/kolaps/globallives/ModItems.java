package com.kolaps.globallives;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            LifeRootMod.LIFE_ROOT.setRegistryName(LifeRootMod.MOD_ID, "liferoot"),
            LifeRootMod.LIFEBLOOM.setRegistryName(LifeRootMod.MOD_ID, "lifebloom")
        );
    }
}

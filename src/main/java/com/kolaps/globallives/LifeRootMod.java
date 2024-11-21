package com.kolaps.globallives;

import net.minecraft.advancements.Advancement;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod(LifeRootMod.MOD_ID)
public class LifeRootMod {
    public static final String MOD_ID = "globallives";
    public static final Item LIFE_ROOT = new Item(new Item.Properties().tab(ItemGroup.TAB_MISC));
    public static final Item LIFEBLOOM = new Item(new Item.Properties().tab(ItemGroup.TAB_MISC));

    public LifeRootMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            Advancement advancement = event.getAdvancement();
            ResourceLocation id = advancement.getId();

            // Фильтрация: исключаем рецепты
            if (!id.getNamespace().equals("minecraft") || !id.getPath().startsWith("recipes")) {
                // Выдаем предмет за каждое уникальное достижение
                event.getPlayer().addItem(new ItemStack(LIFE_ROOT, 1));
            }
        }
    }
}
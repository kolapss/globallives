package com.kolaps.globallives;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.*;
import net.minecraft.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(LifeRootMod.MOD_ID)
public class LifeRootMod {
    public static final String MOD_ID = "globallives"; // ID мода
    public static final String LIVES_TAG = "GlobalLives"; // Тег для хранения данных о жизнях
    public static final Item EtherealPage = new EtherealPageItem(new Item.Properties().tab(ItemGroup.TAB_MISC));
    public static final Item EternalScroll = new EternalScrollItem(new Item.Properties().tab(ItemGroup.TAB_MISC));
    public static final ManageLives lives = new ManageLives();

    public LifeRootMod() {
        // Регистрация событий
        MinecraftForge.EVENT_BUS.register(this);
        NetworkHandler.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new KeyHandler();
        }

    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            Advancement advancement = event.getAdvancement();
            ResourceLocation id = advancement.getId();

            // Фильтрация: исключаем рецепты
            if (!id.getNamespace().equals("minecraft") || !id.getPath().startsWith("recipes")) {
                // Выдаем предмет за каждое уникальное достижение
                event.getPlayer().addItem(new ItemStack(EtherealPage, 1));
            }
        }
    }

}

package com.kolaps.globallives;


import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.command.Commands;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LifeRootMod.MOD_ID)
public class LifeRootMod {
    public static final String MOD_ID = "globallives"; // ID мода
    public static final String LIVES_TAG = "GlobalLives"; // Тег для хранения данных о жизнях
    private static final int INITIAL_LIVES = 5; // Количество жизней по умолчанию
    public static final Item EtherealPage = new EtherealPageItem(new Item.Properties().tab(ItemGroup.TAB_MISC));
    public static final Item EternalScroll = new EternalScrollItem(new Item.Properties().tab(ItemGroup.TAB_MISC));

    public LifeRootMod() {
        // Регистрация событий
        MinecraftForge.EVENT_BUS.register(this);

    }


    public static void updatePlayerLives(PlayerEntity player, int lives) {
        if (!player.level.isClientSide) {
            // Получение Scoreboard
            Scoreboard scoreboard = player.getScoreboard();

            // Создание или получение Objectives для жизней
            ScoreObjective livesObjective = scoreboard.getObjective(LIVES_TAG);
            if (livesObjective == null) {
                livesObjective = scoreboard.addObjective(
                        LIVES_TAG,
                        ScoreCriteria.DUMMY,
                        new StringTextComponent("Global Lives"),
                        ScoreCriteria.RenderType.INTEGER);
            }

            // Установка значения
            Score score = scoreboard.getOrCreatePlayerScore(player.getName().getString(), livesObjective);
            score.setScore(lives);
        }
    }

    public static int getPlayerLivesFromScoreboard(PlayerEntity player) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreObjective livesObjective = scoreboard.getObjective(LIVES_TAG);

        if (livesObjective != null) {
            Score score = scoreboard.getOrCreatePlayerScore(player.getName().getString(), livesObjective);
            return score.getScore();
        }

        return INITIAL_LIVES; // Возвращаем начальное значение, если Objective отсутствует
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();

        if (!player.level.isClientSide) {
            CompoundNBT data = player.getPersistentData();
            CompoundNBT persistentData = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

            // Если данных о жизнях нет, задаём значение по умолчанию
            int lives = persistentData.contains(LIVES_TAG) ? persistentData.getInt(LIVES_TAG) : INITIAL_LIVES;
            persistentData.putInt(LIVES_TAG, lives);

            // Обновляем Scoreboard
            updatePlayerLives(player, lives);
            player.sendMessage(new StringTextComponent("You have enter to onplayerJoin"), player.getUUID());
            // Сохранение данных обратно
            data.put(PlayerEntity.PERSISTED_NBT_TAG, persistentData);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        PlayerEntity player = event.getPlayer();

        if (!player.level.isClientSide) { // Работаем только на стороне сервера
            CompoundNBT data = player.getPersistentData();
            CompoundNBT persistentData = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

            int lives = persistentData.contains(LIVES_TAG) ? persistentData.getInt(LIVES_TAG) : INITIAL_LIVES - 1;
            lives = Math.max(0, lives - 1);
            persistentData.putInt(LIVES_TAG, lives);

            // Обновляем Scoreboard
            updatePlayerLives(player, lives);

            data.put(PlayerEntity.PERSISTED_NBT_TAG, persistentData); // Сохраняем данные обратно
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

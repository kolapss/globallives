package com.kolaps.globallives;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.entity.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ManageLives {
    private static final int INITIAL_LIVES = 3;
    public static final String LIVES_TAG = "GlobalLives";

    public ManageLives() {
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
            int lives = score.getScore();
            if(lives > 0)
            {
                return lives;
            }
            else
            {
                return -1;
            }
        }

        return -1; // Возвращаем начальное значение, если Objective отсутствует
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        MinecraftServer server = serverPlayer.server;

        if (!player.level.isClientSide) {

            // Если данных о жизнях нет, задаём значение по умолчанию
            // int lives = getPlayerLivesFromScoreboard(player)==-1 ?
            // ServerEvents.executeCommandOnServer(server,"scoreboard objectives setdisplay
            // list ad.Lives") : INITIAL_LIVES;
            if (getPlayerLivesFromScoreboard(player) == -1) {
                updatePlayerLives(player, INITIAL_LIVES);
                ServerEvents.executeCommandOnServer(server, "scoreboard objectives setdisplay list " + LIVES_TAG);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            MinecraftServer server = serverPlayer.server;
            if (!player.level.isClientSide) { // Работаем только на стороне сервера
                int lives = getPlayerLivesFromScoreboard(player);
                if (lives > 1) {
                    updatePlayerLives(player, lives - 1);
                } else {
                    ServerEvents.executeCommandOnServer(server,
                            "gamemode spectator " + serverPlayer.getName().getString());
                }
            }
        }

    }
}

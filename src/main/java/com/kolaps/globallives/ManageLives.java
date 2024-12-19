package com.kolaps.globallives;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ManageLives {
    private static final int INITIAL_LIVES = 3;
    public static final String LIVES_TAG = "GlobalLives";

    public ManageLives() {
        // Регистрация событий
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void setPlayerLives(PlayerEntity player, int lives) {
        if (!player.level.isClientSide) {
            CompoundNBT persistentData = player.getPersistentData();
            CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

            data.putInt(LIVES_TAG, lives);
            persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);
            if (player instanceof ServerPlayerEntity) {
                syncPlayerLives((ServerPlayerEntity) player);
            }
        }
    }

    public static int getPlayerLives(PlayerEntity player) {
        CompoundNBT persistentData = player.getPersistentData();
        CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

        if (data.contains(LIVES_TAG)) {
            int lives = data.getInt(LIVES_TAG);
            if(lives>0)
            {
                return lives;
            }
            else
            {
                return 0;
            }
        }

        return -1; // Возвращаем -1, если данных нет
    }

    public static void syncPlayerLives(ServerPlayerEntity player) {
        int lives = getPlayerLives(player);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncLivesPacket(lives));
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();

        if (!player.level.isClientSide) {
            int lives = getPlayerLives(player);
            if (lives == -1) {
                setPlayerLives(player, INITIAL_LIVES);
                player.displayClientMessage(new StringTextComponent("Lives initialized to " + INITIAL_LIVES), true);
                return;
            }
            setPlayerLives(player, lives);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            MinecraftServer server = serverPlayer.server;

            if (!player.level.isClientSide) { // Работаем только на серверной стороне
                int lives = getPlayerLives(player);
                if (lives > 1) {
                    setPlayerLives(player, lives - 1);
                } else {
                    setPlayerLives(player, 0);
                    server.getCommands().performCommand(server.createCommandSourceStack(), 
                            "gamemode spectator " + serverPlayer.getName().getString());
                }
            }
        }
    }
}

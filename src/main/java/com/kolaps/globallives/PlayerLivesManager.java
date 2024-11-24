package com.kolaps.globallives;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLivesManager {

    private static final String LIVES_TAG = "lives";
    private static final int INITIAL_LIVES = 5;

    // Событие: вход игрока на сервер
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            CompoundNBT persistentData = player.getPersistentData();
            CompoundNBT playerData = persistentData.getCompound(ServerPlayerEntity.PERSISTED_NBT_TAG);

            if (!playerData.contains(LIVES_TAG)) { // Если данных о жизнях нет, установить начальные жизни
                playerData.putInt(LIVES_TAG, INITIAL_LIVES);
                persistentData.put(ServerPlayerEntity.PERSISTED_NBT_TAG, playerData);
            }

            int lives = playerData.getInt(LIVES_TAG);
            player.sendMessage(new StringTextComponent("Your number of lives: " + lives), player.getUUID());
        }
    }

    // Событие: смерть игрока
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            CompoundNBT persistentData = player.getPersistentData();
            CompoundNBT playerData = persistentData.getCompound(ServerPlayerEntity.PERSISTED_NBT_TAG);

            int lives = playerData.getInt(LIVES_TAG);
            if (lives > 0) {
                playerData.putInt(LIVES_TAG, lives - 1);
                persistentData.put(ServerPlayerEntity.PERSISTED_NBT_TAG, playerData);
                player.sendMessage(new StringTextComponent("You have lost a life. Remaining:" + (lives - 1)), player.getUUID());
            } else {
                player.sendMessage(new StringTextComponent("You have no more lives!"), player.getUUID());
                // Здесь можно добавить логику для полного окончания игры
            }
        }
    }
}

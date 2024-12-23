package com.kolaps.globallives;

import java.util.Map;
import java.util.HashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ManageLives {
    private static final int INITIAL_LIVES = 1;
    public static final String LIVES_TAG = "GlobalLives";
    private final Map<ServerPlayerEntity, ServerPlayerEntity> spectatorMap = new HashMap<>();

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
            if (lives > 0) {
                return lives;
            } else {
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
            // Восстанавливаем информацию о привязке камеры
            CompoundNBT persistentData = player.getPersistentData();
            CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

            if (data.contains("SpectatorTarget")) {
                String targetUUID = data.getString("SpectatorTarget");
                ServerPlayerEntity targetPlayer = player.getServer().getPlayerList().getPlayers().stream()
                        .filter(p -> p.getUUID().toString().equals(targetUUID))
                        .findFirst()
                        .orElse(null);
                        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                if (targetPlayer != null) {
                    serverPlayer.setGameMode(GameType.SPECTATOR);
                    serverPlayer.setCamera(targetPlayer);
                    spectatorMap.put(serverPlayer, targetPlayer);
                    player.sendMessage(
                            new StringTextComponent("You are watching for " + targetPlayer.getName().getString()),
                            player.getUUID());
                } else {
                    player.sendMessage(new StringTextComponent("The target player is no longer available."), player.getUUID());
                }
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

            if (!player.level.isClientSide) { // Работаем только на серверной стороне
                int lives = getPlayerLives(player);
                if (lives > 1) {
                    setPlayerLives(player, lives - 1);
                } else {
                    setPlayerLives(player, 0);
                    serverPlayer.setGameMode(GameType.SPECTATOR);
                    serverPlayer.sendMessage(new StringTextComponent("You have switched to spectator mode."),
                            serverPlayer.getUUID());
                    ServerPlayerEntity targetPlayer = serverPlayer.getServer().getPlayerList().getPlayers().stream()
                            .filter(playerr -> !playerr.isSpectator() && !playerr.isDeadOrDying()
                                    && playerr != serverPlayer)
                            .findFirst()
                            .orElse(null);
                    if (targetPlayer != null) {
                        serverPlayer.setCamera(targetPlayer); // Привязываем камеру к другому игроку
                        spectatorMap.put(serverPlayer, targetPlayer); // Сохраняем связь

                        // Сохраняем в NBT
                        CompoundNBT persistentData = serverPlayer.getPersistentData();
                        CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
                        data.putString("SpectatorTarget", targetPlayer.getUUID().toString());
                        persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

                        serverPlayer.sendMessage(
                                new StringTextComponent("You are watching for " + targetPlayer.getName().getString()),
                                serverPlayer.getUUID());
                    } else {
                        serverPlayer.sendMessage(new StringTextComponent("There are no live players to watch."),
                                serverPlayer.getUUID());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity deadPlayer = (ServerPlayerEntity) player;
            if (spectatorMap.containsKey(deadPlayer)) {
                ServerPlayerEntity targetPlayer = spectatorMap.get(deadPlayer);

                // Проверяем, что камера осталась на целевом игроке
                if (deadPlayer.getCamera() != targetPlayer) {
                    deadPlayer.setCamera(targetPlayer);
                }

                // Если цель умерла, сбрасываем наблюдение
                if (targetPlayer.isDeadOrDying() || targetPlayer.isSpectator()) {
                    spectatorMap.remove(deadPlayer);

                    // Удаляем привязку из NBT
                    CompoundNBT persistentData = deadPlayer.getPersistentData();
                    CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
                    data.remove("SpectatorTarget");
                    persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

                    deadPlayer.sendMessage(new StringTextComponent("The player you were watching died."),
                            deadPlayer.getUUID());
                }
            }
        }
    }
}

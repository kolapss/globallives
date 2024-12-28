package com.kolaps.globallives;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraft.block.NetherPortalBlock;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ManageLives {
    private static final int INITIAL_LIVES = 1;
    public static final String LIVES_TAG = "GlobalLives";
    private static final int DELAY_TICKS = 20; // Задержка на 1 секунду (20 тиков)
    private int tickCounter = 0;
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

    public void fixFrozenPlayers(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        for (ServerPlayerEntity frozenPlayer : server.getPlayerList().getPlayers()) {
            CompoundNBT persistentData = frozenPlayer.getPersistentData();
            CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
            if (data.getBoolean("IsFrozen")) {
                // Телепортируем замороженного игрока к новому живому игроку
                frozenPlayer.teleportTo(player.getX(), player.getY(), player.getZ());
                frozenPlayer.setCamera(player);
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                spectatorMap.put(frozenPlayer, serverPlayer); // Обновляем связь

                // Обновляем NBT
                data.putBoolean("IsFrozen", false);
                data.putString("SpectatorTarget", player.getUUID().toString());
                persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

                frozenPlayer.sendMessage(new StringTextComponent(
                        "A new player has joined. You are now watching " + player.getName().getString()
                                + "."),
                        frozenPlayer.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();

        if (!player.level.isClientSide) {
            int lives = getPlayerLives(player);
            if (lives == -1) {
                setPlayerLives(player, INITIAL_LIVES);
                fixFrozenPlayers((ServerPlayerEntity) player);
                player.displayClientMessage(new StringTextComponent("Lives initialized to " + INITIAL_LIVES), true);
                return;
            }
            setPlayerLives(player, lives);

            if (lives > 0) {
                fixFrozenPlayers((ServerPlayerEntity) player);
            }
            if (lives == 0) {
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
                        player.sendMessage(new StringTextComponent("The target player is no longer available."),
                                player.getUUID());
                    }
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
                player.inventory.dropAll();
                BlockPos serverPlayerPosition = serverPlayer.blockPosition();
                int lives = getPlayerLives(player);
                if (lives > 1) {
                    setPlayerLives(player, lives - 1);
                } else {
                    setPlayerLives(player, 0);
                    serverPlayer.setGameMode(GameType.SPECTATOR);
                    serverPlayer.sendMessage(new StringTextComponent("You have switched to spectator mode."),
                            serverPlayer.getUUID());
                    List<ServerPlayerEntity> livePlayers = server.getPlayerList().getPlayers().stream()
                            .filter(playerr -> !playerr.isSpectator() && !playerr.isDeadOrDying()
                                    && playerr != serverPlayer)
                            .collect(Collectors.toList());
                    Map<ServerPlayerEntity, ServerPlayerEntity> updates = new HashMap<>();
                    for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : spectatorMap.entrySet()) {
                        updates.put(entry.getKey(), entry.getValue());
                    }
                    if (livePlayers.isEmpty()) {
                        frozePlayer(serverPlayer);
                        // Если цель умерла, сбрасываем наблюдение
                        for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : updates.entrySet()) {
                            ServerPlayerEntity curDeadPlayer = entry.getKey();
                            if (entry.getValue() == serverPlayer) {
                                curDeadPlayer.teleportTo(serverPlayerPosition.getX(), serverPlayerPosition.getY(),
                                        serverPlayerPosition.getZ());
                                unbindPlayer(curDeadPlayer);
                            }
                        }
                    } else {
                        // Если к нам кто-то привязан, то перепревязываем их.
                        ServerPlayerEntity targetPlayer = livePlayers.get(0);
                        for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : updates.entrySet()) {
                            ServerPlayerEntity curDeadPlayer = entry.getKey();
                            if (entry.getValue() == serverPlayer) {
                                curDeadPlayer.setCamera(targetPlayer); // Привязываем камеру к другому игроку
                                spectatorMap.remove(curDeadPlayer);
                                spectatorMap.put(curDeadPlayer, targetPlayer); // Сохраняем связь

                                // Сохраняем в NBT
                                CompoundNBT persistentData = curDeadPlayer.getPersistentData();
                                CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
                                data.putString("SpectatorTarget", targetPlayer.getUUID().toString());
                                persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

                                curDeadPlayer.sendMessage(
                                        new StringTextComponent(
                                                "You are watching " + targetPlayer.getName().getString() + "."),
                                        curDeadPlayer.getUUID());
                            }
                        }
                        // Есть другие живые игроки
                        serverPlayer.setCamera(targetPlayer); // Привязываем камеру к другому игроку
                        spectatorMap.put(serverPlayer, targetPlayer); // Сохраняем связь

                        // Сохраняем в NBT
                        CompoundNBT persistentData = serverPlayer.getPersistentData();
                        CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
                        data.putString("SpectatorTarget", targetPlayer.getUUID().toString());
                        persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

                        serverPlayer.sendMessage(
                                new StringTextComponent(
                                        "You are watching " + targetPlayer.getName().getString() + "."),
                                serverPlayer.getUUID());
                    }
                }
            }
        }
    }

    public void unbindPlayer(ServerPlayerEntity curDeadPlayer) {
        spectatorMap.remove(curDeadPlayer);

        // Удаляем привязку из NBT
        CompoundNBT persistentData = curDeadPlayer.getPersistentData();
        CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        data.remove("SpectatorTarget");
        persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

        curDeadPlayer.sendMessage(new StringTextComponent("The player you were watching died."),
                curDeadPlayer.getUUID());
        frozePlayer(curDeadPlayer);
    }

    public void frozePlayer(ServerPlayerEntity serverPlayer) {
        // Нет других живых игроков, игрок становится "замороженным"
        // spectatorMap.put(serverPlayer, null); // Сохраняем, что игрок заморожен

        // Сохраняем состояние в NBT
        CompoundNBT persistentData = serverPlayer.getPersistentData();
        CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        data.putBoolean("IsFrozen", true);
        BlockPos playerPosition = serverPlayer.blockPosition();
        data.putInt("x", playerPosition.getX());
        data.putInt("y", playerPosition.getY());
        data.putInt("z", playerPosition.getZ());
        persistentData.put(PlayerEntity.PERSISTED_NBT_TAG, data);

        serverPlayer.sendMessage(
                new StringTextComponent("You are frozen until another player joins."),
                serverPlayer.getUUID());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        // Получаем игрока, который вышел
        ServerPlayerEntity leftPlayer = (ServerPlayerEntity) event.getPlayer();
        BlockPos serverPlayerPosition = leftPlayer.blockPosition();
        if (!spectatorMap.isEmpty()) {
            Map<ServerPlayerEntity, ServerPlayerEntity> updates = new HashMap<>();
            for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : spectatorMap.entrySet()) {
                updates.put(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : updates.entrySet()) {
                ServerPlayerEntity curDeadPlayer = entry.getKey();
                if (entry.getValue() == leftPlayer) {
                    curDeadPlayer.teleportTo(serverPlayerPosition.getX(), serverPlayerPosition.getY(),
                            serverPlayerPosition.getZ());
                    unbindPlayer(curDeadPlayer);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ServerPlayerEntity changedPlayer = (ServerPlayerEntity) event.getPlayer();

        if (!spectatorMap.isEmpty()) {
            Map<ServerPlayerEntity, ServerPlayerEntity> updates = new HashMap<>();
            MinecraftServer server = changedPlayer.getServer();
            for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : spectatorMap.entrySet()) {
                updates.put(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<ServerPlayerEntity, ServerPlayerEntity> entry : updates.entrySet()) {
                ServerPlayerEntity deadPlayer = entry.getKey();
                CompoundNBT persistentData = deadPlayer.getPersistentData();
                CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
                String deadUUID = data.getString("SpectatorTarget");

                if (deadUUID.trim().equals(changedPlayer.getUUID().toString().trim())) {
                    System.out.println("tp " + deadPlayer.getName() + " " + changedPlayer.getName());
                    spectatorMap.remove(deadPlayer);
                    ServerEvents.executeCommandOnServer(server,
                            "tp " + deadPlayer.getName().getString() + " " + changedPlayer.getName().getString());
                    ServerPlayerEntity updatedDeadPlayer = server.getPlayerList().getPlayer(deadPlayer.getUUID());
                    spectatorMap.put(updatedDeadPlayer, changedPlayer);

                    tickCounter=0;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity deadPlayer = (ServerPlayerEntity) player;
            CompoundNBT persistentData = deadPlayer.getPersistentData();
            CompoundNBT data = persistentData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
            if (spectatorMap.containsKey(deadPlayer)) {
                ServerPlayerEntity targetPlayer = spectatorMap.get(deadPlayer);
                if (tickCounter < DELAY_TICKS) {
                    tickCounter++;
                    return;  // Возвращаемся до окончания задержки
                }
                if (deadPlayer.getCamera() != targetPlayer) {
                    deadPlayer.setCamera(targetPlayer);
                }

            }
            if (data.getBoolean("IsFrozen")) {
                BlockPos playerPosition = deadPlayer.blockPosition();
                int x = data.getInt("x");
                int y = data.getInt("y");
                int z = data.getInt("z");
                if (x == playerPosition.getX() && y == playerPosition.getY() && z == playerPosition.getZ()) {

                } else {
                    deadPlayer.teleportTo(x, y, z);
                }
            }
        }
    }
}

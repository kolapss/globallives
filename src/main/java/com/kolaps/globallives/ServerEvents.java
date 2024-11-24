package com.kolaps.globallives;

import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID)
public class ServerEvents {
    
    // Обработчик события запуска сервера
    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer(); // Получаем сервер
        executeCommandOnServer(server, "scoreboard objectives setdisplay list GlobalLives"); // Выполняем команду
    }

    // Функция для выполнения команды
    public static void executeCommandOnServer(MinecraftServer server, String command) {
        // Получаем Overworld (основной мир)
        ServerWorld overworld = server.getLevel(World.OVERWORLD);

        if (overworld != null) {
            overworld.getServer().getCommands().performCommand(
                    overworld.getServer().createCommandSourceStack(),
                    command);
        }
    }
}

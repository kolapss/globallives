package com.kolaps.globallives;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID)
public class PlayerInteractionHandler {

    @SubscribeEvent
    public static void onRightClickWithLifebloom(PlayerInteractEvent.RightClickItem event) {
        // Ensure the event is server-side and the player is a ServerPlayerEntity
        if (!event.getWorld().isClientSide && event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            ItemStack heldItem = event.getItemStack();

            // Check if the player is holding the Lifebloom item
            if (heldItem.getItem() == LifeRootMod.EternalScroll) {
                // Decrease the Lifebloom count by 1
                heldItem.shrink(1);

                // Execute the command from the server context
                ServerWorld serverWorld = (ServerWorld) player.level;
                serverWorld.getServer().getCommands().performCommand(
                    serverWorld.getServer().createCommandSourceStack(),
                    "scoreboard players remove " + player.getName().getString() + " ad.boolDeath 1"
                );

                // Cancel further processing of the event
                event.setCanceled(true);
            }
        }
    }
}
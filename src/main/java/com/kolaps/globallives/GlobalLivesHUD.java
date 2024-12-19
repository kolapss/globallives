package com.kolaps.globallives;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, value = Dist.CLIENT)
public class GlobalLivesHUD {
    private static int currentLives = 0;

    public static void updateLivesDisplay(int lives) {
        currentLives = lives;
        showLives();
    }

    private static boolean showLives = false;
    private static long displayEndTime = 0;

    public static void showLives() {
        showLives = true;
        displayEndTime = System.currentTimeMillis() + 3000; // Показываем 3 секунды
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (showLives && Minecraft.getInstance().player != null) {
            if (System.currentTimeMillis() > displayEndTime) {
                showLives = false;
                return;
            }

            MatrixStack matrixStack = event.getMatrixStack();
            Minecraft mc = Minecraft.getInstance();

            // Рисуем текст на экране
            String message = "Lives: " + currentLives;
            AbstractGui.drawString(matrixStack, mc.font, message, 10, 10, 0xFFFFFF);
        }
    }
}

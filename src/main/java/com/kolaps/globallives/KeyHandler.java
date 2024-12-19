package com.kolaps.globallives;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LifeRootMod.MOD_ID, value = Dist.CLIENT)
public class KeyHandler {

    private static final String CATEGORY = "key.categories.globallives";
    private static final String KEY_SHOW_LIVES = "key.globallives.show_lives";

    public static final KeyBinding showLivesKey = new KeyBinding(KEY_SHOW_LIVES, GLFW.GLFW_KEY_L, CATEGORY);

    public KeyHandler() {
        // Регистрируем клавишу
        ClientRegistry.registerKeyBinding(showLivesKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        // Проверяем, нажата ли наша клавиша
        if (showLivesKey.isDown()) {
            GlobalLivesHUD.showLives();
        }
    }
}

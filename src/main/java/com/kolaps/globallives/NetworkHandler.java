package com.kolaps.globallives;

import com.kolaps.globallives.LifeRootMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LifeRootMod.MOD_ID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int packetId = 0;

        CHANNEL.registerMessage(packetId++,
                SyncLivesPacket.class,
                SyncLivesPacket::encode,
                SyncLivesPacket::decode,
                SyncLivesPacket::handle
        );
    }
}

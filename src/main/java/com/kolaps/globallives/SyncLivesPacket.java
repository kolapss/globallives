package com.kolaps.globallives;

import com.kolaps.globallives.ManageLives;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public class SyncLivesPacket {
    private final int lives;

    public SyncLivesPacket(int lives) {
        this.lives = lives;
    }

    // Десериализация пакета
    public static SyncLivesPacket decode(PacketBuffer buffer) {
        return new SyncLivesPacket(buffer.readInt());
    }

    // Сериализация пакета
    public static void encode(SyncLivesPacket packet, PacketBuffer buffer) {
        buffer.writeInt(packet.lives);
    }

    // Обработка пакета
    public static void handle(SyncLivesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                PlayerEntity player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    GlobalLivesHUD.updateLivesDisplay(packet.lives);
                }
            });
        }
        context.setPacketHandled(true);
    }

    public int getLives() {
        return lives;
    }
}

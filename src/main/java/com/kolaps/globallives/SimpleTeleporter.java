package com.kolaps.globallives;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

public class SimpleTeleporter implements ITeleporter {
    private final Vector3d position;

    public SimpleTeleporter(Vector3d position) {
        this.position = position;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerWorld currentWorld, ServerWorld destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        // Используем teleportTo для перемещения сущности
        entity.teleportTo(position.x, position.y, position.z);
        return entity;
    }
}
package com.troubledev.waves;

import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3d;

/**
 * Quando uma tropa chega ao fim da lane, troca o caminho dela por um vai-e-vem curto,
 * de um lado para o outro (perpendicular à lane), em volta do destino.
 * Sem isso ela voltaria pela lane inteira (o formato "Line" do caminho vai e volta).
 */
public class CreepLaneSystem extends EntityTickingSystem<EntityStore> {

    // Distância (na horizontal) do destino para considerar que a tropa chegou
    private static final double ARRIVE_DISTANCE = 4.0;
    // Metade da largura do vai-e-vem no destino
    private static final double PATROL_HALF_WIDTH = 6.0;

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(NPCEntity.getComponentType(), CreepLaneComponent.getComponentType());
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        var lane = chunk.getComponent(index, CreepLaneComponent.getComponentType());
        if (lane.hasArrived()) return;

        var transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;

        var pos = transform.getPosition();
        var dest = lane.getDestination();
        var dx = dest.x() - pos.x();
        var dz = dest.z() - pos.z();
        if (dx * dx + dz * dz > ARRIVE_DISTANCE * ARRIVE_DISTANCE) return;

        var npc = chunk.getComponent(index, NPCEntity.getComponentType());
        npc.getPathManager().setTransientPath(buildPatrolPath(lane.getOrigin(), dest));
        lane.setArrived(true);
    }

    /** Dois pontos ao lado do destino, perpendiculares à direção da lane. */
    private static TransientPath buildPatrolPath(Vector3d origin, Vector3d dest) {
        var dirX = dest.x() - origin.x();
        var dirZ = dest.z() - origin.z();
        var length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length < 0.001) { dirX = 1; dirZ = 0; length = 1; }

        // Perpendicular no plano horizontal
        var perpX = -dirZ / length * PATROL_HALF_WIDTH;
        var perpZ = dirX / length * PATROL_HALF_WIDTH;

        var path = new TransientPath();
        path.addWaypoint(new Vector3d(dest.x() + perpX, dest.y(), dest.z() + perpZ), new Rotation3f());
        path.addWaypoint(new Vector3d(dest.x() - perpX, dest.y(), dest.z() - perpZ), new Rotation3f());
        return path;
    }
}

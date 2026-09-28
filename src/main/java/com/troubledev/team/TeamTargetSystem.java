package com.troubledev.team;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Rede de segurança contra "aggro" entre aliados: se um NPC com time marcar como alvo
 * alguém do próprio time (ex.: depois de levar uma flecha aliada), o alvo é descartado.
 * Sem alvo, a tropa sai do combate e volta para a lane.
 */
public class TeamTargetSystem extends EntityTickingSystem<EntityStore> {

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(
                NPCEntity.getComponentType(),
                TeamComponent.getComponentType(),
                MarkedEntitySupport.getComponentType()
        );
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
        var team = chunk.getComponent(index, TeamComponent.getComponentType()).getTeam();
        var marked = chunk.getComponent(index, MarkedEntitySupport.getComponentType());

        for (int slot = 0; slot < marked.getMarkedEntitySlotCount(); slot++) {
            var target = marked.getMarkedEntityRef(slot);
            if (target == null || !target.isValid()) continue;

            var targetTeam = store.getComponent(target, TeamComponent.getComponentType());
            if (targetTeam != null && targetTeam.getTeam() == team) {
                marked.clearMarkedEntity(slot);
            }
        }
    }
}

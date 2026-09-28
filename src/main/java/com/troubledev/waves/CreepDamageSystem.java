package com.troubledev.waves;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.troubledev.team.TeamComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Todo golpe dado por uma tropa (NPC com time) causa CreepStats.DAMAGE,
 * independente da arma ou do tipo do NPC. Roda no grupo de filtro de dano,
 * antes do dano ser aplicado.
 */
public class CreepDamageSystem extends DamageEventSystem {

    @NullableDecl
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    @Override
    public void handle(
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
            @NonNullDecl Damage damage
    ) {
        // Inclui projéteis (ProjectileSource estende EntitySource)
        if (!(damage.getSource() instanceof Damage.EntitySource source)) return;

        var attacker = source.getRef();
        if (attacker == null || !attacker.isValid()) return;

        // Só tropas: NPC que pertence a um time. Jogadores continuam com o dano normal.
        if (store.getComponent(attacker, NPCEntity.getComponentType()) == null) return;
        if (store.getComponent(attacker, TeamComponent.getComponentType()) == null) return;

        damage.setAmount(CreepStats.DAMAGE);
    }
}

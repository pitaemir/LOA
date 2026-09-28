package com.troubledev.team;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Bloqueia friendly fire: dano entre duas entidades do mesmo time é cancelado.
 * Vale para jogador -> tropa aliada, tropa -> jogador aliado e tropa -> tropa aliada.
 */
public class TeamDamageSystem extends DamageEventSystem {

    @NullableDecl
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        // Só entidades com time podem ser protegidas
        return Archetype.of(TeamComponent.getComponentType());
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

        var attackerTeam = store.getComponent(attacker, TeamComponent.getComponentType());
        if (attackerTeam == null) return;

        var targetTeam = chunk.getComponent(index, TeamComponent.getComponentType());
        if (targetTeam == null) return;

        if (attackerTeam.getTeam() == targetTeam.getTeam()) {
            damage.setCancelled(true);
        }
    }
}

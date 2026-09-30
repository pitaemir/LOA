package com.troubledev.team;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Bloqueia friendly fire: dano entre duas entidades do mesmo time é cancelado.
 * Vale para jogador -> tropa aliada, tropa -> jogador aliado e tropa -> tropa aliada.
 * Flechas de arqueiros que batem num aliado têm o dano redirecionado para o alvo inimigo do arqueiro.
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

        if (attackerTeam.getTeam() != targetTeam.getTeam()) return;

        damage.setCancelled(true);

        // Flecha de tropa que bateu num aliado: a física do jogo não deixa a flecha atravessar,
        // então o dano vai para o inimigo que o arqueiro estava mirando (o tiro não é desperdiçado).
        if (source instanceof Damage.ProjectileSource projectile
                && store.getComponent(attacker, NPCEntity.getComponentType()) != null) {
            var enemy = findEnemyTarget(store, attacker, attackerTeam.getTeam());
            if (enemy != null) {
                var redirected = new Damage(
                        new Damage.ProjectileSource(attacker, projectile.getProjectile()),
                        damage.getCause(),
                        damage.getAmount()
                );
                DamageSystems.executeDamage(enemy, commandBuffer, redirected);
            }
        }
    }

    /** O inimigo que a tropa tem marcado como alvo, ou null. */
    private static Ref<EntityStore> findEnemyTarget(Store<EntityStore> store, Ref<EntityStore> npc, Team team) {
        var marked = store.getComponent(npc, MarkedEntitySupport.getComponentType());
        if (marked == null) return null;
        for (int slot = 0; slot < marked.getMarkedEntitySlotCount(); slot++) {
            var target = marked.getMarkedEntityRef(slot);
            if (target == null || !target.isValid()) continue;
            var targetTeam = store.getComponent(target, TeamComponent.getComponentType());
            if (targetTeam != null && targetTeam.getTeam() != team) return target;
        }
        return null;
    }
}

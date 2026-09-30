package com.troubledev.waves;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.troubledev.team.TeamComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Golpes dados por uma tropa (NPC com time):
 *  - só causam dano no alvo que a tropa escolheu (sem dano em área);
 *  - causam sempre CreepStats.DAMAGE, independente da arma ou do tipo do NPC.
 * Roda no grupo de filtro de dano, antes do dano ser aplicado.
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

    private static boolean isMarkedTarget(Store<EntityStore> store, Ref<EntityStore> attacker, Ref<EntityStore> target) {
        var marked = store.getComponent(attacker, MarkedEntitySupport.getComponentType());
        if (marked == null) return true; // sem informação de alvo: não bloqueia
        for (int slot = 0; slot < marked.getMarkedEntitySlotCount(); slot++) {
            if (target.equals(marked.getMarkedEntityRef(slot))) return true;
        }
        return false;
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

        // Alvo único: o golpe (ou flecha) de uma tropa só causa dano em quem ela escolheu como alvo.
        // Evita dano em área de golpes que acertam várias entidades e flechas perdidas.
        if (!isMarkedTarget(store, attacker, chunk.getReferenceTo(index))) {
            damage.setCancelled(true);
            return;
        }

        damage.setAmount(CreepStats.DAMAGE);
    }
}

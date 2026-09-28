package com.troubledev.team;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.StoreSystem;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.Blackboard;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.AttitudeView;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Faz os NPCs decidirem amigo/inimigo pelo TeamComponent:
 * mesmo time -> FRIENDLY, times diferentes -> HOSTILE.
 *
 * Se alguma das duas entidades não tem time, retorna null e o jogo
 * usa as regras normais de atitude (arquivos Server/NPC/Attitude).
 */
public class TeamAttitudeSystem extends StoreSystem<EntityStore> {

    // Menor número = maior prioridade. A reputação do jogo usa 100; 0 é reservado para overrides.
    private static final int PRIORITY = 10;

    @Override
    public void onSystemAddedToStore(@NonNullDecl Store<EntityStore> store) {
        var blackboard = store.getResource(Blackboard.getResourceType());
        var attitudeView = blackboard.getView(AttitudeView.class, 0L);

        attitudeView.registerProvider(PRIORITY, (npcRef, roleIndex, targetRef, accessor) -> {
            var npcTeam = accessor.getComponent(npcRef, TeamComponent.getComponentType());
            if (npcTeam == null) return null;

            var targetTeam = accessor.getComponent(targetRef, TeamComponent.getComponentType());
            if (targetTeam == null) return null;

            return npcTeam.getTeam() == targetTeam.getTeam() ? Attitude.FRIENDLY : Attitude.HOSTILE;
        });
    }

    @Override
    public void onSystemRemovedFromStore(@NonNullDecl Store<EntityStore> store) {
    }
}

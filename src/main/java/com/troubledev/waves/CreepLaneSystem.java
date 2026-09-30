package com.troubledev.waves;

import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.troubledev.team.TeamComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3d;

/**
 * Controla o caminho de cada tropa na lane:
 *  - Ao sair de um combate, dá a ela um caminho novo que começa onde ela está.
 *    O movimento de caminho do jogo lembra o nó para onde ia antes da luta e voltaria até ele.
 *  - A cada segundo, se ela está sem alvo, procura a tropa inimiga mais próxima e ataca.
 *  - A cada segundo, se ela está fora de combate e quase não andou (presa em outra tropa),
 *    refaz o caminho; se continuar presa, contorna com um desvio para o lado.
 *  - Ao chegar ao fim da lane, troca o caminho por um vai-e-vem curto, de um lado para o outro
 *    (perpendicular à lane). Sem isso ela voltaria pela lane inteira (o formato "Line" vai e volta).
 */
public class CreepLaneSystem extends EntityTickingSystem<EntityStore> {

    // Distância (na horizontal) do destino para considerar que a tropa chegou
    private static final double ARRIVE_DISTANCE = 4.0;
    // Metade da largura do vai-e-vem no destino
    private static final double PATROL_HALF_WIDTH = 6.0;
    // A cada STUCK_CHECK_INTERVAL segundos, uma tropa fora de combate que andou menos que
    // STUCK_MIN_DISTANCE está presa: refaz o caminho (e, se continuar presa, desvia para o lado)
    private static final float STUCK_CHECK_INTERVAL = 1f;
    private static final double STUCK_MIN_DISTANCE = 0.5;
    private static final double DETOUR_SIDE = 1.2;
    // A cada AGGRO_CHECK_INTERVAL segundos, uma tropa sem alvo procura a tropa inimiga mais próxima
    // até AGGRO_RANGE blocos e entra em combate com ela (não fica parada enquanto a luta acontece ao lado)
    private static final float AGGRO_CHECK_INTERVAL = 1f;
    private static final double AGGRO_RANGE = 12.0;

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(
                NPCEntity.getComponentType(),
                CreepLaneComponent.getComponentType(),
                TeamComponent.getComponentType(),
                MarkedEntitySupport.getComponentType(),
                WorldSupport.getComponentType()
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
        var lane = chunk.getComponent(index, CreepLaneComponent.getComponentType());
        var transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;

        var npc = chunk.getComponent(index, NPCEntity.getComponentType());
        var worldSupport = chunk.getComponent(index, WorldSupport.getComponentType());
        var pos = transform.getPosition();

        var inCombat = hasEnemyTarget(chunk, index, store);

        // Sem alvo: de tempos em tempos procura um inimigo por perto e ataca
        if (!inCombat && checkAggro(lane, chunk, index, store, pos, dt)) {
            inCombat = true;
        }

        // Saiu do combate agora: recomeça o caminho a partir da posição atual
        if (lane.isInCombat() && !inCombat && !lane.hasArrived()) {
            assignPath(npc, worldSupport, WaveManager.buildPath(pos, lane.getDestination()));
            resetStuckCheck(lane, pos);
        }
        lane.setInCombat(inCombat);

        if (lane.hasArrived()) return;

        var dest = lane.getDestination();
        var dx = dest.x() - pos.x();
        var dz = dest.z() - pos.z();
        if (dx * dx + dz * dz <= ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
            assignPath(npc, worldSupport, buildPatrolPath(lane.getOrigin(), dest));
            lane.setArrived(true);
            return;
        }

        if (!inCombat) checkStuck(lane, npc, worldSupport, pos, dt);
    }

    /** A cada segundo: se a tropa quase não andou, refaz o caminho; da 2ª vez seguida em diante, com desvio lateral. */
    private static void checkStuck(CreepLaneComponent lane, NPCEntity npc, WorldSupport worldSupport, Vector3d pos, float dt) {
        if (lane.getLastCheckPosition() == null) {
            resetStuckCheck(lane, pos);
            return;
        }

        lane.setCheckTimer(lane.getCheckTimer() + dt);
        if (lane.getCheckTimer() < STUCK_CHECK_INTERVAL) return;
        lane.setCheckTimer(0f);

        var last = lane.getLastCheckPosition();
        var mx = pos.x() - last.x();
        var mz = pos.z() - last.z();
        lane.setLastCheckPosition(pos);

        if (mx * mx + mz * mz >= STUCK_MIN_DISTANCE * STUCK_MIN_DISTANCE) {
            lane.setStuckChecks(0);
            return;
        }

        var stuck = lane.getStuckChecks() + 1;
        lane.setStuckChecks(stuck);
        if (stuck == 1) {
            assignPath(npc, worldSupport, WaveManager.buildPath(pos, lane.getDestination()));
        } else {
            // Alterna o lado do desvio a cada tentativa
            var side = (stuck % 2 == 0) ? DETOUR_SIDE : -DETOUR_SIDE;
            assignPath(npc, worldSupport, WaveManager.buildDetourPath(pos, lane.getDestination(), side));
        }
    }

    private static void resetStuckCheck(CreepLaneComponent lane, Vector3d pos) {
        lane.setLastCheckPosition(pos);
        lane.setCheckTimer(0f);
        lane.setStuckChecks(0);
    }

    /**
     * Troca o caminho da tropa e avisa o sensor de caminho (o mesmo que a ação "ResetPath" do template):
     * o sensor guarda o caminho antigo e, sem o aviso, o movimento ficaria dividido entre os dois.
     */
    private static void assignPath(NPCEntity npc, WorldSupport worldSupport, TransientPath path) {
        npc.getPathManager().setTransientPath(path);
        worldSupport.requestNewPath();
    }

    /** A cada segundo: procura a tropa inimiga mais próxima dentro de AGGRO_RANGE; se achar, trava nela e entra em Attack. */
    private static boolean checkAggro(CreepLaneComponent lane, ArchetypeChunk<EntityStore> chunk, int index,
                                      Store<EntityStore> store, Vector3d pos, float dt) {
        lane.setAggroTimer(lane.getAggroTimer() + dt);
        if (lane.getAggroTimer() < AGGRO_CHECK_INTERVAL) return false;
        lane.setAggroTimer(0f);

        var team = chunk.getComponent(index, TeamComponent.getComponentType()).getTeam();
        Ref<EntityStore> nearest = null;
        var nearestDistSq = AGGRO_RANGE * AGGRO_RANGE;

        for (var other : WaveManager.liveTroops()) {
            var otherTeam = store.getComponent(other, TeamComponent.getComponentType());
            if (otherTeam == null || otherTeam.getTeam() == team) continue;
            var otherTransform = store.getComponent(other, TransformComponent.getComponentType());
            if (otherTransform == null) continue;
            var distSq = otherTransform.getPosition().distanceSquared(pos);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = other;
            }
        }
        if (nearest == null) return false;

        var self = chunk.getReferenceTo(index);
        chunk.getComponent(index, MarkedEntitySupport.getComponentType())
                .setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, nearest);
        var state = store.getComponent(self, StateSupport.getComponentType());
        if (state != null) state.setState(self, "Attack", null, store);
        return true;
    }

    /** A tropa está com algum inimigo marcado como alvo (= em combate)? */
    private static boolean hasEnemyTarget(ArchetypeChunk<EntityStore> chunk, int index, Store<EntityStore> store) {
        var team = chunk.getComponent(index, TeamComponent.getComponentType()).getTeam();
        var marked = chunk.getComponent(index, MarkedEntitySupport.getComponentType());

        for (int slot = 0; slot < marked.getMarkedEntitySlotCount(); slot++) {
            var target = marked.getMarkedEntityRef(slot);
            if (target == null || !target.isValid()) continue;
            var targetTeam = store.getComponent(target, TeamComponent.getComponentType());
            if (targetTeam != null && targetTeam.getTeam() != team) return true;
        }
        return false;
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

        // Na patrulha, olhando para frente (sentido em que a tropa vinha na lane)
        var forward = WaveManager.facing(origin, dest);
        var path = new TransientPath();
        path.addWaypoint(new Vector3d(dest.x() + perpX, dest.y(), dest.z() + perpZ), new Rotation3f(forward));
        path.addWaypoint(new Vector3d(dest.x() - perpX, dest.y(), dest.z() - perpZ), new Rotation3f(forward));
        return path;
    }
}

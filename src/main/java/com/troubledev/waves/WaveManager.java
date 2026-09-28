package com.troubledev.waves;

import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.troubledev.team.Team;
import com.troubledev.team.TeamComponent;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Teste de ondas nos dois sentidos da lane:
 *  - inimigos (time RED) nascem no ponto de spawn e andam até a base;
 *  - aliados  (time BLUE) nascem na base e andam até o ponto de spawn.
 * Eles se encontram no meio e lutam (ver TeamAttitudeSystem).
 *
 * Estado só em memória (some ao reiniciar o servidor) — é um protótipo.
 */
public final class WaveManager {

    // Tropas do próprio mod (Server/NPC/Roles/LOA): mesmo Kweebec guerreiro, cor de cada time.
    // Outros roles "_Patrol" do jogo também funcionam, porque seguem o caminho atribuído (Follow_Path).
    public static final String DEFAULT_ENEMY_TYPE = "Creep_Melee_Red";
    public static final String DEFAULT_ALLY_TYPE = "Creep_Melee_Green";
    public static final String ENEMY_RANGED_TYPE = "Creep_Ranged_Red";
    public static final String ALLY_RANGED_TYPE = "Creep_Ranged_Green";
    public static final int DEFAULT_COUNT = 3;
    public static final int DEFAULT_RANGED_COUNT = 2;
    // Os arqueiros nascem esse tanto atrás dos corpo a corpo
    private static final double RANGED_SPAWN_BEHIND = 3.0;
    public static final int DEFAULT_INTERVAL_SECONDS = 60;

    // Distância entre waypoints intermediários (o sensor de caminho procura num raio de 30 blocos)
    private static final double WAYPOINT_SPACING = 8.0;

    private static Vector3d spawnPoint;
    private static Vector3d basePoint;
    private static World world;
    private static ScheduledFuture<?> task;
    private static WaveSettings settings = WaveSettings.defaults();

    /**
     * O que cada onda spawna, por lado: count corpo a corpo + rangedCount arqueiros.
     * enemyType/allyType null = esse lado não spawna.
     */
    public record WaveSettings(int count, int rangedCount, String enemyType, String allyType) {
        public static WaveSettings defaults() {
            return new WaveSettings(DEFAULT_COUNT, DEFAULT_RANGED_COUNT, DEFAULT_ENEMY_TYPE, DEFAULT_ALLY_TYPE);
        }
    }

    private WaveManager() {}

    public static void setSpawnPoint(World w, Vector3d pos) {
        world = w;
        spawnPoint = new Vector3d(pos);
    }

    public static void setBasePoint(World w, Vector3d pos) {
        world = w;
        basePoint = new Vector3d(pos);
    }

    public static Vector3d getSpawnPoint() { return spawnPoint; }
    public static Vector3d getBasePoint() { return basePoint; }
    public static boolean isRunning() { return task != null; }
    public static boolean isConfigured() { return spawnPoint != null && basePoint != null && world != null; }

    public static void start(int intervalSeconds, WaveSettings waveSettings) {
        stop();
        settings = waveSettings;
        task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    // ECS só pode ser mexido na thread do mundo
                    if (world != null && world.isAlive()) world.execute(() -> spawnWave(settings));
                },
                0, intervalSeconds, TimeUnit.SECONDS
        );
    }

    public static void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    /** Spawna uma onda agora. Precisa rodar na thread do mundo. Retorna quantos nasceram. */
    public static int spawnWave(WaveSettings waveSettings) {
        if (!isConfigured()) return 0;

        var spawned = 0;
        if (waveSettings.enemyType() != null) {
            spawned += spawnGroup(waveSettings.enemyType(), waveSettings.count(), Team.RED, spawnPoint, basePoint, 0);
            spawned += spawnGroup(ENEMY_RANGED_TYPE, waveSettings.rangedCount(), Team.RED, spawnPoint, basePoint, RANGED_SPAWN_BEHIND);
        }
        if (waveSettings.allyType() != null) {
            spawned += spawnGroup(waveSettings.allyType(), waveSettings.count(), Team.BLUE, basePoint, spawnPoint, 0);
            spawned += spawnGroup(ALLY_RANGED_TYPE, waveSettings.rangedCount(), Team.BLUE, basePoint, spawnPoint, RANGED_SPAWN_BEHIND);
        }
        return spawned;
    }

    /** behind = quantos blocos atrás do ponto de saída (no sentido contrário da lane) nascer. */
    private static int spawnGroup(String npcType, int count, Team team, Vector3d from, Vector3d to, double behind) {
        var store = world.getEntityStore().getStore();
        var random = ThreadLocalRandom.current();
        var spawned = 0;

        var back = new Vector3d(from).sub(to);
        back.y = 0;
        if (back.lengthSquared() > 0.001) back.normalize(behind);
        else back.zero();

        for (int i = 0; i < count; i++) {
            // Espalha um pouco pra não nascerem todos no mesmo bloco
            var pos = new Vector3d(
                    from.x() + back.x() + random.nextDouble() * 2 - 1,
                    from.y(),
                    from.z() + back.z() + random.nextDouble() * 2 - 1
            );

            var result = NPCPlugin.get().spawnNPC(store, npcType, null, pos, new Rotation3f());
            if (result == null || result.first() == null) continue;

            var ref = result.first();
            store.addComponent(ref, TeamComponent.getComponentType(), new TeamComponent(team));
            CreepStats.applyHealth(store, ref);
            store.addComponent(ref, CreepLaneComponent.getComponentType(), new CreepLaneComponent(from, to));

            var npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc != null) npc.getPathManager().setTransientPath(buildPath(pos, to));
            spawned++;
        }

        return spawned;
    }

    /**
     * Remove todas as tropas (NPCs com time) do mundo. team null = dos dois times.
     * Jogadores e NPCs sem time não são tocados. Precisa rodar na thread do mundo.
     */
    public static int clearTroops(Store<EntityStore> store, Team team) {
        var troops = new ArrayList<Ref<EntityStore>>();
        var query = Archetype.of(NPCEntity.getComponentType(), TeamComponent.getComponentType());

        // Junta primeiro e remove depois: não dá pra remover entidades enquanto percorre os chunks
        store.forEachChunk(query, (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                var troopTeam = chunk.getComponent(i, TeamComponent.getComponentType());
                if (team == null || troopTeam.getTeam() == team) troops.add(chunk.getReferenceTo(i));
            }
        });

        var removed = 0;
        for (var ref : troops) {
            if (!ref.isValid()) continue;
            store.removeEntity(ref, RemoveReason.REMOVE);
            removed++;
        }
        return removed;
    }

    /** Caminho em linha reta de from até to, com waypoints a cada WAYPOINT_SPACING blocos. */
    private static TransientPath buildPath(Vector3d from, Vector3d to) {
        var path = new TransientPath();
        var distance = from.distance(to);
        var steps = Math.max(1, (int) Math.ceil(distance / WAYPOINT_SPACING));

        for (int i = 0; i <= steps; i++) {
            var t = (double) i / steps;
            var point = new Vector3d(from).lerp(to, t);
            path.addWaypoint(point, new Rotation3f());
        }
        return path;
    }
}

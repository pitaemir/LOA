package com.troubledev.waves;

import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.joml.Vector3d;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Teste de ondas: a cada X segundos, spawna N inimigos no ponto de spawn
 * e faz eles andarem até a base seguindo um caminho de waypoints.
 *
 * Estado só em memória (some ao reiniciar o servidor) — é um protótipo.
 */
public final class WaveManager {

    // Roles "_Patrol" têm o comportamento Follow_Path, que segue o caminho atribuído
    public static final String DEFAULT_NPC_TYPE = "Trork_Warrior_Patrol";
    public static final int DEFAULT_COUNT = 3;
    public static final int DEFAULT_INTERVAL_SECONDS = 60;

    // Distância entre waypoints intermediários (o sensor de caminho procura num raio de 30 blocos)
    private static final double WAYPOINT_SPACING = 8.0;

    private static Vector3d spawnPoint;
    private static Vector3d basePoint;
    private static World world;
    private static ScheduledFuture<?> task;
    private static String npcType = DEFAULT_NPC_TYPE;
    private static int count = DEFAULT_COUNT;

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

    public static void start(int intervalSeconds, String type, int amount) {
        stop();
        npcType = type;
        count = amount;
        task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    // ECS só pode ser mexido na thread do mundo
                    if (world != null && world.isAlive()) world.execute(WaveManager::spawnWave);
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
    public static int spawnWave() {
        if (!isConfigured()) return 0;

        var store = world.getEntityStore().getStore();
        var random = ThreadLocalRandom.current();
        var spawned = 0;

        for (int i = 0; i < count; i++) {
            // Espalha um pouco pra não nascerem todos no mesmo bloco
            var pos = new Vector3d(
                    spawnPoint.x() + random.nextDouble() * 2 - 1,
                    spawnPoint.y(),
                    spawnPoint.z() + random.nextDouble() * 2 - 1
            );

            var result = NPCPlugin.get().spawnNPC(store, npcType, null, pos, new Rotation3f());
            if (result == null || result.first() == null) continue;

            var npc = store.getComponent(result.first(), NPCEntity.getComponentType());
            if (npc != null) npc.getPathManager().setTransientPath(buildPath(pos, basePoint));
            spawned++;
        }

        return spawned;
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

package com.troubledev.waves;

import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.troubledev.team.Team;
import com.troubledev.team.TeamComponent;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
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
    // Distância lateral entre tropas da mesma fileira ao nascer
    private static final double SPAWN_SPACING = 1.5;
    public static final int DEFAULT_INTERVAL_SECONDS = 60;

    // Distância entre waypoints. Pequena de propósito: depois de uma luta a tropa retoma do nó
    // mais próximo, e com nós espaçados esse nó podia ficar vários blocos para trás.
    private static final double WAYPOINT_SPACING = 2.0;

    // Pontos da lane de teste do MobaMap. /loa wave setspawn e setbase substituem até o próximo reinício.
    public static final Vector3d DEFAULT_SPAWN_POINT = new Vector3d(2, 80, -153);
    public static final Vector3d DEFAULT_BASE_POINT = new Vector3d(1, 80, 23);

    // Tropas das ondas (para a busca de alvo do CreepLaneSystem). Refs inválidas são limpas na hora de usar.
    private static final Set<Ref<EntityStore>> troops = ConcurrentHashMap.newKeySet();

    private static Vector3d spawnPoint = new Vector3d(DEFAULT_SPAWN_POINT);
    private static Vector3d basePoint = new Vector3d(DEFAULT_BASE_POINT);
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

    /** As ondas acontecem no mundo de quem deu o comando (ex.: a cópia do MobaMap em que você está). */
    public static void useWorld(World w) {
        world = w;
    }

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

    /**
     * Spawna uma fileira de tropas em formação: lado a lado (perpendicular à lane),
     * SPAWN_SPACING blocos entre cada uma, "behind" blocos atrás do ponto de saída.
     * Cada tropa anda numa linha paralela às outras até o fim da lane.
     * Formação fixa em vez de posição aleatória: evita tropas nascendo uma dentro da outra.
     */
    private static int spawnGroup(String npcType, int count, Team team, Vector3d from, Vector3d to, double behind) {
        var store = world.getEntityStore().getStore();
        var spawned = 0;

        // Direção da lane (horizontal) e a perpendicular a ela
        var forward = new Vector3d(to).sub(from);
        forward.y = 0;
        if (forward.lengthSquared() < 0.001) forward.set(0, 0, 1);
        forward.normalize();
        var side = new Vector3d(-forward.z, 0, forward.x);

        for (int i = 0; i < count; i++) {
            var lateral = (i - (count - 1) / 2.0) * SPAWN_SPACING;
            var pos = new Vector3d(from)
                    .fma(-behind, forward)
                    .fma(lateral, side);

            // Cada tropa tem a sua própria linha na lane: o destino dela tem o mesmo deslocamento
            // lateral de onde nasceu. A formação se mantém até o fim sem precisar da separação
            // (que empurra as tropas e fazia elas travarem).
            var laneFrom = new Vector3d(from).fma(lateral, side);
            var laneTo = new Vector3d(to).fma(lateral, side);

            var result = NPCPlugin.get().spawnNPC(store, npcType, null, pos, spawnRotation(from, to));
            if (result == null || result.first() == null) continue;

            var ref = result.first();
            store.addComponent(ref, TeamComponent.getComponentType(), new TeamComponent(team));
            CreepStats.applyHealth(store, ref);
            CreepStats.disableRegen(store, ref);
            CreepStats.disableBodyBlock(store, ref);
            store.addComponent(ref, CreepLaneComponent.getComponentType(), new CreepLaneComponent(laneFrom, laneTo));
            troops.add(ref);

            var npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc != null) npc.getPathManager().setTransientPath(buildPath(pos, laneTo));
            spawned++;
        }

        return spawned;
    }

    /** Tropas vivas das ondas (remove as que já morreram ou sumiram). */
    public static Iterable<Ref<EntityStore>> liveTroops() {
        troops.removeIf(ref -> !ref.isValid());
        return troops;
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
    /** Teste de depuração: para onde as tropas olham ao nascer (/loa wave facing). */
    public enum SpawnFacing { FORWARD, BACKWARD, ZERO }

    private static SpawnFacing spawnFacing = SpawnFacing.BACKWARD; // testado: nascer de costas evita tropas presas no spawn

    public static void setSpawnFacing(SpawnFacing facing) { spawnFacing = facing; }
    public static SpawnFacing getSpawnFacing() { return spawnFacing; }

    private static Rotation3f spawnRotation(Vector3d from, Vector3d to) {
        return switch (spawnFacing) {
            case FORWARD -> facing(from, to);
            case BACKWARD -> facing(to, from);
            case ZERO -> new Rotation3f(); // o que era antes: yaw 0 (-Z) para os dois times
        };
    }

    /**
     * Rotação olhando de "from" para "to" (no plano horizontal).
     * No Hytale o ângulo (yaw) 0 aponta para -Z; PhysicsMath faz a conversão direção <-> ângulo.
     */
    static Rotation3f facing(Vector3d from, Vector3d to) {
        var rotation = new Rotation3f();
        var dx = to.x() - from.x();
        var dz = to.z() - from.z();
        if (dx * dx + dz * dz > 0.0001) rotation.addYaw(PhysicsMath.headingFromDirection(dx, dz));
        return rotation;
    }

    /**
     * Caminho que primeiro passa por um ponto de desvio (ao lado e um pouco à frente)
     * e depois segue até o destino. Usado para contornar uma tropa que está bloqueando.
     */
    static TransientPath buildDetourPath(Vector3d from, Vector3d to, double sideOffset) {
        var forward = new Vector3d(to).sub(from);
        forward.y = 0;
        if (forward.lengthSquared() < 0.001) return buildPath(from, to);
        forward.normalize();
        var side = new Vector3d(-forward.z, 0, forward.x);

        var detour = new Vector3d(from).fma(sideOffset, side).fma(1.5, forward);
        var path = new TransientPath();
        path.addWaypoint(new Vector3d(from), facing(from, detour));
        addLine(path, detour, to);
        return path;
    }

    static TransientPath buildPath(Vector3d from, Vector3d to) {
        var path = new TransientPath();
        addLine(path, from, to);
        return path;
    }

    /** Pontos em linha reta de from até to, a cada WAYPOINT_SPACING blocos, todos olhando no sentido da linha. */
    private static void addLine(TransientPath path, Vector3d from, Vector3d to) {
        var forward = facing(from, to);
        var distance = from.distance(to);
        var steps = Math.max(1, (int) Math.ceil(distance / WAYPOINT_SPACING));

        for (int i = 0; i <= steps; i++) {
            var t = (double) i / steps;
            var point = new Vector3d(from).lerp(to, t);
            path.addWaypoint(point, new Rotation3f(forward));
        }
    }

}

package com.troubledev.testing;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Ao entrar no servidor, leva o jogador para uma cópia do mapa de teste (MobaMap).
 * A mesma cópia é reaproveitada enquanto ela existir; o MobaMap está configurado para
 * apagar a cópia quando ela fica vazia (DeleteOnRemove + WorldEmpty), então nada acumula.
 */
public final class AutoTestMap {

    public static final String MAP_NAME = "MobaMap";
    // Espera o jogador terminar de entrar antes de teleportar
    private static final long JOIN_DELAY_SECONDS = 2;

    private static boolean enabled = true;
    private static CompletableFuture<World> currentCopy;
    // Quem já foi levado nesta sessão do servidor: voltar ao mundo principal depois não te manda de novo
    private static final Set<UUID> alreadySent = ConcurrentHashMap.newKeySet();

    private AutoTestMap() {}

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    /**
     * Chamado quando um jogador entra num mundo. Só age no mundo principal (não em instâncias)
     * e só na primeira entrada do jogador desde que o servidor ligou.
     */
    public static void onPlayerJoined(World world, Ref<EntityStore> playerRef, UUID playerUuid) {
        if (!enabled || isInstance(world)) return;
        if (!alreadySent.add(playerUuid)) return;

        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> world.execute(() -> sendToTestMap(world, playerRef)),
                JOIN_DELAY_SECONDS, TimeUnit.SECONDS
        );
    }

    private static void sendToTestMap(World world, Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) return;
        var store = world.getEntityStore().getStore();

        // Reaproveita a cópia atual se ela ainda existe; senão cria uma nova
        var copy = currentCopy;
        if (copy == null || (copy.isDone() && (copy.isCompletedExceptionally() || !copy.join().isAlive()))) {
            var transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            var returnPoint = transform == null
                    ? null
                    : new Transform(new Vector3d(transform.getPosition()), new Rotation3f(transform.getRotation()));
            copy = InstancesPlugin.get().spawnInstance(MAP_NAME, world, returnPoint);
            currentCopy = copy;
        }

        InstancesPlugin.teleportPlayerToLoadingInstance(playerRef, store, copy, null);
    }

    /** Cópias ("instance-MobaMap-...") e o modo de edição ("instance-edit-...") são instâncias. */
    private static boolean isInstance(World world) {
        return world.getName().startsWith("instance-");
    }
}

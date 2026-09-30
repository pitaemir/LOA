package com.troubledev.waves;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3d;

/**
 * De onde a tropa saiu e para onde ela vai na lane.
 * Quando chega ao destino (arrived), ela passa a patrulhar ali (ver CreepLaneSystem).
 *
 * Não é salvo em disco: tropas que sobram de antes de um reinício voltam ao vai-e-vem normal.
 */
public class CreepLaneComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, CreepLaneComponent> TYPE;

    public static void setComponentType(ComponentType<EntityStore, CreepLaneComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, CreepLaneComponent> getComponentType() {
        return TYPE;
    }

    private Vector3d origin = new Vector3d();
    private Vector3d destination = new Vector3d();
    private boolean arrived = false;
    private boolean inCombat = false;
    // Verificação de "preso" (CreepLaneSystem): posição na última checagem, tempo desde ela, checagens seguidas sem andar
    private Vector3d lastCheckPosition = null;
    private float checkTimer = 0f;
    private int stuckChecks = 0;
    private float aggroTimer = 0f;

    public CreepLaneComponent() {
    }

    public CreepLaneComponent(Vector3d origin, Vector3d destination) {
        this.origin = new Vector3d(origin);
        this.destination = new Vector3d(destination);
    }

    public Vector3d getOrigin() { return origin; }
    public Vector3d getDestination() { return destination; }
    public boolean hasArrived() { return arrived; }
    public void setArrived(boolean arrived) { this.arrived = arrived; }
    public boolean isInCombat() { return inCombat; }
    public void setInCombat(boolean inCombat) { this.inCombat = inCombat; }
    public Vector3d getLastCheckPosition() { return lastCheckPosition; }
    public void setLastCheckPosition(Vector3d position) { this.lastCheckPosition = new Vector3d(position); }
    public float getCheckTimer() { return checkTimer; }
    public void setCheckTimer(float checkTimer) { this.checkTimer = checkTimer; }
    public int getStuckChecks() { return stuckChecks; }
    public void setStuckChecks(int stuckChecks) { this.stuckChecks = stuckChecks; }
    public float getAggroTimer() { return aggroTimer; }
    public void setAggroTimer(float aggroTimer) { this.aggroTimer = aggroTimer; }

    @NullableDecl
    @Override
    public CreepLaneComponent clone() {
        var copy = new CreepLaneComponent(origin, destination);
        copy.arrived = arrived;
        copy.inCombat = inCombat;
        return copy;
    }
}

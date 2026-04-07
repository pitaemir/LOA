package com.troubledev.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.level.WeaponMasteryTable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.HashMap;
import java.util.Map;

public class WeaponMasteryComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, WeaponMasteryComponent> TYPE;

    public static void setComponentType(ComponentType<EntityStore, WeaponMasteryComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, WeaponMasteryComponent> getComponentType() {
        return TYPE;
    }

    // Serializado como "weaponId1:xp1,weaponId2:xp2"
    public static final BuilderCodec<WeaponMasteryComponent> CODEC = BuilderCodec
            .builder(WeaponMasteryComponent.class, WeaponMasteryComponent::new)
            .append(
                    new KeyedCodec<>("MasteryData", Codec.STRING),
                    (component, value) -> component.masteryXpByWeapon = deserialize(value),
                    component -> serialize(component.masteryXpByWeapon)
            ).add()
            .build();

    private Map<String, Long> masteryXpByWeapon = new HashMap<>();

    private static String serialize(Map<String, Long> map) {
        if (map.isEmpty()) return "";
        var sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append(':').append(v).append(','));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static Map<String, Long> deserialize(String data) {
        var map = new HashMap<String, Long>();
        if (data == null || data.isEmpty()) return map;
        for (var entry : data.split(",")) {
            var parts = entry.split(":", 2);
            if (parts.length == 2) map.put(parts[0], Long.parseLong(parts[1]));
        }
        return map;
    }

    public WeaponMasteryComponent() {}

    public int getMasteryLevel(String weaponId) {
        return WeaponMasteryTable.getLevelForXP(masteryXpByWeapon.getOrDefault(weaponId, 0L));
    }

    public long getMasteryXP(String weaponId) {
        return masteryXpByWeapon.getOrDefault(weaponId, 0L);
    }

    public float getMasteryProgress(String weaponId) {
        return WeaponMasteryTable.getProgressToNextLevel(masteryXpByWeapon.getOrDefault(weaponId, 0L));
    }

    /** Adiciona XP de maestria a uma arma. Retorna true se subiu de nível. */
    public boolean addMasteryXP(String weaponId, long amount) {
        if (amount <= 0) return false;
        int oldLevel = getMasteryLevel(weaponId);
        masteryXpByWeapon.merge(weaponId, amount, Long::sum);
        return getMasteryLevel(weaponId) > oldLevel;
    }

    @NullableDecl
    @Override
    public WeaponMasteryComponent clone() {
        var copy = new WeaponMasteryComponent();
        copy.masteryXpByWeapon = new HashMap<>(this.masteryXpByWeapon);
        return copy;
    }
}

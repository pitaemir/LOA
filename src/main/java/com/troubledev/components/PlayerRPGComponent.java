package com.troubledev.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.level.XPTable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class PlayerRPGComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerRPGComponent> TYPE;

    public static void setComponentType(ComponentType<EntityStore, PlayerRPGComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, PlayerRPGComponent> getComponentType() {
        return TYPE;
    }

    public static final BuilderCodec<PlayerRPGComponent> CODEC = BuilderCodec
            .builder(PlayerRPGComponent.class, PlayerRPGComponent::new)
            .append(
                    new KeyedCodec<>("TotalExperience", Codec.LONG),
                    (component, value) -> component.totalExperience = value,
                    component -> component.totalExperience
            ).add()
            .build();

    private long totalExperience = 0;

    public PlayerRPGComponent() {
    }

    public PlayerRPGComponent(long totalExperience) {
        this.totalExperience = Math.max(0L, totalExperience);
    }

    public long getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(long xp) {
        this.totalExperience = Math.max(0L, xp);
    }

    public int getLevel() {
        return XPTable.getLevelForXP(totalExperience);
    }

    public long getCurrentLevelXP() {
        return XPTable.getXPInCurrentLevel(totalExperience);
    }

    public long getXPToNextLevel() {
        return XPTable.getXPToNextLevel(totalExperience);
    }

    public float getProgress() {
        return XPTable.getProgressToNextLevel(totalExperience);
    }

    public boolean isMaxLevel() {
        return getLevel() >= XPTable.MAX_LEVEL;
    }

    public boolean addExperience(long amount) {
        if (amount <= 0) return false;

        int oldLevel = getLevel();
        totalExperience += amount;
        int newLevel = getLevel();

        return newLevel > oldLevel;
    }

    @NullableDecl
    @Override
    public PlayerRPGComponent clone() {
        return new PlayerRPGComponent(this.totalExperience);
    }

    @Override
    public String toString() {
        return "PlayerRPGComponent{level=" + getLevel() +
                ", totalXP=" + totalExperience +
                ", toNext=" + getXPToNextLevel() + "}";
    }
}

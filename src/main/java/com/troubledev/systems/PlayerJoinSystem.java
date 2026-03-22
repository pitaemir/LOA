package com.troubledev.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.components.PlayerLOAComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.troubledev.ui.LOAXPHud;

public class PlayerJoinSystem extends RefSystem<EntityStore> {

    @Override
    public void onEntityAdded(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl AddReason addReason,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        if (addReason != AddReason.LOAD) return;

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        var player = store.getComponent(ref, Player.getComponentType());
        var loaType = PlayerLOAComponent.getComponentType();
        var loa = store.getComponent(ref, loaType);

        if (loa != null) {
            playerRef.sendMessage(Message.raw(
                    "Welcome back! Level %d (%d XP)".formatted(loa.getLevel(), loa.getTotalExperience())
            ));
        } else {
            commandBuffer.addComponent(ref, loaType, new PlayerLOAComponent());
            playerRef.sendMessage(Message.raw("Welcome! Your adventure begins at Level 1."));
        }

        if (player != null) {
            var hud = new LOAXPHud(playerRef, loa);  // ← um único HUD, passando loa
            player.getHudManager().setCustomHud(playerRef, hud);
            hud.show();
        }
    }

    @Override
    public void onEntityRemove(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl RemoveReason removeReason,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
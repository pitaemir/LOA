package com.troubledev.handlers;

import com.troubledev.components.PlayerLOAComponent;
import com.troubledev.events.GiveXPEvent;
import com.troubledev.events.LevelUpEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.troubledev.ui.LOAXPHud;

import java.util.function.Consumer;

public class GiveXPHandler implements Consumer<GiveXPEvent> {

    @Override
    public void accept(GiveXPEvent event) {
        if (!event.playerRef().isValid()) return;

        var store = event.playerRef().getStore();
        var loa = store.getComponent(event.playerRef(), PlayerLOAComponent.getComponentType());
        if (loa == null) return;

        var oldLevel = loa.getLevel();
        var leveledUp = loa.addExperience(event.amount());
        var player = store.getComponent(event.playerRef(), Player.getComponentType());

        //Atualiza a HUD depois de ganhar XP
        if (player != null) {
            var rawHud = player.getHudManager().getCustomHud();
            if (rawHud instanceof LOAXPHud hud) {
                hud.refresh(loa);
            }
        }

        if (leveledUp) {
            LevelUpEvent.dispatch(event.playerRef(), oldLevel, loa.getLevel());
        }
    }
}

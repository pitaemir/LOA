package com.troubledev.handlers;

import com.troubledev.components.PlayerRPGComponent;
import com.troubledev.events.GiveXPEvent;
import com.troubledev.events.LevelUpEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.troubledev.ui.RpgXPHud;

import java.util.function.Consumer;

public class GiveXPHandler implements Consumer<GiveXPEvent> {

    @Override
    public void accept(GiveXPEvent event) {
        if (!event.playerRef().isValid()) return;

        var store = event.playerRef().getStore();
        var rpg = store.getComponent(event.playerRef(), PlayerRPGComponent.getComponentType());
        if (rpg == null) return;

        var oldLevel = rpg.getLevel();
        var leveledUp = rpg.addExperience(event.amount());
        var player = store.getComponent(event.playerRef(), Player.getComponentType());

        // ✅ Atualiza a HUD depois de ganhar XP
        if (player != null) {
            var rawHud = player.getHudManager().getCustomHud();
            if (rawHud instanceof RpgXPHud hud) {
                hud.refresh(rpg);
            }
        }

        if (leveledUp) {
            LevelUpEvent.dispatch(event.playerRef(), oldLevel, rpg.getLevel());
        }
    }
}

package com.troubledev.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.troubledev.components.PlayerRPGComponent;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;

public class RpgXPHud extends CustomUIHud {

    private static final int BAR_MAX_WIDTH = 290;
    private final PlayerRPGComponent rpg;

    public RpgXPHud(PlayerRef playerRef, PlayerRPGComponent rpg) {
        super(playerRef);
        this.rpg = rpg;
    }

    @Override
    protected void build(UICommandBuilder ui) {
        ui.append("RpgXPHud.ui");
        if (rpg != null) {
            int fillWidth = Math.round(rpg.getProgress() * BAR_MAX_WIDTH);

            ui.set("#LevelLabel.TextSpans", Message.raw(" Level " + rpg.getLevel()));
            ui.set("#XpText.TextSpans", Message.raw(
                rpg.getCurrentLevelXP() + " / " + (rpg.getCurrentLevelXP() + rpg.getXPToNextLevel()) + " XP"
            ));

            Anchor fillAnchor = new Anchor();
            fillAnchor.setLeft(Value.of(1));
            fillAnchor.setWidth(Value.of(fillWidth));

            Anchor highlightAnchor = new Anchor();
            highlightAnchor.setLeft(Value.of(1));
            highlightAnchor.setWidth(Value.of(fillWidth));

            ui.setObject("#XpBarFill.Anchor", fillAnchor);
            ui.setObject("#XpBarHighlight.Anchor", highlightAnchor);
        }
    }

    public void refresh(PlayerRPGComponent rpg) {
        UICommandBuilder ui = new UICommandBuilder();

        int fillWidth = Math.round(rpg.getProgress() * BAR_MAX_WIDTH);

        ui.set("#LevelLabel.TextSpans", Message.raw(" Level " + rpg.getLevel()));
        ui.set("#XpText.TextSpans", Message.raw(
            rpg.getCurrentLevelXP() + " / " + (rpg.getCurrentLevelXP() + rpg.getXPToNextLevel()) + " XP"
        ));

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(1));
        fillAnchor.setTop(Value.of(1));
        fillAnchor.setHeight(Value.of(10));
        fillAnchor.setWidth(Value.of(fillWidth));

        Anchor highlightAnchor = new Anchor();
        highlightAnchor.setLeft(Value.of(1));
        highlightAnchor.setTop(Value.of(1));
        highlightAnchor.setHeight(Value.of(3));
        highlightAnchor.setWidth(Value.of(fillWidth));

        ui.setObject("#XpBarFill.Anchor", fillAnchor);
        ui.setObject("#XpBarHighlight.Anchor", highlightAnchor);
        

        update(false, ui);
    }
}
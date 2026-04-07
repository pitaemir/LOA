package com.troubledev.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.troubledev.components.PlayerLOAComponent;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;

public class LOAXPHud extends CustomUIHud {

    private static final int BAR_MAX_WIDTH = 290;
    private PlayerLOAComponent loa;

    public LOAXPHud(PlayerRef playerRef, PlayerLOAComponent loa) {
        super(playerRef);
        this.loa = loa;
    }

    @Override
    protected void build(UICommandBuilder ui) {
        ui.append("LOAXPHud.ui");
        ui.set("#LevelUpBanner.Visible", false); // ← esconde ao entrar
        if (loa != null) {
            int fillWidth = Math.round(loa.getProgress() * BAR_MAX_WIDTH);

            ui.set("#LevelLabel.TextSpans", Message.raw(" Level " + loa.getLevel()));
            ui.set("#XpText.TextSpans", Message.raw(
                loa.getCurrentLevelXP() + " / " + (loa.getCurrentLevelXP() + loa.getXPToNextLevel()) + " XP"
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
        }
    }

    public void refresh(PlayerLOAComponent loa) {
        this.loa = loa;
        UICommandBuilder ui = new UICommandBuilder();

        int fillWidth = Math.round(loa.getProgress() * BAR_MAX_WIDTH);

        ui.set("#LevelLabel.TextSpans", Message.raw(" Level " + loa.getLevel()));
        ui.set("#XpText.TextSpans", Message.raw(
            loa.getCurrentLevelXP() + " / " + (loa.getCurrentLevelXP() + loa.getXPToNextLevel()) + " XP"
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

    public void showLevelUp(int newLevel) {
        UICommandBuilder ui = new UICommandBuilder();
        ui.set("#LevelUpBanner.Visible", true);
        ui.set("#LevelUpText.TextSpans", Message.raw("LEVEL UP! Now Level " + newLevel + "!"));
        update(false, ui);

        // esconde depois de 3 segundos
        var playerRef = getPlayerRef();
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            UICommandBuilder hideUi = new UICommandBuilder();
            hideUi.set("#LevelUpBanner.Visible", false);
            update(false, hideUi);
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }
}
package com.troubledev.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.components.PlayerLOAComponent;
import com.troubledev.ui.LOAXPHud;

import javax.annotation.Nonnull;

public class LOAResetLevel extends AbstractPlayerCommand {

    public LOAResetLevel() {
        super("resetlevel", "Resets your level back to 1");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        var loa = store.getComponent(ref, PlayerLOAComponent.getComponentType());
        if (loa == null) {
            playerRef.sendMessage(Message.raw("No LOA data found."));
            return;
        }

        loa.setTotalExperience(0L);
        store.replaceComponent(ref, PlayerLOAComponent.getComponentType(), loa);
        playerRef.sendMessage(Message.raw("Your level has been reset to 1."));

        var player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            var rawHud = player.getHudManager().getCustomHud();
            if (rawHud instanceof LOAXPHud hud) {
                hud.refresh(loa);
            }
        }
    }
}
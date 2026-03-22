package com.troubledev.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.components.PlayerLOAComponent;
import com.troubledev.level.XPTable;

import javax.annotation.Nonnull;

public class LOAStatsCommand extends AbstractPlayerCommand {

    public LOAStatsCommand() {
        super("stats", "Show your loa stats");
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
            playerRef.sendMessage(Message.raw("No loa data found"));
            return;
        }

        var level = loa.getLevel();
        var totalXp = loa.getTotalExperience();
        var currentXp = loa.getCurrentLevelXP();
        var toNext = loa.getXPToNextLevel();
        var progress = (int) (loa.getProgress() * 100);

        playerRef.sendMessage(Message.raw("=== loa Stats ==="));
        playerRef.sendMessage(Message.raw("Level: %d%s".formatted(
                level,
                loa.isMaxLevel() ? " (MAX)" : ""
        )));
        playerRef.sendMessage(Message.raw("Total XP: %d".formatted(totalXp)));

        if (!loa.isMaxLevel()) {
            playerRef.sendMessage(Message.raw("Progress: %d/%d (%d%%)".formatted(
                    currentXp,
                    currentXp + toNext,
                    progress
            )));
            playerRef.sendMessage(Message.raw("To next level: %d XP".formatted(toNext)));
        }
    }
}

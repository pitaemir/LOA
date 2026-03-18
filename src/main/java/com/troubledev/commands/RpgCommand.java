package com.troubledev.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class RpgCommand extends AbstractCommandCollection {

    public RpgCommand() {
        super("rpg", "RPG Debug Commands");
        addSubCommand(new RpgSpawnCommand());
        addSubCommand(new RpgXpCommand());
        addSubCommand(new RpgStatsCommand());
    }
}

package com.troubledev.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class LOACommand extends AbstractCommandCollection {

    public LOACommand() {
        super("loa", "loa Debug Commands");
        addSubCommand(new LOASpawnCommand());
        addSubCommand(new LOAXpCommand());
        addSubCommand(new LOAStatsCommand());
    }
}

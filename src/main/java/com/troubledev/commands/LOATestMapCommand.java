package com.troubledev.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.testing.AutoTestMap;

import javax.annotation.Nonnull;

/**
 * /loa testmap - Liga/desliga a entrada automática na cópia do mapa de teste ao entrar no servidor.
 * Vale até o próximo reinício (padrão: ligado).
 */
public class LOATestMapCommand extends AbstractPlayerCommand {

    public LOATestMapCommand() {
        super("testmap", "Toggle auto-join into a MobaMap test copy");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        AutoTestMap.setEnabled(!AutoTestMap.isEnabled());
        playerRef.sendMessage(Message.raw("Auto-join test map: %s".formatted(AutoTestMap.isEnabled() ? "ON" : "OFF")));
    }
}

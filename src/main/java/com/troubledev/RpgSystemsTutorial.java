package com.troubledev;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.troubledev.commands.RpgCommand;
import com.troubledev.components.PlayerRPGComponent;
import com.troubledev.events.GiveXPEvent;
import com.troubledev.events.LevelUpEvent;
import com.troubledev.handlers.GiveXPHandler;
import com.troubledev.handlers.LevelUpHandler;
import com.troubledev.systems.PlayerJoinSystem;
import com.troubledev.systems.XPGainSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RpgSystemsTutorial extends JavaPlugin {

    public RpgSystemsTutorial(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        var registry = getEntityStoreRegistry();

        var rpgType = registry.registerComponent(
                PlayerRPGComponent.class,
                "MiniRPG_PlayerData",
                PlayerRPGComponent.CODEC
        );
        PlayerRPGComponent.setComponentType(rpgType);

        registry.registerSystem(new XPGainSystem());
        registry.registerSystem(new PlayerJoinSystem());

        getEventRegistry().register(GiveXPEvent.class, new GiveXPHandler());
        getEventRegistry().register(LevelUpEvent.class, new LevelUpHandler());

        getCommandRegistry().registerCommand(new RpgCommand());
    }
}

package com.troubledev;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.troubledev.commands.LOACommand;
import com.troubledev.components.PlayerLOAComponent;
import com.troubledev.components.WeaponMasteryComponent;
import com.troubledev.events.GiveXPEvent;
import com.troubledev.events.LevelUpEvent;
import com.troubledev.handlers.GiveXPHandler;
import com.troubledev.handlers.LevelUpHandler;
import com.troubledev.systems.PlayerJoinSystem;
import com.troubledev.systems.XPGainSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class LOASystems extends JavaPlugin {

    public LOASystems(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        var registry = getEntityStoreRegistry();

        var loaType = registry.registerComponent(
                PlayerLOAComponent.class,
                "Miniloa_PlayerData",
                PlayerLOAComponent.CODEC
        );
        PlayerLOAComponent.setComponentType(loaType);

        var masteryType = registry.registerComponent(
                WeaponMasteryComponent.class,
                "Miniloa_WeaponMastery",
                WeaponMasteryComponent.CODEC
        );
        WeaponMasteryComponent.setComponentType(masteryType);

        registry.registerSystem(new XPGainSystem());
        registry.registerSystem(new PlayerJoinSystem());

        getEventRegistry().register(GiveXPEvent.class, new GiveXPHandler());
        getEventRegistry().register(LevelUpEvent.class, new LevelUpHandler());

        getCommandRegistry().registerCommand(new LOACommand());
    }
}

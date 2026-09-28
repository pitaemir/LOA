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
import com.troubledev.team.TeamAttitudeSystem;
import com.troubledev.team.TeamComponent;
import com.troubledev.team.TeamDamageSystem;
import com.troubledev.team.TeamTargetSystem;
import com.troubledev.waves.CreepDamageSystem;
import com.troubledev.waves.CreepLaneComponent;
import com.troubledev.waves.CreepLaneSystem;
import com.troubledev.waves.WaveManager;
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

        var teamType = registry.registerComponent(
                TeamComponent.class,
                "LOA_Team",
                TeamComponent.CODEC
        );
        TeamComponent.setComponentType(teamType);

        // Sem codec: não é salvo em disco (só vale enquanto a tropa está viva nesta sessão)
        var laneType = registry.registerComponent(CreepLaneComponent.class, CreepLaneComponent::new);
        CreepLaneComponent.setComponentType(laneType);

        registry.registerSystem(new XPGainSystem());
        registry.registerSystem(new PlayerJoinSystem());
        registry.registerSystem(new TeamAttitudeSystem());
        registry.registerSystem(new CreepDamageSystem());
        registry.registerSystem(new TeamDamageSystem());
        registry.registerSystem(new TeamTargetSystem());
        registry.registerSystem(new CreepLaneSystem());

        getEventRegistry().register(GiveXPEvent.class, new GiveXPHandler());
        getEventRegistry().register(LevelUpEvent.class, new LevelUpHandler());

        getCommandRegistry().registerCommand(new LOACommand());
    }

    @Override
    protected void shutdown() {
        WaveManager.stop();
    }
}

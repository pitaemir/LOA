package com.troubledev.team;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/** Time de uma entidade (jogador ou NPC). Mesmo time = aliados, times diferentes = inimigos. */
public class TeamComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, TeamComponent> TYPE;

    public static void setComponentType(ComponentType<EntityStore, TeamComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, TeamComponent> getComponentType() {
        return TYPE;
    }

    public static final BuilderCodec<TeamComponent> CODEC = BuilderCodec
            .builder(TeamComponent.class, TeamComponent::new)
            .append(
                    new KeyedCodec<>("Team", Codec.STRING),
                    (component, value) -> component.team = Team.valueOf(value),
                    component -> component.team.name()
            ).add()
            .build();

    private Team team = Team.BLUE;

    public TeamComponent() {
    }

    public TeamComponent(Team team) {
        this.team = team;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    @NullableDecl
    @Override
    public TeamComponent clone() {
        return new TeamComponent(team);
    }
}

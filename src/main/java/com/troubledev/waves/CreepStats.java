package com.troubledev.waves;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Atributos iguais para todas as tropas das ondas, aliadas ou inimigas,
 * independente do tipo de NPC usado.
 */
public final class CreepStats {

    public static final float HEALTH = 100f;
    /** Dano de cada golpe de uma tropa (ver CreepDamageSystem). */
    public static final float DAMAGE = 10f;

    // Mesmo modificador que o jogo usa para aplicar o MaxHealth do role (BalancingInitialisationSystem).
    // Sobrescrever ele troca a vida do role pela nossa.
    private static final String MAX_HEALTH_MODIFIER = "NPC_Max";

    private CreepStats() {}

    /** Troca a vida máxima do NPC por HEALTH e enche a vida. */
    public static void applyHealth(Store<EntityStore> store, Ref<EntityStore> ref) {
        var stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return;

        int health = DefaultEntityStatTypes.getHealth();
        float baseMax = EntityStatType.getAssetMap().getAsset(health).getMax();

        stats.putModifier(health, MAX_HEALTH_MODIFIER,
                new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, HEALTH - baseMax));
        stats.maximizeStatValue(health);
    }
}

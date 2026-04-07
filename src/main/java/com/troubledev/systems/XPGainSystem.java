package com.troubledev.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.troubledev.components.PlayerLOAComponent;
import com.troubledev.components.WeaponMasteryComponent;
import com.troubledev.events.GiveXPEvent;
import com.troubledev.level.WeaponMasteryTable;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class XPGainSystem extends DeathSystems.OnDeathSystem {

    private static final long XP_PER_KILL = 50L;

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(DeathComponent.getComponentType());
    }

    @Override
    public void onComponentAdded(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl DeathComponent deathComponent,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        var deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) return;

        if (!(deathInfo.getSource() instanceof Damage.EntitySource source)) return;

        var killerRef = source.getRef();
        if (!killerRef.isValid()) return;

        var killer = store.getComponent(killerRef, PlayerRef.getComponentType());
        if (killer == null) return;

        var loa = store.getComponent(killerRef, PlayerLOAComponent.getComponentType());
        if (loa == null) return;

        // XP do player
        GiveXPEvent.dispatch(killerRef, XP_PER_KILL);

        // XP de maestria da arma
        var player = store.getComponent(killerRef, Player.getComponentType());
        if (player == null) return;

        var itemInHand = player.getInventory().getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) return;

        var mastery = store.getComponent(killerRef, WeaponMasteryComponent.getComponentType());
        if (mastery == null) return;

        // Usa o nome da classe como identificador do tipo de arma.
        // Substituir por itemInHand.getItemType().getId() se a API Hytale expor isso.
        var weaponId = itemInHand.getClass().getName();
        boolean leveledUp = mastery.addMasteryXP(weaponId, WeaponMasteryTable.XP_PER_KILL);
        int newLevel = mastery.getMasteryLevel(weaponId);

        killer.sendMessage(Message.raw("+%d Weapon XP (Mastery Lv.%d)".formatted(
            WeaponMasteryTable.XP_PER_KILL, newLevel
        )));

        if (leveledUp) {
            killer.sendMessage(Message.raw("[MASTERY] Level Up! Now Lv.%d!".formatted(newLevel)));
        }
    }
}
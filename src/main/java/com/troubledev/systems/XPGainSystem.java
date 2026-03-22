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
import com.troubledev.events.GiveXPEvent;
import com.troubledev.level.WeaponMasteryTable;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
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

        var inventory = player.getInventory();
        var itemInHand = inventory.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) return;

        var metadata = itemInHand.getMetadata();
        long currentXP = 0L;
        int currentLevel = 1;

        if (metadata != null) {
            if (metadata.containsKey(WeaponMasteryTable.METADATA_XP_KEY)) {
                var xpValue = metadata.get(WeaponMasteryTable.METADATA_XP_KEY);
                currentXP = xpValue.isInt64() ? xpValue.asInt64().getValue() : xpValue.asInt32().getValue();
            }
            if (metadata.containsKey(WeaponMasteryTable.METADATA_LEVEL_KEY)) {
                var levelValue = metadata.get(WeaponMasteryTable.METADATA_LEVEL_KEY);
                currentLevel = levelValue.isInt32() ? levelValue.asInt32().getValue() : (int) levelValue.asInt64().getValue();
            }
        }

        long newXP = currentXP + WeaponMasteryTable.XP_PER_KILL;
        int newLevel = WeaponMasteryTable.getLevelForXP(newXP);

        BsonDocument newMetadata = metadata != null ? metadata.clone() : new BsonDocument();
        newMetadata.put(WeaponMasteryTable.METADATA_XP_KEY, new BsonInt64(newXP));
        newMetadata.put(WeaponMasteryTable.METADATA_LEVEL_KEY, new BsonInt32(newLevel));

       var updatedItem = itemInHand.withMetadata(newMetadata);
        var hotbar = inventory.getHotbar();
        short activeSlot = inventory.getActiveHotbarSlot();
        hotbar.setItemStackForSlot(activeSlot, updatedItem);

        killer.sendMessage(Message.raw("+%d Weapon XP (Mastery Lv.%d)".formatted(
            WeaponMasteryTable.XP_PER_KILL, newLevel
        )));

        if (newLevel > currentLevel) {
            killer.sendMessage(Message.raw("[MASTERY] Level Up! Now Lv.%d!".formatted(newLevel)));
        }
    }
}
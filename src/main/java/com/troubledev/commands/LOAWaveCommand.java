package com.troubledev.commands;

import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.troubledev.team.Team;
import com.troubledev.waves.WaveManager;
import com.troubledev.waves.WaveManager.WaveSettings;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

/**
 * /loa wave - Teste de ondas de tropas nos dois sentidos da lane.
 *
 * Usage:
 *   Por padrão usa os pontos da lane de teste do MobaMap (WaveManager.DEFAULT_*_POINT).
 *   /loa wave setspawn                  -> ponto onde os inimigos nascem (sua posição)
 *   /loa wave setbase                   -> sua base: onde os aliados nascem (sua posição)
 *   /loa wave start                     -> a cada 60s, por lado: 3 corpo a corpo + 2 arqueiros
 *   /loa wave start --interval 20 --count 5 --sides allies
 *   /loa wave start --type Trork_Warrior_Patrol --allytype Kweebec_Razorleaf_Patrol
 *   /loa wave now [--sides enemies]     -> uma onda imediatamente
 *   /loa wave now --count 0 --ranged 3  -> só arqueiros
 *   /loa wave stop
 *   /loa wave clear [--sides allies]    -> remove as tropas (dos dois times por padrão)
 */
public class LOAWaveCommand extends AbstractCommandCollection {

    public LOAWaveCommand() {
        super("wave", "Creep wave test");
        addSubCommand(new SetPointCommand("setspawn", "Set enemy spawn point to your position", true));
        addSubCommand(new SetPointCommand("setbase", "Set your base (ally spawn) to your position", false));
        addSubCommand(new StartCommand());
        addSubCommand(new NowCommand());
        addSubCommand(new StopCommand());
        addSubCommand(new ClearCommand());
    }

    private static Vector3d positionOf(Store<EntityStore> store, Ref<EntityStore> ref) {
        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        return transform == null ? null : transform.getPosition();
    }

    private static String format(Vector3d pos) {
        return "(%.0f, %.0f, %.0f)".formatted(pos.x(), pos.y(), pos.z());
    }

    private static boolean checkConfigured(PlayerRef playerRef) {
        if (WaveManager.isConfigured()) return true;
        playerRef.sendMessage(Message.raw("Set both points first: /loa wave setspawn and /loa wave setbase"));
        return false;
    }

    private static class SetPointCommand extends AbstractPlayerCommand {
        private final boolean spawn;

        SetPointCommand(String name, String description, boolean spawn) {
            super(name, description);
            this.spawn = spawn;
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            var pos = positionOf(store, ref);
            if (pos == null) {
                playerRef.sendMessage(Message.raw("Error: Could not get position"));
                return;
            }

            if (spawn) WaveManager.setSpawnPoint(world, pos);
            else WaveManager.setBasePoint(world, pos);

            playerRef.sendMessage(Message.raw("%s point set at %s".formatted(spawn ? "Spawn" : "Base", format(pos))));
        }
    }

    /** Base dos comandos que spawnam ondas: opções --count, --sides, --type e --allytype. */
    private abstract static class WaveSpawnCommand extends AbstractPlayerCommand {
        private final OptionalArg<Integer> countArg;
        private final OptionalArg<Integer> rangedArg;
        private final OptionalArg<String> sidesArg;
        private final OptionalArg<String> typeArg;
        private final OptionalArg<String> allyTypeArg;

        WaveSpawnCommand(String name, String description) {
            super(name, description);
            this.countArg = withOptionalArg("count", "Melee troops per side (0-20)", ArgTypes.INTEGER)
                    .addValidator(Validators.greaterThanOrEqual(0))
                    .addValidator(Validators.lessThan(21));
            this.rangedArg = withOptionalArg("ranged", "Archers per side (0-10)", ArgTypes.INTEGER)
                    .addValidator(Validators.greaterThanOrEqual(0))
                    .addValidator(Validators.lessThan(11));
            this.sidesArg = withOptionalArg("sides", "both | enemies | allies", ArgTypes.STRING);
            this.typeArg = withOptionalArg("type", "Enemy NPC role (default Creep_Melee_Red)", ArgTypes.STRING);
            this.allyTypeArg = withOptionalArg("allytype", "Ally NPC role (default Creep_Melee_Green)", ArgTypes.STRING);
        }

        /** Lê as opções. Retorna null (e avisa o jogador) se algo for inválido. */
        protected WaveSettings readSettings(CommandContext context, PlayerRef playerRef) {
            var count = countArg.get(context);
            if (count == null) count = WaveManager.DEFAULT_COUNT;

            var ranged = rangedArg.get(context);
            if (ranged == null) ranged = WaveManager.DEFAULT_RANGED_COUNT;

            var sides = sidesArg.get(context);
            if (sides == null) sides = "both";
            boolean enemies, allies;
            switch (sides.toLowerCase()) {
                case "both" -> { enemies = true; allies = true; }
                case "enemies" -> { enemies = true; allies = false; }
                case "allies" -> { enemies = false; allies = true; }
                default -> {
                    playerRef.sendMessage(Message.raw("--sides must be both, enemies or allies"));
                    return null;
                }
            }

            var enemyType = typeArg.get(context);
            if (enemyType == null) enemyType = WaveManager.DEFAULT_ENEMY_TYPE;

            var allyType = allyTypeArg.get(context);
            if (allyType == null) allyType = WaveManager.DEFAULT_ALLY_TYPE;

            if (enemies && !checkNpc(enemyType, playerRef)) return null;
            if (allies && !checkNpc(allyType, playerRef)) return null;

            if (ranged > 0 && enemies && !checkNpc(WaveManager.ENEMY_RANGED_TYPE, playerRef)) return null;
            if (ranged > 0 && allies && !checkNpc(WaveManager.ALLY_RANGED_TYPE, playerRef)) return null;

            return new WaveSettings(count, ranged, enemies ? enemyType : null, allies ? allyType : null);
        }

        private static boolean checkNpc(String type, PlayerRef playerRef) {
            if (NPCPlugin.get().getIndex(type) >= 0) return true;
            playerRef.sendMessage(Message.raw("Unknown NPC: %s".formatted(type)));
            return false;
        }

        protected static String describe(WaveSettings settings) {
            var parts = new StringBuilder();
            if (settings.enemyType() != null) {
                parts.append("%d x %s + %d archers (enemy)".formatted(settings.count(), settings.enemyType(), settings.rangedCount()));
            }
            if (settings.allyType() != null) {
                if (!parts.isEmpty()) parts.append(" | ");
                parts.append("%d x %s + %d archers (ally)".formatted(settings.count(), settings.allyType(), settings.rangedCount()));
            }
            return parts.toString();
        }
    }

    private static class StartCommand extends WaveSpawnCommand {
        private final OptionalArg<Integer> intervalArg;

        StartCommand() {
            super("start", "Start spawning waves");
            this.intervalArg = withOptionalArg("interval", "Seconds between waves (>=5)", ArgTypes.INTEGER)
                    .addValidator(Validators.greaterThanOrEqual(5));
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            WaveManager.useWorld(world);
            if (!checkConfigured(playerRef)) return;

            var settings = readSettings(context, playerRef);
            if (settings == null) return;

            var interval = intervalArg.get(context);
            if (interval == null) interval = WaveManager.DEFAULT_INTERVAL_SECONDS;

            WaveManager.start(interval, settings);
            playerRef.sendMessage(Message.raw("Waves started every %ds: %s".formatted(interval, describe(settings))));
        }
    }

    private static class NowCommand extends WaveSpawnCommand {
        NowCommand() {
            super("now", "Spawn one wave right now");
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            WaveManager.useWorld(world);
            if (!checkConfigured(playerRef)) return;

            var settings = readSettings(context, playerRef);
            if (settings == null) return;

            var spawned = WaveManager.spawnWave(settings);
            playerRef.sendMessage(Message.raw("Spawned %d troops".formatted(spawned)));
        }
    }

    private static class StopCommand extends AbstractPlayerCommand {
        StopCommand() {
            super("stop", "Stop spawning waves");
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            if (!WaveManager.isRunning()) {
                playerRef.sendMessage(Message.raw("Waves are not running"));
                return;
            }
            WaveManager.stop();
            playerRef.sendMessage(Message.raw("Waves stopped"));
        }
    }

    private static class ClearCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> sidesArg;

        ClearCommand() {
            super("clear", "Remove all wave troops");
            this.sidesArg = withOptionalArg("sides", "both | enemies | allies", ArgTypes.STRING);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            var sides = sidesArg.get(context);
            if (sides == null) sides = "both";

            Team team;
            switch (sides.toLowerCase()) {
                case "both" -> team = null;
                case "enemies" -> team = Team.RED;
                case "allies" -> team = Team.BLUE;
                default -> {
                    playerRef.sendMessage(Message.raw("--sides must be both, enemies or allies"));
                    return;
                }
            }

            var removed = WaveManager.clearTroops(store, team);
            var hint = WaveManager.isRunning() ? " (waves are still running, use /loa wave stop)" : "";
            playerRef.sendMessage(Message.raw("Removed %d troops%s".formatted(removed, hint)));
        }
    }
}

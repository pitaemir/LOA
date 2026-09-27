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
import com.troubledev.waves.WaveManager;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

/**
 * /loa wave - Teste de ondas de inimigos.
 *
 * Usage:
 *   /loa wave setspawn                  -> ponto onde os inimigos nascem (sua posição)
 *   /loa wave setbase                   -> ponto para onde eles andam (sua posição)
 *   /loa wave start                     -> 3 Trork_Warrior_Patrol a cada 60s
 *   /loa wave start --interval 20 --count 5 --type Skeleton_Fighter_Patrol
 *   /loa wave now                       -> uma onda imediatamente
 *   /loa wave stop
 */
public class LOAWaveCommand extends AbstractCommandCollection {

    public LOAWaveCommand() {
        super("wave", "Enemy wave test");
        addSubCommand(new SetPointCommand("setspawn", "Set wave spawn point to your position", true));
        addSubCommand(new SetPointCommand("setbase", "Set base (target) point to your position", false));
        addSubCommand(new StartCommand());
        addSubCommand(new NowCommand());
        addSubCommand(new StopCommand());
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

    private static class StartCommand extends AbstractPlayerCommand {
        private final OptionalArg<Integer> intervalArg;
        private final OptionalArg<Integer> countArg;
        private final OptionalArg<String> typeArg;

        StartCommand() {
            super("start", "Start spawning waves");
            this.intervalArg = withOptionalArg("interval", "Seconds between waves (>=5)", ArgTypes.INTEGER)
                    .addValidator(Validators.greaterThanOrEqual(5));
            this.countArg = withOptionalArg("count", "Enemies per wave (1-20)", ArgTypes.INTEGER)
                    .addValidator(Validators.greaterThanOrEqual(1))
                    .addValidator(Validators.lessThan(21));
            this.typeArg = withOptionalArg("type", "NPC role (use a *_Patrol role)", ArgTypes.STRING);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            if (!checkConfigured(playerRef)) return;

            var interval = intervalArg.get(context);
            if (interval == null) interval = WaveManager.DEFAULT_INTERVAL_SECONDS;

            var count = countArg.get(context);
            if (count == null) count = WaveManager.DEFAULT_COUNT;

            var type = typeArg.get(context);
            if (type == null) type = WaveManager.DEFAULT_NPC_TYPE;

            if (NPCPlugin.get().getIndex(type) < 0) {
                playerRef.sendMessage(Message.raw("Unknown NPC: %s".formatted(type)));
                return;
            }

            WaveManager.start(interval, type, count);
            playerRef.sendMessage(Message.raw("Waves started: %d x %s every %ds, from %s to %s".formatted(
                    count, type, interval,
                    format(WaveManager.getSpawnPoint()), format(WaveManager.getBasePoint())
            )));
        }
    }

    private static class NowCommand extends AbstractPlayerCommand {
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
            if (!checkConfigured(playerRef)) return;
            var spawned = WaveManager.spawnWave();
            playerRef.sendMessage(Message.raw("Spawned %d enemies".formatted(spawned)));
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
}

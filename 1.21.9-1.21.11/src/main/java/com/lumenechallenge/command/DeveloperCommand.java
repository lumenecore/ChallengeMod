package com.lumenechallenge.command;

import com.lumenechallenge.challenge.ChallengeGenerator;
import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.condition.ObjectiveCondition;
import com.lumenechallenge.util.WorldMarkerUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Developer-only command for forcing the current objective during playtesting. */
public final class DeveloperCommand {
    private DeveloperCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("challenge")
                    .requires(source -> DeveloperModeConfig.isEnabled())
                    .then(argument("idx", IntegerArgumentType.integer(1, ObjectiveCondition.Type.values().length))
                            .executes(context -> execute(context, false))
                            .then(argument("idy", IntegerArgumentType.integer(1, ObjectiveCondition.Modifier.values().length))
                                    .executes(context -> execute(context, true)))));
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context, boolean hasModifier) {
        if (!DeveloperModeConfig.isEnabled()) {
            context.getSource().sendError(Text.literal("Режим разработчика выключен."));
            return 0;
        }

        final ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (Exception notPlayer) {
            context.getSource().sendError(Text.literal("Команда доступна только игроку в мире ChallengeMod."));
            return 0;
        }

        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null || !WorldMarkerUtil.isMarked(server)) {
            context.getSource().sendError(Text.literal("Команда доступна только в мире ChallengeMod."));
            return 0;
        }

        ChallengeState state = ChallengeState.getServerState(server);
        if (state.challenge() == null || state.completed() || !state.runStarted() || state.playerId() == null
                || !state.playerId().equals(player.getUuid())) {
            context.getSource().sendError(Text.literal("Нет активного забега ChallengeMod для этого игрока."));
            return 0;
        }

        int taskId = IntegerArgumentType.getInteger(context, "idx");
        Integer modifierId = hasModifier ? IntegerArgumentType.getInteger(context, "idy") : null;
        if (ObjectiveCondition.Type.fromId(taskId) == null) {
            context.getSource().sendError(Text.literal("Неизвестный ID задания."));
            return 0;
        }
        if (modifierId != null && ObjectiveCondition.Modifier.fromId(modifierId) == null) {
            context.getSource().sendError(Text.literal("Неизвестный ID усложнения."));
            return 0;
        }

        ChallengeGenerator.GeneratedTask generated = ChallengeGenerator.generateDeveloperTask(
                player, taskId, modifierId, state.runDifficulty(), state.challengeSeed());
        if (generated == null) {
            context.getSource().sendError(Text.literal("Не удалось создать задание; попробуй ещё раз."));
            return 0;
        }
        if (!state.replaceCurrentTaskForDeveloper(player, generated.condition(), generated.definition())) {
            context.getSource().sendError(Text.literal("Не удалось заменить текущее задание."));
            return 0;
        }

        String suffix = modifierId == null ? "без усложнения" : "с усложнением ID " + modifierId;
        context.getSource().sendFeedback(() -> Text.literal("Текущее задание заменено на ID " + taskId + " ("
                + generated.condition().type().name() + "), " + suffix + "."), false);
        return 1;
    }
}

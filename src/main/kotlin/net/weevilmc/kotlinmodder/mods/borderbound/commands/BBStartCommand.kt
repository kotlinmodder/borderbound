package net.weevilmc.kotlinmodder.mods.borderbound.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.weevilmc.kotlinmodder.mods.borderbound.GameManager

object BBStartCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("bbstart")
                .requires { source -> source.hasPermissionLevel(2) } // Requires op
                .then(
                    CommandManager.argument("startsize", IntegerArgumentType.integer(1))
                        .then(
                            CommandManager.argument("finishsize", IntegerArgumentType.integer(1))
                                .then(
                                    CommandManager.argument("time", IntegerArgumentType.integer(1))
                                        .then(
                                            CommandManager.argument("enablepvp", BoolArgumentType.bool())
                                                .executes { context ->
                                                    execute(
                                                        context,
                                                        IntegerArgumentType.getInteger(context, "startsize"),
                                                        IntegerArgumentType.getInteger(context, "finishsize"),
                                                        IntegerArgumentType.getInteger(context, "time"),
                                                        BoolArgumentType.getBool(context, "enablepvp")
                                                    )
                                                }
                                        )
                                        .executes { context ->
                                            execute(
                                                context,
                                                IntegerArgumentType.getInteger(context, "startsize"),
                                                IntegerArgumentType.getInteger(context, "finishsize"),
                                                IntegerArgumentType.getInteger(context, "time"),
                                                false
                                            )
                                        }
                                )
                                .executes { context ->
                                    execute(
                                        context,
                                        IntegerArgumentType.getInteger(context, "startsize"),
                                        IntegerArgumentType.getInteger(context, "finishsize"),
                                        7200,
                                        false
                                    )
                                }
                        )
                        .executes { context ->
                            execute(
                                context,
                                IntegerArgumentType.getInteger(context, "startsize"),
                                100,
                                7200,
                                false
                            )
                        }
                )
                .executes { context ->
                    execute(context, 10000, 100, 7200, false)
                }
        )
    }

    private fun execute(
        context: CommandContext<ServerCommandSource>,
        startSize: Int,
        finishSize: Int,
        time: Int,
        enablePvp: Boolean
    ): Int {
        val source = context.source
        val server = source.server

        // Validate arguments
        if (startSize <= finishSize) {
            source.sendError(Text.literal("Start size must be greater than finish size!"))
            return 0
        }

        // Start the game
        try {
            GameManager.startGame(server, startSize, finishSize, time, enablePvp)
            source.sendFeedback({ Text.literal("Borderbound game started!") }, true)
            return 1
        } catch (e: Exception) {
            source.sendError(Text.literal("Failed to start game: ${e.message}"))
            e.printStackTrace()
            return 0
        }
    }
}

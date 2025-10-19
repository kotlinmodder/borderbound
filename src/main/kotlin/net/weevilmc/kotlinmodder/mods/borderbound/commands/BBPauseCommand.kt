package net.weevilmc.kotlinmodder.mods.borderbound.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.weevilmc.kotlinmodder.mods.borderbound.GameManager
import net.weevilmc.kotlinmodder.mods.borderbound.GameState

object BBPauseCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("bbtogglepause")
                .requires { source -> source.hasPermissionLevel(2) } // Requires op
                .executes { context ->
                    execute(context)
                }
        )
    }

    private fun execute(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val server = source.server

        if (!GameState.isGameActive) {
            source.sendError(Text.literal("No Borderbound game is currently active!"))
            return 0
        }

        if (GameState.isPaused) {
            // Resume the game
            try {
                GameManager.resumeGame(server)
                source.sendFeedback({ Text.literal("Game resumed! Border is shrinking again.") }, true)
                return 1
            } catch (e: Exception) {
                source.sendError(Text.literal("Failed to resume game: ${e.message}"))
                e.printStackTrace()
                return 0
            }
        } else {
            // Pause the game
            try {
                GameManager.pauseGame(server)
                source.sendFeedback({ Text.literal("Game paused! Border has stopped shrinking.") }, true)
                return 1
            } catch (e: Exception) {
                source.sendError(Text.literal("Failed to pause game: ${e.message}"))
                e.printStackTrace()
                return 0
            }
        }
    }
}

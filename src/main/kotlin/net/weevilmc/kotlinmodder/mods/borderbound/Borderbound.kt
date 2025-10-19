package net.weevilmc.kotlinmodder.mods.borderbound

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.command.CommandManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import net.minecraft.world.World
import net.weevilmc.kotlinmodder.mods.borderbound.commands.BBStartCommand
import net.weevilmc.kotlinmodder.mods.borderbound.commands.BBPauseCommand

class Borderbound : ModInitializer {

    override fun onInitialize() {
        // Register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            BBStartCommand.register(dispatcher)
            BBPauseCommand.register(dispatcher)
        }

        // Register tick event to check border size and enable PvP
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (GameState.isGameActive && !GameState.enablePvp) {
                if (GameState.hasBorderReachedFinishSize(server)) {
                    // Mark as enabled so we don't check again
                    GameState.enablePvp = true

                    // Enable PvP game rule
                    val overworld = server.getWorld(World.OVERWORLD)
                    overworld?.gameRules?.get(GameRules.PVP)?.set(true, server)

                    // Notify players
                    server.playerManager.broadcast(
                        Text.literal("The border has reached its final size! PvP is now enabled!"),
                        false
                    )
                }
            }
        }

        // Register player death event for elimination mechanic
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, damageSource ->
            if (GameState.isGameActive && entity is ServerPlayerEntity) {
                // Check if border has reached finish size
                val world = entity.getEntityWorld() as? net.minecraft.server.world.ServerWorld ?: return@register
                val server = world.server
                if (GameState.hasBorderReachedFinishSize(server)) {
                    // Check if killed by another player
                    val attacker = damageSource.attacker
                    if (attacker is ServerPlayerEntity && attacker.uuid != entity.uuid) {
                        // Mark player as eliminated
                        GameState.eliminatedPlayers.add(entity.uuid)

                        // Notify all players
                        server.playerManager.broadcast(
                            Text.literal("${entity.name.string} has been eliminated by ${attacker.name.string}!"),
                            false
                        )
                    }
                }
            }
        }

        // Register player respawn event
        ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
            // Check if player is eliminated
            if (GameState.isGameActive && GameState.eliminatedPlayers.contains(newPlayer.uuid)) {
                newPlayer.changeGameMode(GameMode.SPECTATOR)
                newPlayer.sendMessage(
                    Text.literal("You have been eliminated! You are now in spectator mode."),
                    false
                )
                return@register
            }

            if (GameState.isGameActive && !alive) {
                // Player died - respawn them 100 blocks inside the border
                val world = newPlayer.getEntityWorld() as? net.minecraft.server.world.ServerWorld ?: return@register
                val serverInstance = world.server

                if (!GameState.hasBorderReachedFinishSize(serverInstance)) {
                    // Border hasn't reached finish size yet, respawn inside border
                    val overworld = serverInstance.getWorld(World.OVERWORLD) ?: return@register
                    val border = overworld.worldBorder
                    val centerX = border.getCenterX()
                    val centerZ = border.getCenterZ()
                    val currentRadius = (border.size / 2) - 100 // 100 blocks inside

                    if (currentRadius > 0) {
                        // Get player's designated edge from their starting position
                        val startingPos = GameState.startingPositions[newPlayer.uuid]
                        val worldSpawn = GameState.worldSpawn ?: net.minecraft.util.math.BlockPos(0, 64, 0)

                        // Calculate respawn position on player's designated edge
                        val respawnX: Double
                        val respawnZ: Double

                        if (startingPos != null) {
                            // Calculate direction from center to starting position
                            val deltaX = startingPos.x - worldSpawn.x
                            val deltaZ = startingPos.z - worldSpawn.z
                            val distance = kotlin.math.sqrt((deltaX * deltaX + deltaZ * deltaZ).toDouble())

                            if (distance > 0) {
                                // Normalize and scale to current radius
                                respawnX = centerX + (deltaX / distance) * currentRadius
                                respawnZ = centerZ + (deltaZ / distance) * currentRadius
                            } else {
                                // Fallback to center if can't calculate direction
                                respawnX = centerX
                                respawnZ = centerZ
                            }
                        } else {
                            // Fallback to center if no starting position recorded
                            respawnX = centerX
                            respawnZ = centerZ
                        }

                        // Find a safe spawn position - use dimension height limits
                        val topY = overworld.height + overworld.bottomY

                        // Find the first solid block below
                        var y = topY
                        while (y > overworld.bottomY) {
                            val checkPos = net.minecraft.util.math.BlockPos(respawnX.toInt(), y, respawnZ.toInt())
                            if (overworld.getBlockState(checkPos).isSolidBlock(overworld, checkPos)) {
                                break
                            }
                            y--
                        }

                        val finalPos = net.minecraft.util.math.BlockPos(
                            respawnX.toInt(),
                            y + 1,
                            respawnZ.toInt()
                        )

                        newPlayer.teleport(
                            overworld,
                            finalPos.x + 0.5,
                            finalPos.y.toDouble(),
                            finalPos.z + 0.5,
                            emptySet(),
                            newPlayer.yaw,
                            newPlayer.pitch,
                            false
                        )

                        newPlayer.sendMessage(
                            net.minecraft.text.Text.literal("You respawned at your designated edge!"),
                            false
                        )
                    }
                }
            }
        }
    }
}

package net.weevilmc.kotlinmodder.mods.borderbound

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.EntityType
import net.minecraft.entity.projectile.FireworkRocketEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.server.command.CommandManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
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

        // Note: Persistence could be added here in the future
        // For now, game state is lost on server restart

        // Register tick event to check border size, enable PvP, and show action bar
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (GameState.isGameActive && !GameState.enablePvp) {
                if (GameState.hasBorderReachedFinishSize(server)) {
                    // Mark as enabled so we don't check again
                    GameState.enablePvp = true

                    // Enable PvP game rule
                    val overworld = server.getWorld(World.OVERWORLD)
                    overworld?.gameRules?.get(GameRules.PVP)?.set(true, server)

                    // Play ender dragon growl sound for PvP enabled
                    server.playerManager.playerList.forEach { p ->
                        p.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f)
                    }

                    // Show PvP enabled title
                    GameManager.sendTitleToAll(
                        server,
                        "PvP ENABLED",
                        "The border has reached its final size!",
                        fadeIn = 10,
                        stay = 60,
                        fadeOut = 20
                    )

                }
            }

            // Show action bar border distance to all players
            if (GameState.isGameActive) {
                server.playerManager.playerList.forEach { player ->
                    if (player.getEntityWorld().registryKey == World.OVERWORLD) {
                        val distance = GameManager.getDistanceToBorder(player)
                        if (distance >= 0) {
                            val color = when {
                                distance < 50 -> Formatting.RED
                                distance < 100 -> Formatting.GOLD
                                distance < 160 -> Formatting.YELLOW
                                else -> Formatting.GREEN
                            }
                            val distanceText = String.format("%.1f", distance)
                            player.sendMessage(
                                Text.literal("Border: ${distanceText}m").formatted(color),
                                true // Action bar
                            )
                        }
                    }
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

                        // Play trident thunder sound for elimination
                        server.playerManager.playerList.forEach { p ->
                            p.playSound(SoundEvents.ITEM_TRIDENT_THUNDER.value(), 1.5f, 1.0f)
                        }

                        // Announce elimination
                        server.playerManager.broadcast(
                            Text.literal("${entity.name.string} has been eliminated by ${attacker.name.string}!")
                                .formatted(Formatting.RED, Formatting.BOLD),
                            false
                        )

                        // Check if there's a winner (only one non-eliminated player left)
                        val alivePlayers = server.playerManager.playerList.filter {
                            !GameState.eliminatedPlayers.contains(it.uuid)
                        }
                        if (alivePlayers.size == 1) {
                            val winner = alivePlayers[0]

                            // Announce winner
                            GameManager.sendTitleToAll(
                                server,
                                "${winner.name.string} WINS!",
                                "Champion of Borderbound!",
                                fadeIn = 10,
                                stay = 100,
                                fadeOut = 20
                            )

                            // Spawn 6 fireworks at winner's location
                            val overworld = server.getWorld(World.OVERWORLD)
                            if (overworld != null) {
                                for (i in 0 until 6) {
                                    val firework = FireworkRocketEntity(
                                        overworld,
                                        winner.x + (Math.random() - 0.5) * 3,
                                        winner.y,
                                        winner.z + (Math.random() - 0.5) * 3,
                                        createFireworkStack()
                                    )
                                    overworld.spawnEntity(firework)
                                }
                            }

                            // End the game
                            GameState.isGameActive = false
                        }

                        // Save state
                        val state = BorderboundState.getServerState(server)
                        GameState.saveToState(state)
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

    private fun createFireworkStack(): ItemStack {
        val firework = ItemStack(Items.FIREWORK_ROCKET)

        val colors = it.unimi.dsi.fastutil.ints.IntArrayList()
        colors.add(0xFF0000)
        colors.add(0xFFFF00)
        colors.add(0x00FF00)

        val fadeColors = it.unimi.dsi.fastutil.ints.IntArrayList()
        fadeColors.add(0xFFFFFF)

        val explosions = listOf(
            net.minecraft.component.type.FireworkExplosionComponent(
                net.minecraft.component.type.FireworkExplosionComponent.Type.BURST,
                colors,
                fadeColors,
                true,
                true
            )
        )

        firework.set(
            net.minecraft.component.DataComponentTypes.FIREWORKS,
            net.minecraft.component.type.FireworksComponent(2, explosions)
        )

        return firework
    }
}

package net.weevilmc.kotlinmodder.mods.borderbound

import net.minecraft.block.Blocks
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameRules
import net.minecraft.world.World
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

object GameManager {

    /**
     * Sends a golden title to all players on the server
     * @param server The Minecraft server
     * @param title The main title text (large, center screen)
     * @param subtitle Optional subtitle text (smaller, below title)
     * @param fadeIn Fade in time in ticks (default: 10)
     * @param stay Stay time in ticks (default: 70)
     * @param fadeOut Fade out time in ticks (default: 20)
     */
    fun sendTitleToAll(
        server: MinecraftServer,
        title: String,
        subtitle: String? = null,
        fadeIn: Int = 10,
        stay: Int = 70,
        fadeOut: Int = 20
    ) {
        val titleText = Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD)
        val subtitleText = subtitle?.let { Text.literal(it).formatted(Formatting.YELLOW) }

        server.playerManager.playerList.forEach { player ->
            player.networkHandler.sendPacket(TitleFadeS2CPacket(fadeIn, stay, fadeOut))
            player.networkHandler.sendPacket(TitleS2CPacket(titleText))
            subtitleText?.let {
                player.networkHandler.sendPacket(SubtitleS2CPacket(it))
            }
        }
    }

    fun startGame(
        server: MinecraftServer,
        startSize: Int,
        finishSize: Int,
        time: Int,
        enablePvp: Boolean
    ) {
        // Reset game state
        GameState.reset()

        // Set game parameters
        GameState.startSize = startSize
        GameState.finishSize = finishSize
        GameState.shrinkTimeSeconds = time
        GameState.enablePvp = enablePvp
        GameState.isGameActive = true

        // Get overworld
        val overworld = server.getWorld(World.OVERWORLD)
            ?: throw IllegalStateException("Overworld not found")

        // Get world spawn - center at 0,0 for consistency
        val worldSpawn = BlockPos(0, 64, 0)
        GameState.worldSpawn = worldSpawn

        // Set up world border
        setupWorldBorder(server, worldSpawn, startSize, finishSize, time)

        // Set PvP game rule to match enablePvp parameter
        overworld.gameRules.get(GameRules.PVP).set(enablePvp, server)

        // Get all players
        val players = server.playerManager.playerList
        if (players.isEmpty()) {
            throw IllegalStateException("No players online to start the game")
        }

        // Teleport players and give slow falling
        teleportPlayers(server, players, worldSpawn, startSize)

        // Play goat horn ponder sound for game start
        server.playerManager.playerList.forEach { p ->
            p.playSound(net.minecraft.sound.SoundEvents.GOAT_HORN_SOUNDS.get(0).value(), 2.0f, 1.0f)
        }

        // Show game start title
        val subtitle = if (!enablePvp) {
            "Border: $startSize → $finishSize blocks | PvP enabled at final size"
        } else {
            "Border: $startSize → $finishSize blocks over $time seconds"
        }
        sendTitleToAll(server, "BORDERBOUND", subtitle, fadeIn = 10, stay = 100, fadeOut = 20)

    }

    private fun setupWorldBorder(
        server: MinecraftServer,
        center: BlockPos,
        startSize: Int,
        finishSize: Int,
        timeSeconds: Int
    ) {
        val overworld = server.getWorld(World.OVERWORLD) ?: return
        val border = overworld.worldBorder

        // Center the border on world spawn
        border.setCenter(center.x.toDouble(), center.z.toDouble())

        // Set initial size (diameter)
        border.setSize((startSize * 2).toDouble())

        // Start shrinking to finish size
        border.interpolateSize(
            (startSize * 2).toDouble(),
            (finishSize * 2).toDouble(),
            (timeSeconds * 1000).toLong() // Convert to milliseconds
        )
    }

    private fun teleportPlayers(
        server: MinecraftServer,
        players: List<ServerPlayerEntity>,
        worldSpawn: BlockPos,
        startSize: Int
    ) {
        val playerCount = players.size
        val radius = startSize - 100.0 // 100 blocks inside the border

        // Calculate positions based on player count
        val positions = calculateStartingPositions(worldSpawn, radius, playerCount)

        players.forEachIndexed { index, player ->
            val pos = positions[index]
            GameState.startingPositions[player.uuid] = pos

            // Place starting block (glass)
            val overworld = server.getWorld(World.OVERWORLD) ?: return
            overworld.setBlockState(pos, Blocks.GLASS.defaultState)

            // Teleport player
            player.teleport(
                overworld,
                pos.x + 0.5,
                pos.y + 1.0,
                pos.z + 0.5,
                emptySet(),
                player.yaw,
                player.pitch,
                false
            )

            // Give slow falling for 1 minute (1200 ticks)
            player.addStatusEffect(StatusEffectInstance(StatusEffects.SLOW_FALLING, 1200, 0))

            player.sendMessage(Text.literal("You have been teleported to your starting position!"), false)
        }
    }

    private fun calculateStartingPositions(center: BlockPos, radius: Double, playerCount: Int): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()
        val y = 200 // Starting height

        when {
            playerCount == 1 -> {
                // Single player - place at east
                positions.add(BlockPos(center.x + radius.toInt(), y, center.z))
            }
            playerCount == 2 -> {
                // East and West
                positions.add(BlockPos(center.x + radius.toInt(), y, center.z))
                positions.add(BlockPos(center.x - radius.toInt(), y, center.z))
            }
            playerCount == 3 -> {
                // East, West, North
                positions.add(BlockPos(center.x + radius.toInt(), y, center.z))
                positions.add(BlockPos(center.x - radius.toInt(), y, center.z))
                positions.add(BlockPos(center.x, y, center.z - radius.toInt()))
            }
            playerCount == 4 -> {
                // East, West, North, South
                positions.add(BlockPos(center.x + radius.toInt(), y, center.z))
                positions.add(BlockPos(center.x - radius.toInt(), y, center.z))
                positions.add(BlockPos(center.x, y, center.z - radius.toInt()))
                positions.add(BlockPos(center.x, y, center.z + radius.toInt()))
            }
            else -> {
                // 5-8 players: Cardinal directions + corners
                // First 4 in cardinal directions
                positions.add(BlockPos(center.x + radius.toInt(), y, center.z)) // East
                positions.add(BlockPos(center.x - radius.toInt(), y, center.z)) // West
                positions.add(BlockPos(center.x, y, center.z - radius.toInt())) // North
                positions.add(BlockPos(center.x, y, center.z + radius.toInt())) // South

                // Remaining in corners
                if (playerCount > 4) {
                    val cornerRadius = radius / kotlin.math.sqrt(2.0)
                    positions.add(BlockPos(center.x + cornerRadius.toInt(), y, center.z - cornerRadius.toInt())) // NE
                    if (playerCount > 5) {
                        positions.add(BlockPos(center.x + cornerRadius.toInt(), y, center.z + cornerRadius.toInt())) // SE
                    }
                    if (playerCount > 6) {
                        positions.add(BlockPos(center.x - cornerRadius.toInt(), y, center.z + cornerRadius.toInt())) // SW
                    }
                    if (playerCount > 7) {
                        positions.add(BlockPos(center.x - cornerRadius.toInt(), y, center.z - cornerRadius.toInt())) // NW
                    }
                }
            }
        }

        return positions
    }

    fun getDistanceToBorder(player: ServerPlayerEntity): Double {
        val world = player.getEntityWorld()
        if (world.registryKey != World.OVERWORLD) return -1.0

        val border = world.worldBorder
        return border.getDistanceInsideBorder(player)
    }

    fun pauseGame(server: MinecraftServer) {
        if (!GameState.isGameActive) {
            throw IllegalStateException("No game is active")
        }

        if (GameState.isPaused) {
            throw IllegalStateException("Game is already paused")
        }

        val overworld = server.getWorld(World.OVERWORLD)
            ?: throw IllegalStateException("Overworld not found")

        val border = overworld.worldBorder

        // Save current border state
        GameState.pausedBorderSize = border.size
        GameState.pausedTargetSize = (GameState.finishSize * 2).toDouble()

        // Calculate remaining time
        // The border's sizeLerpTarget gives us the target, and sizeLerpTime gives us the total time
        val currentSize = border.size
        val targetSize = GameState.pausedTargetSize
        val totalShrink = (GameState.startSize * 2).toDouble() - targetSize
        val completedShrink = (GameState.startSize * 2).toDouble() - currentSize
        val remainingFraction = if (totalShrink > 0) (totalShrink - completedShrink) / totalShrink else 0.0
        GameState.remainingShrinkTimeMillis = (GameState.shrinkTimeSeconds * 1000 * remainingFraction).toLong()

        // Stop the border from shrinking by setting it to its current size
        border.setSize(currentSize)

        // Mark as paused
        GameState.isPaused = true

        // Show pause title
        sendTitleToAll(
            server,
            "GAME PAUSED",
            "Remaining time: ${GameState.remainingShrinkTimeMillis / 1000}s",
            fadeIn = 5,
            stay = 50,
            fadeOut = 10
        )

    }

    fun resumeGame(server: MinecraftServer) {
        if (!GameState.isGameActive) {
            throw IllegalStateException("No game is active")
        }

        if (!GameState.isPaused) {
            throw IllegalStateException("Game is not paused")
        }

        val overworld = server.getWorld(World.OVERWORLD)
            ?: throw IllegalStateException("Overworld not found")

        val border = overworld.worldBorder

        // Resume border shrinking from current size to target with remaining time
        border.interpolateSize(
            GameState.pausedBorderSize,
            GameState.pausedTargetSize,
            GameState.remainingShrinkTimeMillis
        )

        // Mark as unpaused
        GameState.isPaused = false

        // Show resume title
        sendTitleToAll(
            server,
            "GAME RESUMED",
            "Border shrinking for ${GameState.remainingShrinkTimeMillis / 1000}s",
            fadeIn = 5,
            stay = 50,
            fadeOut = 10
        )

    }
}

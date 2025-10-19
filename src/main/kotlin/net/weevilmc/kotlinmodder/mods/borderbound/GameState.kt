package net.weevilmc.kotlinmodder.mods.borderbound

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import java.util.UUID

object GameState {
    var isGameActive = false
    var isPaused = false
    var startSize = 10000
    var finishSize = 100
    var shrinkTimeSeconds = 7200
    var enablePvp = false
    var worldSpawn: BlockPos? = null
    var startingPositions = mutableMapOf<UUID, BlockPos>()

    // Pause-related state
    var pausedBorderSize = 0.0
    var pausedTargetSize = 0.0
    var remainingShrinkTimeMillis = 0L

    fun reset() {
        isGameActive = false
        isPaused = false
        startSize = 10000
        finishSize = 100
        shrinkTimeSeconds = 7200
        enablePvp = false
        worldSpawn = null
        startingPositions.clear()
        pausedBorderSize = 0.0
        pausedTargetSize = 0.0
        remainingShrinkTimeMillis = 0L
    }

    fun getCurrentBorderSize(server: net.minecraft.server.MinecraftServer): Double {
        val overworld = server.getWorld(net.minecraft.world.World.OVERWORLD) ?: return startSize.toDouble()
        return overworld.worldBorder.size
    }

    fun hasBorderReachedFinishSize(server: net.minecraft.server.MinecraftServer): Boolean {
        val currentSize = getCurrentBorderSize(server)
        return currentSize <= finishSize * 2.0 + 1.0 // Add small tolerance
    }
}

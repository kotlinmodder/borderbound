package net.weevilmc.kotlinmodder.mods.borderbound

import net.minecraft.server.MinecraftServer
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

    // Elimination tracking
    var eliminatedPlayers = mutableSetOf<UUID>()

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
        eliminatedPlayers.clear()
    }

    fun getCurrentBorderSize(server: MinecraftServer): Double {
        val overworld = server.getWorld(net.minecraft.world.World.OVERWORLD) ?: return startSize.toDouble()
        return overworld.worldBorder.size
    }

    fun hasBorderReachedFinishSize(server: MinecraftServer): Boolean {
        val currentSize = getCurrentBorderSize(server)
        return currentSize <= finishSize * 2.0 + 1.0 // Add small tolerance
    }

    // Persistence methods
    fun saveToState(state: BorderboundState) {
        state.isGameActive = isGameActive
        state.isPaused = isPaused
        state.startSize = startSize
        state.finishSize = finishSize
        state.shrinkTimeSeconds = shrinkTimeSeconds
        state.enablePvp = enablePvp
        state.worldSpawn = worldSpawn
        state.startingPositions = startingPositions.toMutableMap()
        state.pausedBorderSize = pausedBorderSize
        state.pausedTargetSize = pausedTargetSize
        state.remainingShrinkTimeMillis = remainingShrinkTimeMillis
        state.eliminatedPlayers = eliminatedPlayers.toMutableSet()
        state.writeNbt(net.minecraft.nbt.NbtCompound())
    }

    fun loadFromState(state: BorderboundState) {
        isGameActive = state.isGameActive
        isPaused = state.isPaused
        startSize = state.startSize
        finishSize = state.finishSize
        shrinkTimeSeconds = state.shrinkTimeSeconds
        enablePvp = state.enablePvp
        worldSpawn = state.worldSpawn
        startingPositions = state.startingPositions.toMutableMap()
        pausedBorderSize = state.pausedBorderSize
        pausedTargetSize = state.pausedTargetSize
        remainingShrinkTimeMillis = state.remainingShrinkTimeMillis
        eliminatedPlayers = state.eliminatedPlayers.toMutableSet()
    }
}

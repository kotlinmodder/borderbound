package net.weevilmc.kotlinmodder.mods.borderbound

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateManager
import net.minecraft.world.World
import java.util.UUID

class BorderboundState private constructor() : PersistentState() {

    var isGameActive = false
    var isPaused = false
    var startSize = 10000
    var finishSize = 100
    var shrinkTimeSeconds = 7200
    var enablePvp = false
    var worldSpawn: BlockPos? = null
    var startingPositions = mutableMapOf<UUID, BlockPos>()
    var pausedBorderSize = 0.0
    var pausedTargetSize = 0.0
    var remainingShrinkTimeMillis = 0L
    var eliminatedPlayers = mutableSetOf<UUID>()

    fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.putBoolean("isGameActive", isGameActive)
        nbt.putBoolean("isPaused", isPaused)
        nbt.putInt("startSize", startSize)
        nbt.putInt("finishSize", finishSize)
        nbt.putInt("shrinkTimeSeconds", shrinkTimeSeconds)
        nbt.putBoolean("enablePvp", enablePvp)

        worldSpawn?.let { spawn ->
            val spawnNbt = NbtCompound()
            spawnNbt.putInt("x", spawn.x)
            spawnNbt.putInt("y", spawn.y)
            spawnNbt.putInt("z", spawn.z)
            nbt.put("worldSpawn", spawnNbt)
        }

        // Save starting positions
        val positionsList = NbtList()
        startingPositions.forEach { (uuid, pos) ->
            val posNbt = NbtCompound()
            posNbt.putString("uuid", uuid.toString())
            posNbt.putInt("x", pos.x)
            posNbt.putInt("y", pos.y)
            posNbt.putInt("z", pos.z)
            positionsList.add(posNbt)
        }
        nbt.put("startingPositions", positionsList)

        // Save pause state
        nbt.putDouble("pausedBorderSize", pausedBorderSize)
        nbt.putDouble("pausedTargetSize", pausedTargetSize)
        nbt.putLong("remainingShrinkTimeMillis", remainingShrinkTimeMillis)

        // Save eliminated players
        val eliminatedList = NbtList()
        eliminatedPlayers.forEach { uuid ->
            val playerNbt = NbtCompound()
            playerNbt.putString("uuid", uuid.toString())
            eliminatedList.add(playerNbt)
        }
        nbt.put("eliminatedPlayers", eliminatedList)

        markDirty()
        return nbt
    }

    fun readNbt(nbt: NbtCompound) {
        isGameActive = nbt.getBoolean("isGameActive").orElse(false)
        isPaused = nbt.getBoolean("isPaused").orElse(false)
        startSize = nbt.getInt("startSize").orElse(10000)
        finishSize = nbt.getInt("finishSize").orElse(100)
        shrinkTimeSeconds = nbt.getInt("shrinkTimeSeconds").orElse(7200)
        enablePvp = nbt.getBoolean("enablePvp").orElse(false)

        if (nbt.contains("worldSpawn")) {
            val spawnNbt = nbt.getCompound("worldSpawn").get()
            worldSpawn = BlockPos(
                spawnNbt.getInt("x").orElse(0),
                spawnNbt.getInt("y").orElse(64),
                spawnNbt.getInt("z").orElse(0)
            )
        }

        // Load starting positions
        startingPositions.clear()
        if (nbt.contains("startingPositions")) {
            val positionsList = nbt.getList("startingPositions").get()
            for (i in 0 until positionsList.size) {
                val posNbt = positionsList.get(i) as NbtCompound
                val uuidStr = posNbt.getString("uuid").orElse("")
                if (uuidStr.isNotEmpty()) {
                    val uuid = UUID.fromString(uuidStr)
                    val pos = BlockPos(
                        posNbt.getInt("x").orElse(0),
                        posNbt.getInt("y").orElse(64),
                        posNbt.getInt("z").orElse(0)
                    )
                    startingPositions[uuid] = pos
                }
            }
        }

        // Load pause state
        pausedBorderSize = nbt.getDouble("pausedBorderSize").orElse(0.0)
        pausedTargetSize = nbt.getDouble("pausedTargetSize").orElse(0.0)
        remainingShrinkTimeMillis = nbt.getLong("remainingShrinkTimeMillis").orElse(0L)

        // Load eliminated players
        eliminatedPlayers.clear()
        if (nbt.contains("eliminatedPlayers")) {
            val eliminatedList = nbt.getList("eliminatedPlayers").get()
            for (i in 0 until eliminatedList.size) {
                val playerNbt = eliminatedList.get(i) as NbtCompound
                val uuidStr = playerNbt.getString("uuid").orElse("")
                if (uuidStr.isNotEmpty()) {
                    eliminatedPlayers.add(UUID.fromString(uuidStr))
                }
            }
        }
    }

    companion object {
        private var instance: BorderboundState? = null

        fun getServerState(server: MinecraftServer): BorderboundState {
            if (instance == null) {
                instance = BorderboundState()
            }
            return instance!!
        }
    }
}

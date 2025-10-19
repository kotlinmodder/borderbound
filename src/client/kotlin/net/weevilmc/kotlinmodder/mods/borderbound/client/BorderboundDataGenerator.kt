package net.weevilmc.kotlinmodder.mods.borderbound.client

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class BorderboundDataGenerator : DataGeneratorEntrypoint {

    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()
    }
}

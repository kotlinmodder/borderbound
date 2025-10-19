package net.weevilmc.kotlinmodder.mods.borderbound.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.text.Text
import net.minecraft.world.World

class BorderboundClient : ClientModInitializer {

    override fun onInitializeClient() {
        // Register HUD renderer
        HudRenderCallback.EVENT.register { drawContext, tickCounter ->
            renderBorderDistanceHud(drawContext, tickCounter)
        }
    }

    private fun renderBorderDistanceHud(drawContext: DrawContext, tickCounter: RenderTickCounter) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val world = client.world ?: return

        // Only show in overworld
        if (world.registryKey != World.OVERWORLD) return

        val border = world.worldBorder
        val distance = border.getDistanceInsideBorder(player.x, player.z)

        // Calculate color based on distance (red when close, yellow when medium, green when far)
        val color = when {
            distance < 50 -> 0xFFFF0000.toInt() // Red - very close
            distance < 100 -> 0xFFFF8800.toInt() // Orange - close
            distance < 160 -> 0xFFFFFF00.toInt() // Yellow - medium
            else -> 0xFF00FF00.toInt() // Green - safe
        }

        // Format distance
        val distanceText = String.format("%.1f", distance)

        // Draw the HUD
        val text = Text.literal("Border: ${distanceText}m")
        val textRenderer = client.textRenderer
        val textWidth = textRenderer.getWidth(text)

        // Position to the right of the hotbar (bottom center-right)
        val x = (drawContext.scaledWindowWidth / 2) + 100
        val y = drawContext.scaledWindowHeight - 40

        // Draw background
        drawContext.fill(x - 5, y - 2, x + textWidth + 5, y + textRenderer.fontHeight + 2, 0x80000000.toInt())

        // Draw text
        drawContext.drawText(textRenderer, text, x, y, color, true)
    }
}

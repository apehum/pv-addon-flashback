package su.plo.voice.flashback

import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer

fun isCameraRemotePlayer() = Minecraft.getInstance().getCameraEntity() is RemotePlayer

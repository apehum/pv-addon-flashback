package su.plo.voice.flashback

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.phys.Vec3

fun Camera.position(): Vec3 =
    //? if >= 26.1 {
    /*position()
    *///?} else {
    position
    //?}

fun isCameraRemotePlayer() = Minecraft.getInstance().getCameraEntity() is RemotePlayer

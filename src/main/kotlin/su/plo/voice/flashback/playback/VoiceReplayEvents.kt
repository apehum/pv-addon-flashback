package su.plo.voice.flashback.playback

import com.moulberry.flashback.Flashback
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.event.audio.capture.AudioCaptureEvent
import su.plo.voice.api.client.event.audio.capture.AudioCaptureInitializeEvent
import su.plo.voice.api.client.event.audio.device.source.AlSourceWriteEvent
import su.plo.voice.api.client.event.audio.source.AudioSourceResetEvent
import su.plo.voice.api.client.event.connection.ConnectionKeyPairGenerateEvent
import su.plo.voice.api.client.event.render.HudActivationRenderEvent
import su.plo.voice.api.client.event.render.VoiceDistanceRenderEvent
import su.plo.voice.api.client.event.socket.UdpClientClosedEvent
import su.plo.voice.api.client.event.socket.UdpClientConnectEvent
import su.plo.voice.api.client.time.SystemTimeSupplier
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.flashback.FlashbackVoiceAddon
import su.plo.voice.flashback.isCameraRemotePlayer
import kotlin.jvm.optionals.getOrNull

class VoiceReplayEvents(
    private val addon: FlashbackVoiceAddon,
    private val voiceClient: PlasmoVoiceClient,
) {
    @EventSubscribe
    fun onKeyPairGenerate(event: ConnectionKeyPairGenerateEvent) {
        VoiceSetupListener.currentKeyPair?.let {
            event.keyPair = it
        }
    }

    @EventSubscribe
    fun onUdpClientConnect(event: UdpClientConnectEvent) {
        if (!Flashback.isInReplay()) return

        voiceClient.timeSupplier = ReplayTimeSupplier

        val udpClient = DummyUdpClient(voiceClient, event.connectionPacket.secret)
        voiceClient.eventBus.register(addon, udpClient)

        voiceClient.udpClientManager.setClient(udpClient)
        event.isCancelled = true
    }

    @EventSubscribe
    fun onUdpClientDisconnect(event: UdpClientClosedEvent) {
        if (voiceClient.timeSupplier !is ReplayTimeSupplier) return
        voiceClient.timeSupplier = SystemTimeSupplier()
        VoiceSetupListener.currentKeyPair = null
    }

    @EventSubscribe
    fun onHudActivationRender(event: HudActivationRenderEvent) {
        if (!Flashback.isInReplay() || event.isRender) return

        if (!isCameraRemotePlayer()) return
        val player = Minecraft.getInstance().getCameraEntity() as RemotePlayer?

        val isActivated =
            voiceClient.sourceManager.allSelfSourceInfos
                .filter { sourceInfo ->
                    sourceInfo.selfSourceInfo.playerId == player!!.uuid &&
                        sourceInfo.selfSourceInfo.activationId == event.activation.id
                }.any { sourceInfo ->
                    voiceClient.sourceManager
                        .getSourceById(sourceInfo!!.selfSourceInfo.sourceInfo.id)
                        .getOrNull()
                        ?.isActivated()
                        ?: false
                }

        event.isRender = isActivated
    }

    @EventSubscribe(ignoreCancelled = true)
    fun onDistanceRender(event: VoiceDistanceRenderEvent) {
        if (!Flashback.isInReplay()) return

        event.isCancelled = !isCameraRemotePlayer()
    }

    @EventSubscribe(ignoreCancelled = true)
    fun onAudioCaptureInitialize(event: AudioCaptureInitializeEvent) {
        event.isCancelled = Flashback.isInReplay()
    }

    @EventSubscribe(ignoreCancelled = true)
    fun onAudioCapture(event: AudioCaptureEvent) {
        event.isCancelled = Flashback.isInReplay()
    }

    @EventSubscribe
    fun onSourceReset(event: AudioSourceResetEvent) {
        if (!Flashback.isInReplay()) return
        if (event.cause != AudioSourceResetEvent.Cause.SOURCE_STOPPED) return

        event.isCancelled = true
    }

    @EventSubscribe
    fun onSourceWrite(event: AlSourceWriteEvent) {
        // todo: uhh?
//        event.source.getDevice().runInContextAsync(
//            Runnable {
//                Flashback.getReplayServer()?.getDesiredTickRate()
//                event.source.pitch = ReplayInterface.getCurrentSpeed() as Float
//            },
//        )
    }
}

package su.plo.voice.flashback.playback

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.impl.networking.client.ClientNetworkingImpl
import net.minecraft.client.Minecraft
import su.plo.slib.mod.channel.ByteArrayPayload
import su.plo.voice.flashback.FlashbackVoiceAddon
import su.plo.voice.flashback.event.FlashbackEvents
import su.plo.voice.flashback.network.VoiceSetupPacket
import su.plo.voice.flashback.util.extension.encodeToByteArrayPayload
import su.plo.voice.server.ModVoiceServer
import java.security.KeyPair

class VoiceSetupListener(
    private val addon: FlashbackVoiceAddon,
) : ClientPlayNetworking.PlayPayloadHandler<VoiceSetupPacket> {
    init {
        FlashbackEvents.EXPORT_END.register {
            addon.voiceClient.sourceManager.clear()
            addon.voiceClient.deviceManager.outputDevice
                .ifPresent { it.reload() }
        }
    }

    // todo: add public API reset method in ClientAudioSource
//    private val sourceResetMethod by lazy {
//        BaseClientAudioSource::class.java
//            .getDeclaredMethod(
//                "resetAsync",
//                AudioSourceResetEvent.Cause::class.java,
//            ).also { it.isAccessible = true }
//    }

    override fun receive(
        voiceSetupPacket: VoiceSetupPacket,
        context: ClientPlayNetworking.Context,
    ) {
        val stateInitialized = addon.voiceClient.serverInfo.isPresent
        if (stateInitialized) {
            // client is already connected and state is already set,
            // and we don't have to do it again;
            // but we need to reset all existing sources
            addon.voiceClient.sourceManager.sources
                .forEach { source ->
                    source.closeAsync()
                }
            return
        }

        val connection = Minecraft.getInstance().connection ?: return
        currentKeyPair = voiceSetupPacket.keyPair

        // this is a hack, but it's necessary to save compat with existing replays
        // better way is to record packets separately
        val handler =
            ClientNetworkingImpl
                .getAddon(connection)
                .getHandler(ModVoiceServer.CHANNEL)
                as? ClientPlayNetworking.PlayPayloadHandler<ByteArrayPayload>
                ?: return

        listOfNotNull(
            voiceSetupPacket.connection,
            voiceSetupPacket.config,
            voiceSetupPacket.playerList,
            voiceSetupPacket.language,
        ).map { it.encodeToByteArrayPayload() }
            .forEach { handler.receive(it, context) }
    }

    companion object {
        var currentKeyPair: KeyPair? = null
    }
}

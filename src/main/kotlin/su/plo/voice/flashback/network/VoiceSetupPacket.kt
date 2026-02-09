package su.plo.voice.flashback.network

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import su.plo.voice.flashback.FlashbackVoiceAddon
import su.plo.voice.proto.packets.tcp.clientbound.ConfigPacket
import su.plo.voice.proto.packets.tcp.clientbound.ConnectionPacket
import su.plo.voice.proto.packets.tcp.clientbound.LanguagePacket
import su.plo.voice.proto.packets.tcp.clientbound.PlayerListPacket
import java.security.KeyPair

class VoiceSetupPacket(
    val keyPair: KeyPair,
    val connection: ConnectionPacket? = null,
    val config: ConfigPacket? = null,
    val playerList: PlayerListPacket? = null,
    val language: LanguagePacket? = null,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = type

    companion object {
        val type = CustomPacketPayload.Type<VoiceSetupPacket>(FlashbackVoiceAddon.modResourceLocation("setup"))
        val streamCodec = VoiceSetupPacketCodec()
    }
}

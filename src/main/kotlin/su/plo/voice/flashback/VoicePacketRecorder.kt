package su.plo.voice.flashback

import com.moulberry.flashback.Flashback
import net.minecraft.client.Minecraft
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.event.connection.UdpClientPacketReceivedEvent
import su.plo.voice.api.client.event.connection.UdpClientPacketSendEvent
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.flashback.event.FlashbackEvents
import su.plo.voice.flashback.network.PacketUdpWrapper
import su.plo.voice.flashback.network.VoiceSetupPacket
import su.plo.voice.flashback.util.extension.encodeToByteArrayPayload
import su.plo.voice.proto.data.audio.capture.VoiceActivation
import su.plo.voice.proto.data.audio.line.VoiceSourceLine
import su.plo.voice.proto.data.config.PlayerIconConfig
import su.plo.voice.proto.data.encryption.EncryptionInfo
import su.plo.voice.proto.packets.Packet
import su.plo.voice.proto.packets.tcp.clientbound.ConfigPacket
import su.plo.voice.proto.packets.tcp.clientbound.ConnectionPacket
import su.plo.voice.proto.packets.tcp.clientbound.LanguagePacket
import su.plo.voice.proto.packets.tcp.clientbound.PlayerListPacket
import su.plo.voice.proto.packets.tcp.clientbound.SelfSourceInfoPacket
import su.plo.voice.proto.packets.tcp.clientbound.SourceInfoPacket
import su.plo.voice.proto.packets.udp.PacketUdp
import su.plo.voice.proto.packets.udp.clientbound.SelfAudioInfoPacket
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket
import java.security.KeyPair
import javax.crypto.Cipher
import kotlin.jvm.optionals.getOrNull

class VoicePacketRecorder(
    private val voiceClient: PlasmoVoiceClient,
) {
    private val minecraft by lazy { Minecraft.getInstance() }

    init {
        FlashbackEvents.WRITE_INITIAL_SNAPSHOT.register {
            val keyPair = voiceClient.serverConnection.getOrNull()?.keyPair ?: return@register
            val connectionPacket = createConnectionPacket() ?: return@register
            val configPacket = createConfigPacket(keyPair) ?: return@register
            val playerList = createPlayerListPacket() ?: return@register
            val language = createLanguagePacket() ?: return@register

            Flashback.RECORDER.writePacketAsync(
                ClientboundCustomPayloadPacket(
                    VoiceSetupPacket(
                        keyPair,
                        connectionPacket,
                        configPacket,
                        playerList,
                        language,
                    ),
                ),
                ConnectionProtocol.PLAY,
            )

            currentSourcesPackets()?.forEach { sourceInfoPacket ->
                Flashback.RECORDER.writePacketAsync(
                    ClientboundCustomPayloadPacket(
                        sourceInfoPacket.encodeToByteArrayPayload(),
                    ),
                    ConnectionProtocol.PLAY,
                )
            }

            currentSelfSourcesPackets()?.forEach { sourceInfoPacket ->
                Flashback.RECORDER.writePacketAsync(
                    ClientboundCustomPayloadPacket(
                        sourceInfoPacket.encodeToByteArrayPayload(),
                    ),
                    ConnectionProtocol.PLAY,
                )
            }
        }
    }

    @EventSubscribe
    fun onSourceAudioPacketReceived(event: UdpClientPacketReceivedEvent) {
        if (!shouldRecordPackets()) return

        if (event.packet is SourceAudioPacket) {
            recordUdpPacket(event.packet)
        } else if (event.packet is SelfAudioInfoPacket) {
            recordUdpPacket(event.packet)
        }
    }

    @EventSubscribe
    fun onUdpPacketSend(event: UdpClientPacketSendEvent) {
        if (!shouldRecordPackets() || minecraft.player == null || (event.packet !is PlayerAudioPacket)) return
        recordUdpPacket(event.packet)
    }

    private fun recordUdpPacket(packet: Packet<*>) {
        val secret =
            voiceClient.udpClientManager.client
                .getOrNull()
                ?.secret
                ?: return

        val udpPacket =
            PacketUdpWrapper(
                PacketUdp(
                    secret,
                    System.currentTimeMillis(),
                    packet,
                ),
            )

        if (!shouldRecordPackets()) return

        Flashback.RECORDER.writePacketAsync(
            ClientboundCustomPayloadPacket(udpPacket),
            ConnectionProtocol.PLAY,
        )
    }

    private fun shouldRecordPackets(): Boolean = Flashback.RECORDER?.readyToWrite() == true

    private fun createLanguagePacket(): LanguagePacket? {
        val serverConnection = voiceClient.serverConnection.getOrNull() ?: return null
        return LanguagePacket("", serverConnection.language)
    }

    private fun createPlayerListPacket(): PlayerListPacket? {
        val serverConnection = voiceClient.serverConnection.getOrNull() ?: return null
        return PlayerListPacket(serverConnection.players.toList())
    }

    private fun createConnectionPacket(): ConnectionPacket? {
        val udpClient = voiceClient.udpClientManager.client.getOrNull() ?: return null
        val remoteAddress = udpClient.remoteAddress.getOrNull() ?: return null

        return ConnectionPacket(
            udpClient.secret,
            remoteAddress.hostString,
            remoteAddress.port,
        )
    }

    private fun createConfigPacket(keyPair: KeyPair): ConfigPacket? {
        val serverInfo = voiceClient.serverInfo.getOrNull() ?: return null
        val sourceLines = voiceClient.sourceLineManager
        val activations = voiceClient.activationManager

        return ConfigPacket(
            serverInfo.serverId,
            serverInfo.voiceInfo.captureInfo,
            serverInfo.encryption
                .getOrNull()
                ?.let {
                    try {
                        val encryptCipher = Cipher.getInstance("RSA")
                        encryptCipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
                        val encryptionData = encryptCipher.doFinal(it.key.encoded)

                        EncryptionInfo(
                            it.name,
                            encryptionData,
                        )
                    } catch (e: Exception) {
                        return null
                    }
                },
            sourceLines.lines
                .map { sourceLine ->
                    VoiceSourceLine(
                        sourceLine.name,
                        sourceLine.translation,
                        sourceLine.icon,
                        sourceLine.defaultVolume,
                        sourceLine.weight,
                        sourceLine.players.takeIf { sourceLine.hasPlayers() }?.toSet() ?: emptySet(),
                    )
                }.toSet(),
            activations.activations
                .map { activation ->
                    VoiceActivation(
                        activation.name,
                        activation.translation,
                        activation.icon,
                        activation.distances,
                        activation.defaultDistance,
                        activation.isProximity,
                        activation.isStereoSupported,
                        activation.isTransitive,
                        activation.encoderInfo.getOrNull(),
                        activation.weight,
                    )
                }.toSet(),
            serverInfo.playerInfo.permissions,
            PlayerIconConfig(
                serverInfo.playerIconVisibility.toHashSet(),
                serverInfo.playerIconOffset,
            ),
        )
    }

    private fun currentSourcesPackets(): List<SourceInfoPacket>? {
        if (voiceClient.serverInfo.isEmpty) return null
        return voiceClient.sourceManager.sources.map { SourceInfoPacket(it.sourceInfo) }
    }

    private fun currentSelfSourcesPackets(): List<SelfSourceInfoPacket>? {
        if (voiceClient.serverInfo.isEmpty) return null
        return voiceClient.sourceManager.allSelfSourceInfos.map { SelfSourceInfoPacket(it.selfSourceInfo) }
    }
}

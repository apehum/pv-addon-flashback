package su.plo.voice.flashback.network

import com.google.common.io.ByteStreams
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import su.plo.voice.proto.packets.Packet
import su.plo.voice.proto.packets.tcp.clientbound.ConfigPacket
import su.plo.voice.proto.packets.tcp.clientbound.ConnectionPacket
import su.plo.voice.proto.packets.tcp.clientbound.LanguagePacket
import su.plo.voice.proto.packets.tcp.clientbound.PlayerListPacket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class VoiceSetupPacketCodec : StreamCodec<RegistryFriendlyByteBuf, VoiceSetupPacket> {
    override fun decode(buf: RegistryFriendlyByteBuf): VoiceSetupPacket {
        val publicKey = ByteArray(buf.readInt())
        buf.readBytes(publicKey)
        val privateKey = ByteArray(buf.readInt())
        buf.readBytes(privateKey)

        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKeySpec = X509EncodedKeySpec(publicKey)
        val privateKeySpec = PKCS8EncodedKeySpec(privateKey)

        val connection = buf.readPacketOrNull(::ConnectionPacket)
        val config = buf.readPacketOrNull(::ConfigPacket)
        val playerList = buf.readPacketOrNull(::PlayerListPacket)
        val language = buf.readPacketOrNull(::LanguagePacket)

        return VoiceSetupPacket(
            KeyPair(
                keyFactory.generatePublic(publicKeySpec),
                keyFactory.generatePrivate(privateKeySpec),
            ),
            connection,
            config,
            playerList,
            language,
        )
    }

    override fun encode(
        buf: RegistryFriendlyByteBuf,
        voiceSetupPacket: VoiceSetupPacket,
    ) {
        val keyPair = voiceSetupPacket.keyPair

        val publicKey = keyPair.public.encoded
        val privateKey = keyPair.private.encoded

        buf.writeInt(publicKey.size)
        buf.writeBytes(publicKey)

        buf.writeInt(privateKey.size)
        buf.writeBytes(privateKey)

        listOfNotNull(
            voiceSetupPacket.connection,
            voiceSetupPacket.config,
            voiceSetupPacket.playerList,
            voiceSetupPacket.language,
        ).forEach { buf.writePacket(it) }
    }

    private fun <T : Packet<*>> FriendlyByteBuf.readPacketOrNull(packetBuilder: () -> T): T? {
        if (readableBytes() <= 0) return null

        try {
            val packetBytes = ByteArray(readInt())
            readBytes(packetBytes)

            val packet = packetBuilder()
            packet.read(ByteStreams.newDataInput(packetBytes))
            return packet
        } catch (_: Throwable) {
            return null
        }
    }

    private fun FriendlyByteBuf.writePacket(packet: Packet<*>) {
        val packetBytes =
            ByteStreams
                .newDataOutput()
                .also { packet.write(it) }
                .toByteArray()

        writeInt(packetBytes.size)
        writeBytes(packetBytes)
    }
}

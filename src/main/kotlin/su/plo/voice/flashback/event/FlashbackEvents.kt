package su.plo.voice.flashback.event

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import java.util.function.Consumer

data object FlashbackEvents {
    @JvmField
    val WRITE_SNAPSHOT =
        EventFactory.createArrayBacked(
            WriteSnapshot::class.java,
        ) { callbacks ->
            WriteSnapshot { consumer ->
                callbacks.forEach { callback -> callback.onWrite(consumer) }
            }
        }

    @JvmField
    val EXPORT_START =
        EventFactory.createArrayBacked(
            ExportStart::class.java,
        ) { callbacks ->
            ExportStart {
                callbacks.forEach { callback -> callback.onStart() }
            }
        }

    @JvmField
    val EXPORT_END =
        EventFactory.createArrayBacked(
            ExportEnd::class.java,
        ) { callbacks ->
            ExportEnd {
                callbacks.forEach { callback -> callback.onEnd() }
            }
        }

    fun interface WriteSnapshot {
        fun onWrite(consumer: Consumer<Packet<in ClientGamePacketListener>>)
    }

    fun interface ExportStart {
        fun onStart()
    }

    fun interface ExportEnd {
        fun onEnd()
    }
}

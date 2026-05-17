package su.plo.voice.flashback.mixin;

import com.moulberry.flashback.record.Recorder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.voice.flashback.event.FlashbackEvents;

import java.util.function.Consumer;

@Mixin(value = Recorder.class, remap = false)
public class MixinRecorder {
    @Inject(method = "writeCustomSnapshot", at = @At("HEAD"), remap = false)
    public void writeCustomSnapshot(Consumer<Packet<? super ClientGamePacketListener>> consumer, CallbackInfo ci) {
        FlashbackEvents.WRITE_SNAPSHOT.invoker().onWrite(consumer);
    }
}

package su.plo.voice.flashback.mixin;

import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.voice.client.audio.source.BaseClientAudioSource;
import su.plo.voice.flashback.CameraUtilKt;

@Mixin(
        value = BaseClientAudioSource.class,
        remap = false
)
public class MixinBaseClientAudioSource {
    @Inject(method = "getListenerPosition", at = @At("RETURN"), cancellable = true)
    public void getListener(CallbackInfoReturnable<Vec3> cir) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) return;

        Camera audioCamera = editorState.getAudioCamera();
        if (audioCamera == null) return;

        cir.setReturnValue(CameraUtilKt.position(audioCamera));
    }
}

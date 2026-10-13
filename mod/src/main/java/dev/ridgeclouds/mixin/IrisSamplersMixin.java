package dev.ridgeclouds.mixin;
import dev.ridgeclouds.client.Bridge;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.gl.sampler.*;
import net.irisshaders.iris.gl.texture.TextureAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=IrisSamplers.class,remap=false)
public abstract class IrisSamplersMixin {
    @Inject(method="addNoiseSampler",at=@At("TAIL"),remap=false)
    private static void ridgeSampler(SamplerHolder holder,TextureAccess noise,CallbackInfo ci){holder.addDynamicSampler(Bridge::texture,GlSampler.LINEAR,"ridgeField");holder.addDynamicSampler(Bridge::motionTexture,GlSampler.LINEAR,"ridgeMotion");}
}

package dev.ridgeclouds.mixin;
import dev.ridgeclouds.client.Bridge;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=CommonUniforms.class,remap=false)
public abstract class CommonUniformsMixin {
    private static final java.util.Set<FrameUpdateNotifier> ridgeNotifiers=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    @Inject(method="generalCommonUniforms",at=@At("TAIL"),remap=false)
    private static void ridgeUniforms(UniformHolder holder,FrameUpdateNotifier notifier,PackDirectives directives,CallbackInfo ci){
        if(ridgeNotifiers.add(notifier))notifier.addListener(Bridge::frame);
        holder.uniform2i(UniformUpdateFrequency.PER_FRAME,"ridgeOrigin",Bridge::origin);
        holder.uniform4f(UniformUpdateFrequency.PER_FRAME,"ridgeParams",Bridge::parameters);
        holder.uniform2f(UniformUpdateFrequency.PER_FRAME,"ridgeWind",Bridge::wind);
        holder.uniform2f(UniformUpdateFrequency.PER_FRAME,"ridgeBounds",Bridge::bounds);
        holder.uniform1f(UniformUpdateFrequency.PER_FRAME,"ridgeScale",(net.irisshaders.iris.gl.uniform.FloatSupplier)Bridge::scale);
        holder.uniform1f(UniformUpdateFrequency.PER_FRAME,"ridgeTime",(net.irisshaders.iris.gl.uniform.FloatSupplier)Bridge::time);
    }
}

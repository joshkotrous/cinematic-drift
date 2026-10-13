package dev.ridgeclouds.mixin;
import dev.ridgeclouds.client.Bridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method="tick",at=@At("TAIL")) private void ridgeTick(CallbackInfo ci){Bridge.tick();}
    @Inject(method="close",at=@At("HEAD")) private void ridgeClose(CallbackInfo ci){Bridge.close();}
}

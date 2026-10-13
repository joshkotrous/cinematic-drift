package dev.ridgeclouds.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;

public final class RidgeCloudsClient implements ClientModInitializer {
    public void onInitializeClient(){ClientCommandRegistrationCallback.EVENT.register((dispatcher,context)->{
        var root=literal("ridgeclouds").executes(c->status(c.getSource()));
        root.then(literal("automatic").executes(c->{Settings s=Bridge.settings();s.windOverride=false;s.densityOverride=false;s.thickness=16;Bridge.save();return status(c.getSource());}));
        root.then(literal("status").executes(c->status(c.getSource())));
        root.then(literal("enable").executes(c->{Bridge.settings().enabled=true;Bridge.save();return status(c.getSource());}));
        root.then(literal("disable").executes(c->{Bridge.settings().enabled=false;Bridge.save();return status(c.getSource());}));
        root.then(literal("reset").executes(c->{Bridge.FLOW.reset();c.getSource().sendFeedback(Component.literal("Ridge Clouds: flow reset."));return 1;}));
        root.then(literal("wind").then(argument("degrees",FloatArgumentType.floatArg(0,360)).then(argument("speed",FloatArgumentType.floatArg(0,45)).executes(c->{Bridge.settings().windOverride=true;Bridge.settings().wind=FloatArgumentType.getFloat(c,"degrees");Bridge.settings().speed=FloatArgumentType.getFloat(c,"speed");Bridge.save();return status(c.getSource());}))));
        root.then(setting("altitude",-64,500));root.then(setting("density",0,1.8f));root.then(setting("thickness",15,100));root.then(setting("fade",30,200));
        root.then(setting("scale",8,160));
        root.then(literal("quality").then(argument("samples",IntegerArgumentType.integer(32,144)).executes(c->{Bridge.settings().quality=IntegerArgumentType.getInteger(c,"samples");Bridge.save();return status(c.getSource());})));
        root.then(literal("lookout").executes(c->lookout(c.getSource())));
        root.then(literal("preset").then(literal("spill").executes(c->{Settings s=Bridge.settings();s.enabled=true;s.windOverride=false;s.densityOverride=false;s.thickness=16;s.evaporation=85;s.quality=96;float h=0;for(int k=0;k<Bridge.FLOW.terrain.length;k++)if(Bridge.FLOW.validity[k]>0)h=Math.max(h,Bridge.FLOW.terrain[k]);s.altitude=Math.clamp(h-60,80,320);Bridge.save();return status(c.getSource());})));
        dispatcher.register(root);
    });System.out.println("[Ridge Clouds] Client terrain bridge and /ridgeclouds commands loaded.");}
    private static LiteralArgumentBuilder<FabricClientCommandSource> setting(String name,float min,float max){return literal(name).then(argument("value",FloatArgumentType.floatArg(min,max)).executes(c->{float v=FloatArgumentType.getFloat(c,"value");Settings s=Bridge.settings();switch(name){case "altitude"->s.altitude=v;case "density"->{s.density=v;s.densityOverride=true;}case "thickness"->s.thickness=v;case "fade"->s.evaporation=v;case "scale"->s.scale=v;}Bridge.save();return status(c.getSource());}));}
    private static int status(FabricClientCommandSource source){Settings s=Bridge.settings();source.sendFeedback(Component.literal(String.format(java.util.Locale.ROOT,"Ridge Clouds %s | wind %.0f° / %.1f blocks/s | altitude %.0f | density %.2f | thickness %.0f | fade %.0f | %d samples",(s.enabled?"ON":"OFF")+(!s.windOverride&&!s.densityOverride?" (shader/weather defaults)":" (manual override)"),s.wind,s.speed,s.altitude,s.density,s.thickness,s.evaporation,s.quality)));return 1;}
    private static int lookout(FabricClientCommandSource source){int best=-1;float score=-1;double px=source.getPlayer().getX(),pz=source.getPlayer().getZ();for(int k=0;k<Bridge.FLOW.terrain.length;k++){if(Bridge.FLOW.validity[k]<1)continue;double dist=Math.hypot(Bridge.worldX(k)-px,Bridge.worldZ(k)-pz);if(dist>400)continue;float s=Bridge.FLOW.terrain[k]+Bridge.FLOW.drop[k]*.3f;if(s>score){score=s;best=k;}}
        if(best<0){source.sendError(Component.literal("Wait for nearby chunks to load, then run /ridgeclouds lookout again."));return 0;}
        double a=Math.toRadians(Bridge.settings().wind),tx=Bridge.worldX(best),tz=Bridge.worldZ(best),ty=Bridge.FLOW.terrain[best]+35;
        double x=tx+Math.cos(a)*200-Math.sin(a)*120,z=tz+Math.sin(a)*200+Math.cos(a)*120,y=ty+80;
        float yaw=(float)Math.toDegrees(Math.atan2(-(tx-x),tz-z)),pitch=(float)Math.toDegrees(Math.atan2(y-ty,Math.hypot(tx-x,tz-z)));
        String command=String.format(java.util.Locale.ROOT,"/tp @s %.0f %.0f %.0f %.1f %.1f",x,y,z,yaw,pitch);
        source.sendFeedback(Component.literal("Loaded-terrain ridge viewpoint (click to fill teleport; requires cheats): "+command).withStyle(style->style.withClickEvent(new ClickEvent.SuggestCommand(command))));
        source.sendFeedback(Component.literal("After teleporting, use /ridgeclouds preset spill. Wind: 0 east, 90 south, 180 west, 270 north."));return 1;
    }
}

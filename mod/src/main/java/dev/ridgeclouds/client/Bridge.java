package dev.ridgeclouds.client;

import dev.ridgeclouds.core.FlowField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;
import java.nio.FloatBuffer;
import java.nio.file.Path;

/** World adapter and texture transport. The shader performs all drawing inside Iris. */
public final class Bridge {
    public static final FlowField FLOW=new FlowField(96,16);
    private static final FloatBuffer pixels=BufferUtils.createFloatBuffer(96*96*4);
    private static Settings settings=new Settings();
    private static Path config; private static long configStamp=-1,lastFrame=0,lastSettings=0;
    private static ClientLevel world; private static int originX,originZ,cursor=0,texture=0;
    private static boolean dirty=true,active=false;
    private static boolean allocated=false,leeDirty=true; private static int motionTexture=0; private static boolean motionAllocated=false;
    private static int tickCounter=0,noiseTexture=0;
    private static float accumulator=0,lower=0,upper=640,previousWind=-1;
    public static Settings settings(){return settings;}
    public static void save(){try{if(config==null)config=Minecraft.getInstance().gameDirectory.toPath().resolve("config/ridge-clouds.json");java.nio.file.Files.createDirectories(config.getParent());java.nio.file.Files.writeString(config,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(settings));configStamp=java.nio.file.Files.getLastModifiedTime(config).toMillis();}catch(Exception e){System.err.println("[Ridge Clouds] Save: "+e.getMessage());}}
    public static int worldX(int k){return originX+(k%FLOW.n)*16+8;}
    public static int worldZ(int k){return originZ+(k/FLOW.n)*16+8;}
    public static void tick(){Minecraft mc=Minecraft.getInstance();if(config==null)config=mc.gameDirectory.toPath().resolve("config/ridge-clouds.json");if(System.nanoTime()-lastSettings>1_000_000_000L){lastSettings=System.nanoTime();try{long stamp=java.nio.file.Files.exists(config)?java.nio.file.Files.getLastModifiedTime(config).toMillis():0;if(stamp!=configStamp){settings=Settings.read(config);configStamp=java.nio.file.Files.getLastModifiedTime(config).toMillis();}}catch(Exception ignored){}}
        active=settings.enabled&&mc.level!=null&&mc.player!=null&&mc.level.dimension().equals(Level.OVERWORLD);
        if(!active){if(world!=null){world=null;FLOW.clearTerrain();leeDirty=true;dirty=true;}return;}
        int x=((int)Math.floor(mc.player.getX()/16)-48)*16,z=((int)Math.floor(mc.player.getZ()/16)-48)*16;
        if(world!=mc.level){world=mc.level;originX=x;originZ=z;FLOW.clearTerrain();leeDirty=true;cursor=0;}
        else if(x!=originX||z!=originZ){FLOW.shift((x-originX)/16,(z-originZ)/16);originX=x;originZ=z;leeDirty=true;cursor=0;}
        // At most 512 loaded columns per tick; never generate/request missing chunks.
        for(int count=0;count<512;count++){int k=cursor++%(FLOW.n*FLOW.n),i=k%FLOW.n,j=k/FLOW.n,wx=originX+i*16+8,wz=originZ+j*16+8;
            // false prevents requests and avoids accepting the empty fallback chunk as ground.
            var chunk=world.getChunkSource().getChunk(wx>>4,wz>>4,ChunkStatus.FULL,false);
            if(chunk!=null&&!chunk.isEmpty()){
                float h=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,wx&15,wz&15)+1;
                if(FLOW.observeTerrain(k,h))leeDirty=true;
            }
            // Missing columns retain their last known height while inside this bounded grid.

        }FLOW.direction=settings.windOverride?settings.wind:90;FLOW.evaporation=settings.evaporation;
        if(previousWind!=FLOW.direction){leeDirty=true;previousWind=FLOW.direction;}
        if(leeDirty&&tickCounter++%10==0){FLOW.recomputeLee();leeDirty=false;}dirty=true;
    }
    public static void frame(){long now=System.nanoTime();float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;FLOW.altitude=settings.altitude;FLOW.density=settings.densityOverride?settings.density:1f;FLOW.speed=settings.windOverride?settings.speed:nativeSpeed();FLOW.direction=settings.windOverride?settings.wind:90;
        if(active){accumulator=Math.min(.1f,accumulator+dt);while(accumulator>=1f/30){FLOW.step(1f/30);accumulator-=1f/30;dirty=true;}}
    }
    public static int texture(){
        if(texture==0){texture=GL33.glGenTextures();dirty=true;allocated=false;}
        if(dirty){
            pixels.clear();
            lower=Float.POSITIVE_INFINITY;upper=Float.NEGATIVE_INFINITY;
            for(int k=0;k<FLOW.n*FLOW.n;k++){pixels.put(FLOW.terrain[k]).put(FLOW.centre[k]).put(FLOW.cover[k]).put(FLOW.validity[k]);if(FLOW.validity[k]>.9&&FLOW.cover[k]>.04){lower=Math.min(lower,FLOW.centre[k]-settings.thickness*1.6f);upper=Math.max(upper,FLOW.centre[k]+settings.thickness*1.6f);}}
            if(!Float.isFinite(lower)){lower=0;upper=0;}
            pixels.flip();
            TextureUploader.upload(texture,FLOW.n,pixels,!allocated);
            allocated=true;
            if(motionTexture==0)motionTexture=GL33.glGenTextures();pixels.clear();for(int k=0;k<FLOW.n*FLOW.n;k++)pixels.put(FLOW.driftX[k]).put(FLOW.driftZ[k]).put(FLOW.windMultiplier(k)).put(FLOW.validity[k]);pixels.flip();TextureUploader.upload(motionTexture,FLOW.n,pixels,!motionAllocated);motionAllocated=true;dirty=false;
        }
        return texture;
    }
    public static Vector2i origin(){return new Vector2i(originX,originZ);}
    public static Vector4f parameters(){return new Vector4f(settings.thickness,active?1:0,FLOW.n*FLOW.cell,settings.quality);}
    public static Vector2f wind(){double a=Math.toRadians(settings.windOverride?settings.wind:90);float speed=settings.windOverride?settings.speed:nativeSpeed();return new Vector2f((float)Math.cos(a)*speed,(float)Math.sin(a)*speed);}
        private static float cachedNativeSpeed=5;private static long lastNativeRead=0; private static float nativeSpeed(){long now=System.nanoTime();if(now-lastNativeRead<1_000_000_000L)return cachedNativeSpeed;lastNativeRead=now;
        // Complementary r5.9.3: .0006 / .00012 = 5 blocks per second.
        try{Path iris=Minecraft.getInstance().gameDirectory.toPath().resolve("config/iris.properties");java.util.Properties props=new java.util.Properties();try(var r=java.nio.file.Files.newBufferedReader(iris)){props.load(r);}Path sidecar=Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks").resolve(props.getProperty("shaderPack","")+".txt");java.util.Properties options=new java.util.Properties();if(java.nio.file.Files.exists(sidecar))try(var r=java.nio.file.Files.newBufferedReader(sidecar)){options.load(r);}FLOW.mountainWind=option(options,"RIDGE_MOUNTAIN_WIND",1,0,3);FLOW.windRise=option(options,"RIDGE_WIND_RISE",160,32,512);cachedNativeSpeed=5f*Float.parseFloat(options.getProperty("CLOUD_SPEED_MULT","100"))/100f;return cachedNativeSpeed;}catch(Exception e){return 5f;}
    }
    private static float option(java.util.Properties options,String name,float fallback,float min,float max){try{float v=Float.parseFloat(options.getProperty(name,Float.toString(fallback)));return Float.isFinite(v)?Math.clamp(v,min,max):fallback;}catch(Exception e){return fallback;}}
    public static int motionTexture(){texture();return motionTexture;}
    public static float time(){return (float)FLOW.time;}
    public static Vector2f bounds(){return new Vector2f(lower,upper);}
    public static float scale(){return settings.scale;}
    public static int noiseTexture(){if(noiseTexture==0){noiseTexture=GL33.glGenTextures();int size=64;FloatBuffer data=BufferUtils.createFloatBuffer(size*size*size);for(int z=0;z<size;z++)for(int y=0;y<size;y++)for(int x=0;x<size;x++){float a=fract(x*.1031f),b=fract(y*.1031f),c=fract(z*.1031f),d=a*(b+33.33f)+b*(c+33.33f)+c*(a+33.33f);a+=d;b+=d;c+=d;data.put(fract((a+b)*c));}data.flip();TextureUploader.uploadVolume(noiseTexture,size,data);}return noiseTexture;}
    private static float fract(float v){return v-(float)Math.floor(v);}
    public static void close(){if(motionTexture!=0){GL33.glDeleteTextures(motionTexture);motionTexture=0;motionAllocated=false;}if(texture!=0){GL33.glDeleteTextures(texture);texture=0;}if(noiseTexture!=0){GL33.glDeleteTextures(noiseTexture);noiseTexture=0;}}
}

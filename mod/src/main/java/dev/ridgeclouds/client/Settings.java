package dev.ridgeclouds.client;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
public final class Settings {
    public boolean enabled=true;
    public boolean windOverride=false,densityOverride=false;
    public float wind=90,speed=5,altitude=190,density=1f,thickness=16,evaporation=85;
    public int quality=96;
    public float scale=32;
    public static Settings read(Path file){try{if(Files.exists(file)){Settings s=new Gson().fromJson(Files.readString(file),Settings.class);if(s!=null){s.wind=finite(s.wind,90,0,360);s.speed=finite(s.speed,18,0,45);s.altitude=finite(s.altitude,190,-64,500);s.density=finite(s.density,.8f,0,1.8f);s.thickness=finite(s.thickness,48,15,100);s.evaporation=finite(s.evaporation,85,30,200);s.quality=Math.clamp(s.quality,32,144);s.scale=finite(s.scale,32,8,160);return s;}}else{Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(new Settings()));}}catch(Exception e){System.err.println("[Ridge Clouds] Config: "+e.getMessage());}return new Settings();}
    private static float finite(float v,float fallback,float min,float max){return Float.isFinite(v)?Math.clamp(v,min,max):fallback;}
}

import dev.ridgeclouds.core.FlowField;
public class MountainWindCheck{
 public static void main(String[]args){
  FlowField low=new FlowField(16,16),high=new FlowField(16,16);
  for(int k=0;k<256;k++){low.terrain[k]=64;high.terrain[k]=224;low.validity[k]=high.validity[k]=1;}
  low.speed=high.speed=5;low.direction=high.direction=90;low.reset();high.reset();
  if(low.windMultiplier(100)!=1||high.windMultiplier(100)!=2)throw new AssertionError("Elevation gain");
  for(int i=0;i<20;i++){low.step(.05f);high.step(.05f);}
  if(low.driftZ[120]!=0||high.driftZ[120]<4.9f)throw new AssertionError("Visible terrain drift");
  high.mountainWind=0;high.reset();high.step(.05f);if(high.driftZ[120]!=0)throw new AssertionError("Disabled gain");
  high.mountainWind=3;high.windRise=64;for(int i=0;i<100;i++)high.step(.1f);for(float x:high.driftZ)if(!Float.isFinite(x))throw new AssertionError("Finite drift");
  float v=high.driftZ[121];high.shift(1,0);if(high.driftZ[120]!=v)throw new AssertionError("Recenter keeps motion");
  System.out.println("PASS sea-level baseline, 2x summit wind, visible drift, disabled gain, extreme settings and recentering");
 }
}

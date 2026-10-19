import dev.ridgeclouds.core.FlowField;

/** Standalone regression checks for the actual Java simulation shipped in the mod. */
public final class FlowFieldStreamingTest {
    private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    private static void settle(FlowField flow){for(int i=0;i<600;i++)flow.step(1f/30);}
    public static void main(String[] args){
        FlowField flow=new FlowField(16,16);flow.speed=0;
        for(int k=0;k<256;k++)flow.observeTerrain(k,64);
        flow.recomputeLee();settle(flow);
        int peak=8*16+8;
        float height=flow.terrain[peak],cloud=flow.centre[peak];
        flow.observeTerrain(peak,320);flow.recomputeLee();
        check(flow.terrain[peak]==height&&flow.centre[peak]==cloud,"Chunk observation must not move rendered terrain/clouds");
        for(int i=0;i<300;i++){
            float h=flow.terrain[peak],c=flow.centre[peak];flow.step(1f/30);
            check(Math.abs(flow.terrain[peak]-h)<=32f/30+.001f,"Terrain must have bounded per-frame motion");
            check(Math.abs(flow.centre[peak]-c)<5,"New peak must not snap the cloud layer upward");
            check(Float.isFinite(flow.centre[peak]),"Cloud height must remain finite");
        }
        check(Math.abs(flow.terrain[peak]-320)<.01,"Smoothed terrain must converge to actual peak");
        float cached=flow.terrain[peak];settle(flow); // no observations: simulate chunk unload
        check(flow.terrain[peak]==cached&&flow.validity[peak]==1,"Unloaded terrain must remain cached");
        check(!flow.observeTerrain(peak,320),"Reloading unchanged terrain must not invalidate lee field");

        int lee=10*16+8;flow.recomputeLee();settle(flow);
        float drop=flow.drop[lee];check(drop>200,"Peak must produce a downstream dry slope");
        flow.shift(1,1);int shiftedPeak=7*16+7,shiftedLee=9*16+7;
        check(flow.terrain[shiftedPeak]==320,"Grid recenter must preserve world-space heights");
        check(flow.drop[shiftedLee]==drop,"Grid recenter must transport lee-side field with terrain");
        flow.step(1f/30);
        check(flow.terrain[shiftedPeak]==320&&flow.drop[shiftedLee]==drop,"Targets must also survive recentering");
        check(flow.validity[255]==0&&flow.drop[255]==0,"New grid cells must not inherit stale lee data");
        flow.clearTerrain();
        check(flow.validity[shiftedPeak]==0&&flow.terrain[shiftedPeak]==0,"World change must clear cached terrain");
        flow.step(1f/30);check(flow.validity[shiftedPeak]==0,"Cleared world must not restore old observations");
        System.out.println("PASS: streaming peaks, bounded cloud motion, cached unload/reload, grid shifts, world reset");
    }
}

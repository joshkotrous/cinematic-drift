package dev.ridgeclouds.core;

/** Independent heightfield flow model; no Minecraft, Iris or GPU types. */
public final class FlowField {
    public final int n; public final float cell;
    public final float[] terrain, validity, drop;
    private final float[] terrainTarget, dropTarget;
    private final boolean[] known;
    private static final float TERRAIN_RATE=32f, LEE_RATE=64f;
    public float[] centre, cover; private float[] nextCentre, nextCover;
    public float altitude=190, density=.8f, speed=18, direction=90,evaporation=85;
    public double time; public float mountainWind=1,windRise=160; public float[] driftX,driftZ; private float[] nextDriftX,nextDriftZ;
    public float windMultiplier(int k){float h=Math.clamp((terrain[k]-64)/Math.max(32,windRise),0,1);return 1+mountainWind*h*h*(3-2*h);}
    public FlowField(int n,float cell){this.n=n;this.cell=cell;int size=n*n;terrain=new float[size];validity=new float[size];drop=new float[size];terrainTarget=new float[size];dropTarget=new float[size];known=new boolean[size];centre=new float[size];cover=new float[size];nextCentre=new float[size];nextCover=new float[size];driftX=new float[size];driftZ=new float[size];nextDriftX=new float[size];nextDriftZ=new float[size];reset();}
    /** A loaded chunk updates the target only; never move visible clouds on a client tick. */
    public boolean observeTerrain(int k,float height){
        if(!Float.isFinite(height))return false;
        boolean changed=!known[k]||terrainTarget[k]!=height;
        terrainTarget[k]=height;known[k]=true;return changed;
    }
    /** Only a world/dimension change invalidates cached terrain. Chunk unloads do not. */
    public void clearTerrain(){
        java.util.Arrays.fill(terrain,0);java.util.Arrays.fill(terrainTarget,0);
        java.util.Arrays.fill(validity,0);java.util.Arrays.fill(known,false);
        java.util.Arrays.fill(drop,0);java.util.Arrays.fill(dropTarget,0);reset();
    }
    private static float approach(float current,float target,float maximum){return current+Math.clamp(target-current,-maximum,maximum);}
    private void settleTerrain(float dt){
        for(int k=0;k<n*n;k++){
            if(known[k]){terrain[k]=approach(terrain[k],terrainTarget[k],TERRAIN_RATE*dt);validity[k]=Math.min(1,validity[k]+dt*.5f);}
            drop[k]=approach(drop[k],dropTarget[k],LEE_RATE*dt);
        }
    }
    public void reset(){for(int k=0;k<n*n;k++){centre[k]=Math.max(altitude,terrain[k]+24);cover[k]=density;driftX[k]=0;driftZ[k]=0;}time=0;}
    private float sample(float[] a,float x,float z){x=Math.clamp(x,0,n-1.001f);z=Math.clamp(z,0,n-1.001f);int i=(int)x,j=(int)z,k=j*n+i;float fx=x-i,fz=z-j;return (a[k]*(1-fx)+a[k+1]*fx)*(1-fz)+(a[k+n]*(1-fx)+a[k+n+1]*fx)*fz;}
    public void recomputeLee(){double a=Math.toRadians(direction);float wx=(float)Math.cos(a),wz=(float)Math.sin(a);for(int j=0;j<n;j++)for(int i=0;i<n;i++){int k=j*n+i;if(!known[k]){dropTarget[k]=0;continue;}float crest=terrainTarget[k];for(int d=1;d<n;d++){float x=i-wx*d,z=j-wz*d;if(x<0||z<0||x>n-1||z>n-1)break;int upstream=(int)z*n+(int)x;if(known[upstream])crest=Math.max(crest,terrainTarget[upstream]);}dropTarget[k]=known[k]?Math.max(0,crest-terrainTarget[k]-12):0;}}
    public void step(float dt){dt=Math.clamp(dt,0,.1f);settleTerrain(dt);time+=dt;double a=Math.toRadians(direction);float wx=(float)Math.cos(a)*speed,wz=(float)Math.sin(a)*speed;
        for(int j=0;j<n;j++)for(int i=0;i<n;i++){int k=j*n+i;float dx=(terrain[j*n+Math.min(n-1,i+1)]-terrain[j*n+Math.max(0,i-1)])/(2*cell),dz=(terrain[Math.min(n-1,j+1)*n+i]-terrain[Math.max(0,j-1)*n+i])/(2*cell);float boost=windMultiplier(k),localX=wx*boost,localZ=wz*boost;float up=Math.max(0,localX*dx+localZ*dz),g=dx*dx+dz*dz,collision=Math.clamp((terrain[k]+60-altitude)/90,0,1);float vx=localX-up*dx/(g+.2f)*collision*.85f,vz=localZ-up*dz/(g+.2f)*collision*.85f;float x=i-vx*dt/cell,z=j-vz*dt/cell;boolean outside=x<0||z<0||x>n-1||z>n-1;nextDriftX[k]=(outside?0:sample(driftX,x,z))+wx*(boost-1)*dt;nextDriftZ[k]=(outside?0:sample(driftZ,x,z))+wz*(boost-1)*dt;float target=Math.max(altitude-drop[k]*.65f,terrain[k]+28),old=outside?altitude:sample(centre,x,z),rate=target>old?3:drop[k]>12?1.1f:.24f;nextCentre[k]=Math.max(terrain[k]+12,old+(target-old)*(1-(float)Math.exp(-rate*dt)));float wet=(float)Math.exp(-Math.pow(drop[k]/evaporation,2));float influx=density*wet,c=outside?influx:sample(cover,x,z);nextCover[k]=Math.max(0,c+(influx-c)*(1-(float)Math.exp(-(.08+(1-wet)*1.3)*dt)));}
        float[] swap=driftX;driftX=nextDriftX;nextDriftX=swap;swap=driftZ;driftZ=nextDriftZ;nextDriftZ=swap;swap=centre;centre=nextCentre;nextCentre=swap;swap=cover;cover=nextCover;nextCover=swap;
    }
    /** Recenter without resetting surviving weather. dx/dz are grid-cell origin shifts. */
    public void shift(int dx,int dz){float[] h=terrain.clone(),v=validity.clone(),c=centre.clone(),d=cover.clone(),mx=driftX.clone(),mz=driftZ.clone(),ht=terrainTarget.clone(),d0=drop.clone(),dt0=dropTarget.clone();boolean[] known0=known.clone();for(int j=0;j<n;j++)for(int i=0;i<n;i++){int k=j*n+i,x=i+dx,z=j+dz;if(x>=0&&z>=0&&x<n&&z<n){int s=z*n+x;terrain[k]=h[s];terrainTarget[k]=ht[s];known[k]=known0[s];drop[k]=d0[s];dropTarget[k]=dt0[s];validity[k]=v[s];centre[k]=c[s];cover[k]=d[s];driftX[k]=mx[s];driftZ[k]=mz[s];}else{terrain[k]=0;terrainTarget[k]=0;known[k]=false;drop[k]=0;dropTarget[k]=0;validity[k]=0;centre[k]=altitude;cover[k]=density;driftX[k]=0;driftZ[k]=0;}}}
}

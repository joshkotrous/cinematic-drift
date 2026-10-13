#define RIDGE_DENSITY_MULT 108 //[75 85 95 100 105 108 110 115 120 130 150]
#define RIDGE_MOUNTAIN_WIND 1.0 //[0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define RIDGE_WIND_RISE 160.0 //[64.0 96.0 128.0 160.0 192.0 256.0 320.0]
#define GetVolumetricClouds RidgeOriginalVolumetricClouds
#define GetCloudNoise RidgeOriginalCloudNoise
#include "/lib/atmospherics/clouds/unboundClouds.glsl"
#undef GetVolumetricClouds
#undef GetCloudNoise
// Ridge Clouds. Runs inside Complementary's cloud stage, before its post effects.
uniform sampler2D ridgeField;
 uniform sampler2D ridgeMotion;
uniform ivec2 ridgeOrigin;
uniform vec4 ridgeParams; // thickness, active, extent, ray samples
uniform vec2 ridgeWind;
uniform float ridgeTime;
uniform sampler3D ridgeNoise;
uniform vec2 ridgeBounds;
uniform float ridgeScale;

float RidgeHash(vec3 p) { p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z); }
float RidgeNoise(vec3 p) {
    return texture3D(ridgeNoise,(p+.5)/64.0).r;
}
float RidgeDensity(vec3 p) {
    vec2 uv=p.xz/ridgeParams.z;
    if(any(lessThan(uv,vec2(0)))||any(greaterThan(uv,vec2(1))))return 0.0;
    vec4 f=texture2D(ridgeField,uv);
    if(f.a<.9||f.b<.04||abs(p.y-f.g)>ridgeParams.x*1.6)return 0.0;
    float known=smoothstep(.9,1.0,f.a);
    float border=smoothstep(0.0,64.0,min(min(p.x,p.z),min(ridgeParams.z-p.x,ridgeParams.z-p.z)));
    float edge=smoothstep(0.0,22.0,p.y-f.r);
    // Evaluate the original pack's density and weather response at a terrain-warped height.
    vec3 originalPos=vec3(p.x+float(ridgeOrigin.x % 16384),
        float(cloudAlt1i)+(p.y-f.g)*cloudStretch/ridgeParams.x,
        p.z+float(ridgeOrigin.y % 16384));
    originalPos.xz-=texture2D(ridgeMotion,uv).rg*step(0.0001,float(RIDGE_MOUNTAIN_WIND))*step(0.0,float(RIDGE_WIND_RISE));
    float nativeDensity=RidgeOriginalCloudNoise(originalPos,cloudAlt1i,0.0,0.0);
    return clamp(nativeDensity*8.0*float(RIDGE_DENSITY_MULT)*0.01,0.0,1.0)*f.b*edge*known*border;
}
vec4 GetVolumetricClouds(int cloudAltitude,float distanceThreshold,inout float cloudLinearDepth,float skyFade,float skyMult0,vec3 cameraPos,vec3 nPlayerPos,float lViewPosM,float VdotS,float VdotU,float dither) {
    if(ridgeParams.y<.5||isEyeInWater!=0)return vec4(0.0);
    vec3 rd=nPlayerPos;
    // Safe slab intersection, including horizontal rays and cameras inside cloud.
    vec3 safeDir=mix(rd,vec3(.00001),lessThan(abs(rd),vec3(.00001)));
    vec3 a=(vec3(0,ridgeBounds.x,0)-cameraPos)/safeDir,b=(vec3(ridgeParams.z,ridgeBounds.y,ridgeParams.z)-cameraPos)/safeDir;
    vec3 lo=min(a,b),hi=max(a,b);
    float start=max(0.0,max(max(lo.x,lo.y),lo.z));
    float end=min(min(min(hi.x,hi.y),hi.z),min(distanceThreshold,lViewPosM));
    if(end<=start)return vec4(0.0);
    int count=min(int(ridgeParams.w),max(12,int(ceil((end-start)/8.0))));
    #ifndef DEFERRED1
        count=min(count,24);
    #endif
    float stepLength=(end-start)/float(count);
    vec3 accum=vec3(0.0);float trans=1.0,first=0.0;
    vec3 skyColor=GetSky(VdotU,VdotS,dither,true,false);
    #ifdef ATM_COLOR_MULTS
        skyColor*=sqrtAtmColorMult;
    #endif
    vec3 sunDirection=normalize((gbufferModelViewInverse*vec4(sunVec,0)).xyz);
    sunDirection*=sunVisibility>.5?1.0:-1.0;
    float phase=.35+.65*pow(max(0.0,sunVisibility>.5?VdotS:-VdotS),6.0);
    for(int i=0;i<144;i++) {
        if(i>=count)break;
        float t=start+(float(i)+dither)*stepLength;
        vec3 p=cameraPos+rd*t;float den=RidgeDensity(p);
        if(den>.005){
            if(first==0.0)first=t;
            float shadow=0.0;
            shadow=RidgeDensity(p+sunDirection*24.0)+RidgeDensity(p+sunDirection*64.0);
            float lighting=exp(-shadow*1.8);
            vec3 lit=cloudAmbientColor*.6+cloudLightColor*lighting*(.65+phase);
            lit=mix(lit,skyColor,1.0-exp(-t*.00015));
            lit*=pow2(1.0-maxBlindnessDarkness);
            float alpha=1.0-exp(-den*stepLength*.027);
            accum+=trans*alpha*lit;trans*=1.0-alpha;
            if(trans<.012)break;
        }
    }
    float opacity=1.0-trans;
    if(opacity>.5)cloudLinearDepth=sqrt(first/renderDistance);
    return vec4(accum/max(opacity,.00001),opacity);
}

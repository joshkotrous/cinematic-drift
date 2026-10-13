#version 300 es
precision highp float;
out vec4 color;
uniform sampler2D field;
uniform vec2 resolution;
uniform vec3 eye,target;
uniform float time,extent,windAngle,windSpeed,thickness;
uniform int steps,enabled,showFlow;
vec3 sun=normalize(vec3(-.55,.65,-.3));
float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
vec4 data(vec2 p){vec2 size=vec2(textureSize(field,0));vec2 q=clamp((p/extent+.5)*size-.5,vec2(0),size-1.001);ivec2 i=ivec2(floor(q));vec2 f=fract(q);return mix(mix(texelFetch(field,i,0),texelFetch(field,i+ivec2(1,0),0),f.x),mix(texelFetch(field,i+ivec2(0,1),0),texelFetch(field,i+ivec2(1,1),0),f.x),f.y);}
float height(vec2 p){return data(p).r;}
float density(vec3 p){vec2 uv=p.xz/extent+.5;if(any(lessThan(uv,vec2(0)))||any(greaterThan(uv,vec2(1))))return 0.;vec4 f=data(p.xz);float edge=smoothstep(0.,22.,p.y-f.r);float layer=exp(-pow((p.y-f.g)/thickness,2.)*2.);vec3 q=vec3(p.x,p.y-f.g,p.z)*.014-vec3(cos(windAngle),0,sin(windAngle))*time*windSpeed*.014;float n=noise(q)*.6+noise(q*2.03)*.28+noise(q*4.1)*.12;float threshold=mix(.62,.29,smoothstep(0.,.6,f.b));return max(0.,n-threshold)*2.6*layer*f.b*edge;}
vec3 sky(vec3 rd){vec3 c=mix(vec3(.62,.74,.8),vec3(.19,.34,.49),smoothstep(-.1,.8,rd.y));return c+vec3(1.,.78,.52)*pow(max(0.,dot(rd,sun)),190.)*.8;}
void main(){vec2 uv=(gl_FragCoord.xy-.5*resolution)/resolution.y;vec3 f=normalize(target-eye),r=normalize(cross(f,vec3(0,1,0))),u=cross(r,f),rd=normalize(f*1.65+r*uv.x+u*uv.y);vec3 bg=sky(rd);float hit=3000.,dist=8.;vec3 p;
 for(int i=0;i<180;i++){p=eye+rd*dist;if(abs(p.x)<extent*.5&&abs(p.z)<extent*.5){float d=p.y-height(p.xz);if(d<1.2){hit=dist;break;}dist+=clamp(d*.45,3.,35.);}else dist+=25.;if(dist>3000.)break;}
 if(hit<3000.){p=eye+rd*hit;float h=height(p.xz);vec3 normal=normalize(vec3(height(p.xz-vec2(4,0))-height(p.xz+vec2(4,0)),8.,height(p.xz-vec2(0,4))-height(p.xz+vec2(0,4))));float rock=smoothstep(.55,.85,1.-normal.y);vec3 albedo=mix(vec3(.17,.25,.2),vec3(.31,.32,.3),rock);albedo=mix(albedo,vec3(.73,.78,.77),smoothstep(265.,340.,h)*smoothstep(.45,.85,normal.y));float detail=.85+.15*noise(p*.12);bg=albedo*detail*(.4+max(0.,dot(normal,sun))*.9);bg=mix(bg,sky(rd),1.-exp(-hit*.00036));if(showFlow==1){float a=atan(sin(windAngle),cos(windAngle));vec2 q=mat2(cos(a),-sin(a),sin(a),cos(a))*p.xz;float line=step(.95,fract(q.y/60.))*step(.5,fract((q.x-time*windSpeed)/120.));bg=mix(bg,vec3(.35,.85,.7),line*.7);}}
 vec3 accum=vec3(0);float trans=1.;if(enabled==1){float maxDist=min(hit,2300.);float stepLen=maxDist/float(steps);float jitter=hash(vec3(gl_FragCoord.xy,fract(time)));for(int i=0;i<160;i++){if(i>=steps)break;float t=(float(i)+jitter)*stepLen;vec3 c=eye+rd*t;if(c.y<0.||c.y>550.)continue;float den=density(c);if(den>.005){float shadow=0.;for(int j=1;j<=4;j++)shadow+=density(c+sun*float(j)*26.);float light=exp(-shadow*1.2);float phase=.35+.65*pow(max(0.,dot(rd,sun)),6.);vec3 lit=vec3(.4,.52,.61)*.6+vec3(1.,.9,.75)*light*(.65+phase);lit=mix(lit,sky(rd),1.-exp(-t*.0004));float alpha=1.-exp(-den*stepLen*.027);accum+=trans*alpha*lit;trans*=1.-alpha;if(trans<.012)break;}}}
 vec3 result=accum+trans*bg;result=pow(result,vec3(1./2.2));color=vec4(result,1.);
}

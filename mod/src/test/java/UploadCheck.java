import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import dev.ridgeclouds.client.TextureUploader;
public class UploadCheck{
 public static void main(String[]args){
  if(!GLFW.glfwInit())throw new AssertionError("GLFW");GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,0);long w=GLFW.glfwCreateWindow(32,32,"Texture regression",0,0);GLFW.glfwMakeContextCurrent(w);GL.createCapabilities();
  int texture=GL33.glGenTextures(),previous=GL33.glGenTextures(),pbo=GL33.glGenBuffers();GL33.glBindTexture(GL33.GL_TEXTURE_2D,previous);GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER,pbo);GL33.glBufferData(GL33.GL_PIXEL_UNPACK_BUFFER,16,GL33.GL_STREAM_DRAW);
  int[] keys={GL33.GL_UNPACK_ALIGNMENT,GL33.GL_UNPACK_ROW_LENGTH,GL33.GL_UNPACK_SKIP_ROWS,GL33.GL_UNPACK_SKIP_PIXELS,GL33.GL_UNPACK_IMAGE_HEIGHT,GL33.GL_UNPACK_SKIP_IMAGES,GL33.GL_UNPACK_SWAP_BYTES,GL33.GL_UNPACK_LSB_FIRST};
  int[] poison={8,4096,17,31,4096,3,1,1};for(int i=0;i<keys.length;i++)GL33.glPixelStorei(keys[i],poison[i]);
  int count=96*96*4;FloatBuffer pixels=BufferUtils.createFloatBuffer(count),read=BufferUtils.createFloatBuffer(count);
  for(int frame=0;frame<100;frame++){
   pixels.clear();for(int k=0;k<count;k++)pixels.put(k*.01f+frame);pixels.flip();TextureUploader.upload(texture,96,pixels,frame==0);
   if(GL33.glGetInteger(GL33.GL_PIXEL_UNPACK_BUFFER_BINDING)!=pbo||GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D)!=previous)throw new AssertionError("Binding restoration");for(int i=0;i<keys.length;i++)if(GL33.glGetInteger(keys[i])!=poison[i])throw new AssertionError("Unpack restoration "+keys[i]);
   GL33.glBindTexture(GL33.GL_TEXTURE_2D,texture);read.clear();GL33.glGetTexImage(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA,GL33.GL_FLOAT,read);for(int k=0;k<count;k++)if(read.get(k)!=k*.01f+frame)throw new AssertionError("Texture mismatch "+k);GL33.glBindTexture(GL33.GL_TEXTURE_2D,previous);
   if(GL33.glGetError()!=0)throw new AssertionError("GL error");
  }
  int vol=GL33.glGenTextures();FloatBuffer cube=BufferUtils.createFloatBuffer(16*16*16);for(int k=0;k<16*16*16;k++)cube.put(k*.001f);cube.flip();TextureUploader.uploadVolume(vol,16,cube);if(GL33.glGetInteger(GL33.GL_PIXEL_UNPACK_BUFFER_BINDING)!=pbo)throw new AssertionError("Volume PBO restoration");for(int i=0;i<keys.length;i++)if(GL33.glGetInteger(keys[i])!=poison[i])throw new AssertionError("Volume unpack restoration");GL33.glBindTexture(GL33.GL_TEXTURE_3D,vol);FloatBuffer readCube=BufferUtils.createFloatBuffer(16*16*16);GL33.glGetTexImage(GL33.GL_TEXTURE_3D,0,GL33.GL_RED,GL33.GL_FLOAT,readCube);for(int k=0;k<16*16*16;k++)if(readCube.get(k)!=k*.001f)throw new AssertionError("Noise upload mismatch");if(GL33.glGetError()!=0)throw new AssertionError("Volume GL error");
System.out.println("PASS noise volume upload and 100 cloud uploads with poisoned row/skip/byte/PBO state; exact GPU roundtrip and state restoration");GLFW.glfwDestroyWindow(w);GLFW.glfwTerminate();
 }
}

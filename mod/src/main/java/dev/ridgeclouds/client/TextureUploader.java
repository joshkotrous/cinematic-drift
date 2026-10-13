package dev.ridgeclouds.client;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL33;

/** Client-memory transfers must not inherit atlas/PBO pixel-unpack state. */
public final class TextureUploader {
    private static final int[] UNPACK={GL33.GL_UNPACK_ALIGNMENT,GL33.GL_UNPACK_ROW_LENGTH,
        GL33.GL_UNPACK_SKIP_ROWS,GL33.GL_UNPACK_SKIP_PIXELS,GL33.GL_UNPACK_IMAGE_HEIGHT,
        GL33.GL_UNPACK_SKIP_IMAGES,GL33.GL_UNPACK_SWAP_BYTES,GL33.GL_UNPACK_LSB_FIRST};
    public static void upload(int texture,int size,FloatBuffer pixels,boolean allocate){
        if(!pixels.isDirect()||pixels.remaining()!=size*size*4)throw new IllegalArgumentException("Cloud upload requires a complete direct RGBA field");
        int previousTexture=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        int previousPbo=GL33.glGetInteger(GL33.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] previous=new int[UNPACK.length];
        for(int i=0;i<UNPACK.length;i++)previous[i]=GL33.glGetInteger(UNPACK[i]);
        try{
            GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER,0);
            for(int i=0;i<UNPACK.length;i++)GL33.glPixelStorei(UNPACK[i],i==0?4:0);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D,texture);
            if(allocate){
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_S,GL33.GL_CLAMP_TO_EDGE);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_T,GL33.GL_CLAMP_TO_EDGE);
                GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,size,size,0,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);
            }else GL33.glTexSubImage2D(GL33.GL_TEXTURE_2D,0,0,0,size,size,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);
        }finally{
            GL33.glBindTexture(GL33.GL_TEXTURE_2D,previousTexture);
            for(int i=0;i<UNPACK.length;i++)GL33.glPixelStorei(UNPACK[i],previous[i]);
            GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER,previousPbo);
        }
    }
    public static void uploadVolume(int texture,int size,FloatBuffer pixels){
        if(!pixels.isDirect()||pixels.remaining()!=size*size*size)throw new IllegalArgumentException("Complete noise volume required");
        int binding=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_3D),pbo=GL33.glGetInteger(GL33.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] state=new int[UNPACK.length];for(int i=0;i<state.length;i++)state[i]=GL33.glGetInteger(UNPACK[i]);
        try{GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER,0);for(int i=0;i<state.length;i++)GL33.glPixelStorei(UNPACK[i],i==0?4:0);
            GL33.glBindTexture(GL33.GL_TEXTURE_3D,texture);GL33.glTexParameteri(GL33.GL_TEXTURE_3D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);GL33.glTexParameteri(GL33.GL_TEXTURE_3D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
            GL33.glTexParameteri(GL33.GL_TEXTURE_3D,GL33.GL_TEXTURE_WRAP_S,GL33.GL_REPEAT);GL33.glTexParameteri(GL33.GL_TEXTURE_3D,GL33.GL_TEXTURE_WRAP_T,GL33.GL_REPEAT);GL33.glTexParameteri(GL33.GL_TEXTURE_3D,GL33.GL_TEXTURE_WRAP_R,GL33.GL_REPEAT);
            GL33.glTexImage3D(GL33.GL_TEXTURE_3D,0,GL33.GL_R32F,size,size,size,0,GL33.GL_RED,GL33.GL_FLOAT,pixels);
        }finally{GL33.glBindTexture(GL33.GL_TEXTURE_3D,binding);for(int i=0;i<state.length;i++)GL33.glPixelStorei(UNPACK[i],state[i]);GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER,pbo);}
    }
}

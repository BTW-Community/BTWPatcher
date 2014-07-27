package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.RenderUtils;
import org.lwjgl.opengl.GL11;

abstract public class GLAPI {
    private static final GLAPI instance = MAL.newInstance(GLAPI.class, "glwrapper");

    public static <T> T select(T v1, T v2) {
        return instance.select_Impl(v1, v2);
    }

    public static void glBindTexture(int texture) {
        if (texture >= 0) {
            instance.glBindTexture_Impl(texture);
        }
    }

    public static void glBlendFunc(int src, int dst) {
        instance.glBlendFunc_Impl(src, dst);
    }

    public static void glClearColor(float r, float g, float b, float a) {
        instance.glClearColor_Impl(r, g, b, a);
    }

    public static int getBoundTexture() {
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    public static void deleteTexture(int texture) {
        if (texture >= 0) {
            GL11.glDeleteTextures(texture);
        }
    }

    abstract protected <T> T select_Impl(T v1, T v2);

    abstract protected void glBindTexture_Impl(int texture);

    abstract protected void glBlendFunc_Impl(int src, int dst);

    abstract protected void glClearColor_Impl(float r, float g, float b, float a);

    private static final class V1 extends GLAPI {
        @Override
        protected <T> T select_Impl(T v1, T v2) {
            return v1;
        }

        @Override
        protected void glBindTexture_Impl(int texture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }

        @Override
        protected void glBlendFunc_Impl(int src, int dst) {
            GL11.glBlendFunc(src, dst);
        }

        @Override
        protected void glClearColor_Impl(float r, float g, float b, float a) {
            GL11.glClearColor(r, g, b, a);
        }
    }

    private static final class V2 extends GLAPI {
        @Override
        protected <T> T select_Impl(T v1, T v2) {
            return v2;
        }

        @Override
        protected void glBindTexture_Impl(int texture) {
            RenderUtils.glBindTexture(texture);
        }

        @Override
        protected void glBlendFunc_Impl(int src, int dst) {
            RenderUtils.glBlendFunc(src, dst);
        }

        @Override
        protected void glClearColor_Impl(float r, float g, float b, float a) {
            RenderUtils.glClearColor(r, g, b, a);
        }
    }
}

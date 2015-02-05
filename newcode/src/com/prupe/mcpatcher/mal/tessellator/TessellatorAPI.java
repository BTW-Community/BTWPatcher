package com.prupe.mcpatcher.mal.tessellator;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TessellatorFactory;
import net.minecraft.src.VertexFormats;

abstract public class TessellatorAPI {
    private static final TessellatorAPI instance = MAL.newInstance(TessellatorAPI.class, "tessellator");

    public static Tessellator getTessellator() {
        return instance.getTessellator_Impl();
    }

    public static void startDrawingQuads(Tessellator tessellator) {
        instance.startDrawingQuads_Impl(tessellator);
    }

    public static void addVertexWithUV(Tessellator tessellator, double x, double y, double z, double u, double v) {
        instance.addVertexWithUV_Impl(tessellator, x, y, z, u, v);
    }

    public static void setColorOpaque_F(Tessellator tessellator, float r, float g, float b) {
        instance.setColorOpaque_F_Impl(tessellator, r, g, b);
    }

    public static void draw(Tessellator tessellator) {
        instance.draw_Impl(tessellator);
    }

    abstract protected Tessellator getTessellator_Impl();

    abstract protected void startDrawingQuads_Impl(Tessellator tessellator);

    abstract protected void addVertexWithUV_Impl(Tessellator tessellator, double x, double y, double z, double u, double v);

    abstract protected void setColorOpaque_F_Impl(Tessellator tessellator, float r, float g, float b);

    abstract protected void draw_Impl(Tessellator tessellator);

    final private static class V1 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return Tessellator.instance;
        }

        @Override
        protected void startDrawingQuads_Impl(Tessellator tessellator) {
            tessellator.startDrawingQuads();
        }

        @Override
        protected void addVertexWithUV_Impl(Tessellator tessellator, double x, double y, double z, double u, double v) {
            tessellator.addVertexWithUV(x, y, z, u, v);
        }

        @Override
        protected void setColorOpaque_F_Impl(Tessellator tessellator, float r, float g, float b) {
            tessellator.setColorOpaque_F(r, g, b);
        }

        @Override
        protected void draw_Impl(Tessellator tessellator) {
            tessellator.draw();
        }
    }

    final private static class V2 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return TessellatorFactory.getInstance().getTessellator();
        }

        @Override
        protected void startDrawingQuads_Impl(Tessellator tessellator) {
            tessellator.startDrawingQuads();
        }

        @Override
        protected void addVertexWithUV_Impl(Tessellator tessellator, double x, double y, double z, double u, double v) {
            tessellator.addVertexWithUV(x, y, z, u, v);
        }

        @Override
        protected void setColorOpaque_F_Impl(Tessellator tessellator, float r, float g, float b) {
            tessellator.setColorOpaque_F(r, g, b);
        }

        @Override
        protected void draw_Impl(Tessellator tessellator) {
            TessellatorFactory.getInstance().drawInt();
        }
    }

    final private static class V3 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return TessellatorFactory.getInstance().getTessellator();
        }

        @Override
        protected void startDrawingQuads_Impl(Tessellator tessellator) {
            tessellator.startDrawingQuads();
        }

        @Override
        protected void addVertexWithUV_Impl(Tessellator tessellator, double x, double y, double z, double u, double v) {
            tessellator.addVertexWithUV(x, y, z, u, v);
        }

        @Override
        protected void setColorOpaque_F_Impl(Tessellator tessellator, float r, float g, float b) {
            tessellator.setColorOpaque_F(r, g, b);
        }

        @Override
        protected void draw_Impl(Tessellator tessellator) {
            TessellatorFactory.getInstance().drawVoid();
        }
    }

    final private static class V4 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return TessellatorFactory.getInstance().getTessellator();
        }

        @Override
        protected void startDrawingQuads_Impl(Tessellator tessellator) {
            tessellator.startDrawing(7, VertexFormats.standardQuadFormat);
        }

        @Override
        protected void addVertexWithUV_Impl(Tessellator tessellator, double x, double y, double z, double u, double v) {
            tessellator.addXYZ(x, y, z).addUV(u, v).next();
        }

        @Override
        protected void setColorOpaque_F_Impl(Tessellator tessellator, float r, float g, float b) {
            tessellator.setColorF(r, g, b, 1.0f);
        }

        @Override
        protected void draw_Impl(Tessellator tessellator) {
            TessellatorFactory.getInstance().drawVoid();
        }
    }
}

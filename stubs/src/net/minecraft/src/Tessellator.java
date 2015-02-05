package net.minecraft.src;

public class Tessellator {
    public static Tessellator instance;

    public Tessellator(int bufferSize) {
    }

    public Tessellator() { // forge replaces 1-arg constructor
    }

    public void reset() {
    }

    public int draw() {
        return 0;
    }

    public void startDrawing(int drawMode) {
    }

    public void startDrawingQuads() {
    }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
    }

    public void setColorOpaque_F(float r, float g, float b) {
    }

    // 1.8.2-pre5+
    public void startDrawing(int drawMode, VertexFormat vertexFormat) {
    }

    public Tessellator addXYZ(double x, double y, double z) {
        return this;
    }

    public Tessellator addUV(double u, double v) {
        return this;
    }

    public Tessellator setColorF(float r, float g, float b, float a) {
        return this;
    }

    public void next() {
    }
}

package net.minecraft.src;

import java.util.Map;

public class Tessellator {
    public static Tessellator instance;

    public static boolean convertQuadsToTriangles;

    public int rawBuffer[];
    public int rawBufferIndex;
    public int addedVertices;
    public int vertexCount;
    public int bufferSize;
    public int drawMode;
    public boolean isDrawing;

    // added by ctm
    public TextureAtlas textureMap;
    public Map<TextureAtlas, Tessellator> children;

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

    public void addVertex(double x, double y, double z) {
    }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
    }

    public void setColorOpaque_F(float r, float g, float b) {
    }
}

package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.BlendMethod;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ItemRenderer;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

class ItemOverlay {
    private static final float ITEM_2D_THICKNESS = 0.0625f;

    private String propertiesName;
    private String texture;
    private BlendMethod blendMethod;
    private float rotate;
    private float speed;

    static void beginOuter2D() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_GREATER);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
    }

    static void endOuter2D() {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    static void beginOuter3D() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
    }

    static void endOuter3D() {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    void render2D(Tessellator tessellator, float fade) {
        begin(fade);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        tessellator.addVertexWithUV(0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        tessellator.addVertexWithUV(1.0f, 1.0f, 0.0f, 1.0f, 1.0f);
        tessellator.addVertexWithUV(1.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        tessellator.draw();
        end();
    }

    void render3D(Tessellator tessellator, float fade, int width, int height) {
        begin(fade);
        ItemRenderer.renderItemIn2D(tessellator, 0.0f, 0.0f, 1.0f, 1.0f, width, height, ITEM_2D_THICKNESS);
        end();
    }

    private void begin(float fade) {
        TexturePackAPI.bindTexture(texture);
        blendMethod.applyBlending();
        blendMethod.applyFade(fade);
        GL11.glPushMatrix();
        if (speed != 0.0f) {
            float offset = (float) (System.currentTimeMillis() % 3000L) / 3000.0f * 8.0f;
            GL11.glTranslatef(offset * speed, 0.0f, 0.0f);
        }
        GL11.glRotatef(rotate, 0.0f, 0.0f, 1.0f);
    }

    private void end() {
        GL11.glPopMatrix();
    }

    @Override
    public String toString() {
        return "ItemOverlay{" + propertiesName + ", " + texture + "}";
    }
}

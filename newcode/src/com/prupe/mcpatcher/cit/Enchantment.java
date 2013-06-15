package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.BlendMethod;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ItemRenderer;
import net.minecraft.src.ResourceAddress;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

import java.util.Properties;

final class Enchantment extends OverrideBase {
    private static final float ITEM_2D_THICKNESS = 0.0625f;

    private final BlendMethod blendMethod;
    private final float rotation;
    private final float speed;
    final float duration;

    static void beginOuter2D() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_GREATER);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
    }

    static void endOuter2D() {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
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
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    Enchantment(ResourceAddress propertiesName, Properties properties) {
        super(propertiesName, properties);

        String value = MCPatcherUtils.getStringProperty(properties, "blend", "add");
        blendMethod = BlendMethod.parse(value);
        if (blendMethod == null) {
            error("unknown blend type %s", value);
        }
        rotation = MCPatcherUtils.getFloatProperty(properties, "rotation", 0.0f);
        speed = MCPatcherUtils.getFloatProperty(properties, "speed", 0.0f);
        duration = MCPatcherUtils.getFloatProperty(properties, "duration", 1.0f);
    }

    @Override
    String getType() {
        return "enchantment";
    }

    void render2D(Tessellator tessellator, float intensity, float x0, float y0, float x1, float y1, float z) {
        if (intensity <= 0.0f) {
            return;
        }
        if (intensity > 1.0f) {
            intensity = 1.0f;
        }
        begin(intensity);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x0, y0, z, 0.0f, 0.0f);
        tessellator.addVertexWithUV(x0, y1, z, 0.0f, 1.0f);
        tessellator.addVertexWithUV(x1, y1, z, 1.0f, 1.0f);
        tessellator.addVertexWithUV(x1, y0, z, 1.0f, 0.0f);
        tessellator.draw();
        end();
    }

    void render3D(Tessellator tessellator, float intensity, int width, int height) {
        if (intensity <= 0.0f) {
            return;
        }
        if (intensity > 1.0f) {
            intensity = 1.0f;
        }
        begin(intensity);
        ItemRenderer.renderItemIn2D(tessellator, 0.0f, 0.0f, 1.0f, 1.0f, width, height, ITEM_2D_THICKNESS);
        end();
    }

    void beginArmor(float intensity) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        begin(intensity);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    void endArmor() {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        end();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void begin(float intensity) {
        TexturePackAPI.bindTexture(textureName);
        blendMethod.applyBlending();
        blendMethod.applyFade(intensity);
        GL11.glPushMatrix();
        if (speed != 0.0f) {
            float offset = (float) (System.currentTimeMillis() % 3000L) / 3000.0f * 8.0f;
            GL11.glTranslatef(offset * speed, 0.0f, 0.0f);
        }
        GL11.glRotatef(rotation, 0.0f, 0.0f, 1.0f);
    }

    private void end() {
        GL11.glPopMatrix();
    }
}

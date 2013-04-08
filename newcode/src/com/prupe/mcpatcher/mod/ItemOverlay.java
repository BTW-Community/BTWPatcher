package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.BlendMethod;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ItemRenderer;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

import java.util.Properties;

class ItemOverlay {
    static final int AVERAGE = 0;
    static final int CYCLE = 1;
    static final int TOP = 2;

    private static final float ITEM_2D_THICKNESS = 0.0625f;

    private final String texture;
    private final BlendMethod blendMethod;
    private final float rotation;
    private final float speed;
    final float duration;
    final int weight;
    final int applyMethod;
    final int limit;
    final int groupID;

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

    static ItemOverlay create(ItemOverride override, Properties properties) {
        String value = MCPatcherUtils.getStringProperty(properties, "blend", "add");
        BlendMethod blendMethod = BlendMethod.parse(value);
        if (blendMethod == null) {
            override.error("unknown blend type %s", value);
            return null;
        }
        float rotation = MCPatcherUtils.getFloatProperty(properties, "rotation", 0.0f);
        float speed = MCPatcherUtils.getFloatProperty(properties, "speed", 0.0f);
        float duration = MCPatcherUtils.getFloatProperty(properties, "duration", 1.0f);
        int weight = MCPatcherUtils.getIntProperty(properties, "weight", 1);
        int applyMethod;
        String[] tokens = MCPatcherUtils.getStringProperty(properties, "apply", "average").toLowerCase().split("\\s+");
        if (tokens[0].equals("average")) {
            applyMethod = AVERAGE;
        } else if (tokens[0].equals("top")) {
            applyMethod = TOP;
        } else if (tokens[0].equals("cycle")) {
            applyMethod = CYCLE;
        } else {
            override.error("unknown apply type %s", tokens[0]);
            return null;
        }
        int limit = 0;
        if (tokens.length > 1) {
            try {
                limit = Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) {
            }
        }
        limit = Math.max(Math.min(limit, CITUtils.MAX_ENCHANTMENTS - 1), 0);
        return new ItemOverlay(override.textureName, blendMethod, rotation, speed, duration, weight, applyMethod, limit);
    }

    ItemOverlay(String texture, BlendMethod blendMethod, float rotation, float speed, float duration, int weight, int applyMethod, int limit) {
        this.texture = texture;
        this.blendMethod = blendMethod;
        this.rotation = rotation;
        this.speed = speed;
        this.duration = duration;
        this.weight = weight;
        this.applyMethod = applyMethod;
        this.limit = limit;
        groupID = (limit << 2) | applyMethod;
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
        TexturePackAPI.bindTexture(texture);
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

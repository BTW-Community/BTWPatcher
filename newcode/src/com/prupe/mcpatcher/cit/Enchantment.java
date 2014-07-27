package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.BlendMethod;
import com.prupe.mcpatcher.mal.resource.GLAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.tessellator.TessellatorAPI;
import com.prupe.mcpatcher.mal.tile.IconAPI;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.util.Properties;

final class Enchantment extends OverrideBase {
    private static final float ITEM_2D_THICKNESS = 0.0625f;

    static float baseArmorWidth;
    static float baseArmorHeight;

    final int layer;
    final BlendMethod blendMethod;
    private final float rotation;
    private final double speed;
    final float duration;

    private boolean armorScaleSet;
    private float armorScaleX;
    private float armorScaleY;

    static void beginOuter2D() {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GLAPI.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GL11.glEnable(GL11.GL_BLEND);
        GLAPI.glDepthFunc(GL11.GL_EQUAL);
        GLAPI.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
    }

    static void endOuter2D() {
        GLAPI.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GLAPI.glDepthFunc(GL11.GL_LEQUAL);
        GLAPI.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    static void beginOuter3D() {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GLAPI.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GL11.glEnable(GL11.GL_BLEND);
        GLAPI.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
    }

    static void endOuter3D() {
        GLAPI.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GLAPI.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    Enchantment(ResourceLocation propertiesName, Properties properties) {
        super(propertiesName, properties);

        if (!error && textureName == null && alternateTextures == null) {
            error("no source texture specified");
        }

        layer = MCPatcherUtils.getIntProperty(properties, "layer", 0);
        String value = MCPatcherUtils.getStringProperty(properties, "blend", "add");
        blendMethod = BlendMethod.parse(value);
        if (blendMethod == null) {
            error("unknown blend type %s", value);
        }
        rotation = MCPatcherUtils.getFloatProperty(properties, "rotation", 0.0f);
        speed = MCPatcherUtils.getDoubleProperty(properties, "speed", 0.0);
        duration = MCPatcherUtils.getFloatProperty(properties, "duration", 1.0f);

        String valueX = MCPatcherUtils.getStringProperty(properties, "armorScaleX", "");
        String valueY = MCPatcherUtils.getStringProperty(properties, "armorScaleY", "");
        if (!valueX.isEmpty() && !valueY.isEmpty()) {
            try {
                armorScaleX = Float.parseFloat(valueX);
                armorScaleY = Float.parseFloat(valueY);
                armorScaleSet = true;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
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
        if (!bindTexture(CITUtils.lastOrigIcon)) {
            return;
        }
        begin(intensity);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x0, y0, z, 0.0f, 0.0f);
        tessellator.addVertexWithUV(x0, y1, z, 0.0f, 1.0f);
        tessellator.addVertexWithUV(x1, y1, z, 1.0f, 1.0f);
        tessellator.addVertexWithUV(x1, y0, z, 1.0f, 0.0f);
        TessellatorAPI.draw(tessellator);
        end();
    }

    void render3D(Tessellator tessellator, float intensity, int width, int height) {
        if (intensity <= 0.0f) {
            return;
        }
        if (intensity > 1.0f) {
            intensity = 1.0f;
        }
        if (!bindTexture(CITUtils.lastOrigIcon)) {
            return;
        }
        begin(intensity);
        ItemRenderer.renderItemIn2D(tessellator, 1.0f, 0.0f, 0.0f, 1.0f, width, height, ITEM_2D_THICKNESS);
        end();
    }

    boolean bindTexture(Icon icon) {
        ResourceLocation texture;
        if (alternateTextures != null && icon != null) {
            texture = alternateTextures.get(IconAPI.getIconName(icon));
            if (texture == null) {
                texture = textureName;
            }
        } else {
            texture = textureName;
        }
        if (texture == null) {
            return false;
        } else {
            TexturePackAPI.bindTexture(texture);
            return true;
        }
    }

    void beginArmor(float intensity) {
        GL11.glEnable(GL11.GL_BLEND);
        GLAPI.glDepthFunc(GL11.GL_EQUAL);
        GLAPI.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        begin(intensity);
        if (!armorScaleSet) {
            setArmorScale();
        }
        GL11.glScalef(armorScaleX, armorScaleY, 1.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    void endArmor() {
        GLAPI.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GLAPI.glDepthFunc(GL11.GL_LEQUAL);
        GLAPI.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        end();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void begin(float intensity) {
        blendMethod.applyBlending();
        blendMethod.applyDepthFunc();
        blendMethod.applyFade(intensity);
        GL11.glPushMatrix();
        if (speed != 0.0) {
            double offset = ((double) System.currentTimeMillis() * speed) / 3000.0;
            offset -= Math.floor(offset);
            GL11.glTranslatef((float) offset * 8.0f, 0.0f, 0.0f);
        }
        GL11.glRotatef(rotation, 0.0f, 0.0f, 1.0f);
    }

    private void end() {
        GL11.glPopMatrix();
    }

    private void setArmorScale() {
        armorScaleSet = true;
        armorScaleX = 1.0f;
        armorScaleY = 0.5f;
        BufferedImage overlayImage = TexturePackAPI.getImage(textureName);
        if (overlayImage != null) {
            if (overlayImage.getWidth() < baseArmorWidth) {
                armorScaleX *= baseArmorWidth / (float) overlayImage.getWidth();
            }
            if (overlayImage.getHeight() < baseArmorHeight) {
                armorScaleY *= baseArmorHeight / (float) overlayImage.getHeight();
            }
        }
        logger.finer("%s: scaling by %.3fx%.3f for armor model", this, armorScaleX, armorScaleY);
    }
}

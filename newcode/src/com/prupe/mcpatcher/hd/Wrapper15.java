package com.prupe.mcpatcher.hd;

import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import net.minecraft.src.ResourceLocation;
import net.minecraft.src.Texture;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

// 1.5 only
public class Wrapper15 {
    public static void setupTexture(Texture texture, ResourceLocation textureName) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        int[] rgb = new int[width * height];
        IntBuffer buffer = texture.getTextureData().asIntBuffer();
        buffer.position(0);
        buffer.get(rgb);
        MipmapHelper.setupTexture(rgb, width, height, 0, 0, false, false, textureName.getPath());
    }

    public static void setupTexture(Texture texture, BufferedImage image, int glTextureId, boolean blur, boolean clamp, ResourceLocation textureName) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = new int[width * height];
        image.getRGB(0, 0, width, height, rgb, 0, width);
        TexturePackAPI.bindTexture(glTextureId);
        MipmapHelper.setupTexture(rgb, width, height, 0, 0, blur, clamp, textureName.getPath());
    }
}

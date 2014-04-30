package com.prupe.mcpatcher.hd;

import net.minecraft.src.ResourceLocation;
import net.minecraft.src.Texture;

import java.awt.image.BufferedImage;

// 1.5 only
public class Wrapper15 {
    public static void setupTexture(Texture texture, BufferedImage image, int glTexture, boolean blur, boolean clamp, ResourceLocation textureName) {
        MipmapHelper.setupTexture(glTexture, image, blur, clamp, textureName);
    }
}

package net.minecraft.src;

import java.util.List;

public class TextureAtlas extends AbstractTexture implements TickableTextureObject, IconRegister {
    public static ResourceLocation blocksAtlas;
    public static ResourceLocation itemsAtlas;

    public List<TextureAtlasSprite> animations; // made public by Extended HD
    public String basePath; // made public by Extended HD

    public TextureAtlas(int type, String basePath) {
    }

    public Icon registerIcon(String name) {
        return null;
    }

    public void refreshTextures1(ResourceManager resources) {
    }

    public void updateAnimations() {
    }
}

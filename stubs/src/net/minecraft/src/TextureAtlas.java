package net.minecraft.src;

public class TextureAtlas extends AbstractTexture implements TickableTextureObject, IconRegister {
    public static ResourceLocation blocksAtlas;
    public static ResourceLocation itemsAtlas;

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

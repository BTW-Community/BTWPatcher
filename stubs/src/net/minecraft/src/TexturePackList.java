package net.minecraft.src;

import java.util.List;

public class TexturePackList {
    public void updateAvailableTexturePacks() {
    }

    public boolean setTexturePack(ITexturePack texturePackBase) {
        return false;
    }

    public List<ITexturePack> availableTexturePacks() {
        return null;
    }

    public ITexturePack getDefaultTexturePack() { // added by BaseMod.TexturePackBaseMod
        return null;
    }

    public ITexturePack getSelectedTexturePack() {
        return null;
    }
}

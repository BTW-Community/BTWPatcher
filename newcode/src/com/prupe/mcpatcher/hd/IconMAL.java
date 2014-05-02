package com.prupe.mcpatcher.hd;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.TextureAtlasSprite;

abstract class IconMAL {
    static final IconMAL instance = MAL.newInstance(IconMAL.class, "texturepack");

    abstract void registerTileAnimations();

    abstract int getIconX0(TextureAtlasSprite icon);

    abstract int getIconY0(TextureAtlasSprite icon);

    abstract int getIconWidth(TextureAtlasSprite icon);

    abstract int getIconHeight(TextureAtlasSprite icon);

    final private static class V1 extends IconMAL {
        @Override
        void registerTileAnimations() {
        }

        @Override
        int getIconX0(TextureAtlasSprite icon) {
            return icon.getX0();
        }

        @Override
        int getIconY0(TextureAtlasSprite icon) {
            return icon.getY0();
        }

        @Override
        int getIconWidth(TextureAtlasSprite icon) {
            return Math.round(icon.getSheetWidth() * (icon.getMaxU() - icon.getMinU()));
        }

        @Override
        int getIconHeight(TextureAtlasSprite icon) {
            return Math.round(icon.getSheetHeight() * (icon.getMaxV() - icon.getMinV()));
        }
    }

    final private static class V2 extends IconMAL {
        @Override
        void registerTileAnimations() {
            FancyDial.registerAnimations();
        }

        @Override
        int getIconX0(TextureAtlasSprite icon) {
            return icon.getX0();
        }

        @Override
        int getIconY0(TextureAtlasSprite icon) {
            return icon.getY0();
        }

        @Override
        int getIconWidth(TextureAtlasSprite icon) {
            return icon.getWidth();
        }

        @Override
        int getIconHeight(TextureAtlasSprite icon) {
            return icon.getHeight();
        }
    }
}

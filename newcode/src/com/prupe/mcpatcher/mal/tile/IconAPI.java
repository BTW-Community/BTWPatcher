package com.prupe.mcpatcher.mal.tile;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.TextureAtlasSprite;

abstract public class IconAPI {
    private static final IconAPI instance = MAL.newInstance(IconAPI.class, "texturepack");

    public static boolean needRegisterTileAnimations() {
        return instance.needRegisterTileAnimations_Impl();
    }

    public static int getIconX0(TextureAtlasSprite icon) {
        return instance.getIconX0_Impl(icon);
    }

    public static int getIconY0(TextureAtlasSprite icon) {
        return instance.getIconY0_Impl(icon);
    }

    public static int getIconWidth(TextureAtlasSprite icon) {
        return instance.getIconWidth_Impl(icon);
    }

    public static int getIconHeight(TextureAtlasSprite icon) {
        return instance.getIconHeight_Impl(icon);
    }

    abstract boolean needRegisterTileAnimations_Impl();

    abstract int getIconX0_Impl(TextureAtlasSprite icon);

    abstract int getIconY0_Impl(TextureAtlasSprite icon);

    abstract int getIconWidth_Impl(TextureAtlasSprite icon);

    abstract int getIconHeight_Impl(TextureAtlasSprite icon);

    final private static class V1 extends IconAPI {
        @Override
        boolean needRegisterTileAnimations_Impl() {
            return false;
        }

        @Override
        int getIconX0_Impl(TextureAtlasSprite icon) {
            return icon.getX0();
        }

        @Override
        int getIconY0_Impl(TextureAtlasSprite icon) {
            return icon.getY0();
        }

        @Override
        int getIconWidth_Impl(TextureAtlasSprite icon) {
            try {
                return Math.round(icon.getSheetWidth() * (icon.getMaxU() - icon.getMinU()));
            } catch (NullPointerException e) {
                return 0;
            }
        }

        @Override
        int getIconHeight_Impl(TextureAtlasSprite icon) {
            try {
                return Math.round(icon.getSheetHeight() * (icon.getMaxV() - icon.getMinV()));
            } catch (NullPointerException e) {
                return 0;
            }
        }
    }

    final private static class V2 extends IconAPI {
        @Override
        boolean needRegisterTileAnimations_Impl() {
            return true;
        }

        @Override
        int getIconX0_Impl(TextureAtlasSprite icon) {
            return icon.getX0();
        }

        @Override
        int getIconY0_Impl(TextureAtlasSprite icon) {
            return icon.getY0();
        }

        @Override
        int getIconWidth_Impl(TextureAtlasSprite icon) {
            return icon.getWidth();
        }

        @Override
        int getIconHeight_Impl(TextureAtlasSprite icon) {
            return icon.getHeight();
        }
    }
}

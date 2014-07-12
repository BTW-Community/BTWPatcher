package com.prupe.mcpatcher.mal.tile;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Icon;
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

    public static int getIconWidth(Icon icon) {
        return instance.getIconWidth_Impl((TextureAtlasSprite) icon);
    }

    public static int getIconHeight(Icon icon) {
        return instance.getIconHeight_Impl((TextureAtlasSprite) icon);
    }

    public static String getIconName(Icon icon) {
        return instance.getIconName_Impl((TextureAtlasSprite) icon);
    }

    abstract protected boolean needRegisterTileAnimations_Impl();

    abstract protected int getIconX0_Impl(TextureAtlasSprite icon);

    abstract protected int getIconY0_Impl(TextureAtlasSprite icon);

    abstract protected int getIconWidth_Impl(TextureAtlasSprite icon);

    abstract protected int getIconHeight_Impl(TextureAtlasSprite icon);

    abstract protected String getIconName_Impl(TextureAtlasSprite icon);

    final private static class V1 extends IconAPI {
        @Override
        protected boolean needRegisterTileAnimations_Impl() {
            return false;
        }

        @Override
        protected int getIconX0_Impl(TextureAtlasSprite icon) {
            return icon.getX0();
        }

        @Override
        protected int getIconY0_Impl(TextureAtlasSprite icon) {
            return icon.getY0();
        }

        @Override
        protected int getIconWidth_Impl(TextureAtlasSprite icon) {
            try {
                return Math.round(icon.getSheetWidth() * (icon.getMaxU() - icon.getMinU()));
            } catch (NullPointerException e) {
                return 0;
            }
        }

        @Override
        protected int getIconHeight_Impl(TextureAtlasSprite icon) {
            try {
                return Math.round(icon.getSheetHeight() * (icon.getMaxV() - icon.getMinV()));
            } catch (NullPointerException e) {
                return 0;
            }
        }

        @Override
        protected String getIconName_Impl(TextureAtlasSprite icon) {
            return icon.getIconName();
        }
    }

    final private static class V2 extends IconAPI {
        @Override
        protected boolean needRegisterTileAnimations_Impl() {
            return true;
        }

        @Override
        protected int getIconX0_Impl(TextureAtlasSprite icon) {
            return icon.getX0();
        }

        @Override
        protected int getIconY0_Impl(TextureAtlasSprite icon) {
            return icon.getY0();
        }

        @Override
        protected int getIconWidth_Impl(TextureAtlasSprite icon) {
            return icon.getWidth();
        }

        @Override
        protected int getIconHeight_Impl(TextureAtlasSprite icon) {
            return icon.getHeight();
        }

        @Override
        protected String getIconName_Impl(TextureAtlasSprite icon) {
            return icon.getIconName();
        }
    }
}

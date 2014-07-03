package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps Icon interface.
 */
public class IconMod extends com.prupe.mcpatcher.ClassMod {
    public static InterfaceMethodRef getWidth;
    public static InterfaceMethodRef getHeight;
    public static InterfaceMethodRef getX0;
    public static InterfaceMethodRef getY0;
    public static final InterfaceMethodRef getMinU = new InterfaceMethodRef("Icon", "getMinU", "()F");
    public static final InterfaceMethodRef getMaxU = new InterfaceMethodRef("Icon", "getMaxU", "()F");
    public static final InterfaceMethodRef getInterpolatedU = new InterfaceMethodRef("Icon", "getInterpolatedU", "(D)F");
    public static final InterfaceMethodRef getMinV = new InterfaceMethodRef("Icon", "getMinV", "()F");
    public static final InterfaceMethodRef getMaxV = new InterfaceMethodRef("Icon", "getMaxV", "()F");
    public static final InterfaceMethodRef getInterpolatedV = new InterfaceMethodRef("Icon", "getInterpolatedV", "(D)F");
    public static final InterfaceMethodRef getIconName = new InterfaceMethodRef("Icon", "getIconName", "()Ljava/lang/String;");
    public static InterfaceMethodRef getSheetWidth;
    public static InterfaceMethodRef getSheetHeight;

    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("14w25a") < 0;
    }

    public static void setupMod(Mod mod) {
        if (haveClass()) {
            mod.addClassMod(new IconMod(mod));
        } else {
            mod.getClassMap().addAlias("Icon", "TextureAtlasSprite");
            mod.getClassMap().addAlias("net.minecraft.src.Icon", "Icon");
        }
    }

    private IconMod(Mod mod) {
        super(mod);

        List<InterfaceMethodRef> methods = new ArrayList<InterfaceMethodRef>();

        if (ResourceLocationMod.haveClass()) {
            getWidth = new InterfaceMethodRef("Icon", "getWidth", "()I");
            getHeight = new InterfaceMethodRef("Icon", "getHeight", "()I");
            getX0 = null;
            getY0 = null;
            getSheetWidth = null;
            getSheetHeight = null;
            methods.add(getWidth);
            methods.add(getHeight);
        } else {
            getWidth = null;
            getHeight = null;
            getX0 = new InterfaceMethodRef("Icon", "getX0", "()I");
            getY0 = new InterfaceMethodRef("Icon", "getY0", "()I");
            getSheetWidth = new InterfaceMethodRef("Icon", "getSheetWidth", "()I");
            getSheetHeight = new InterfaceMethodRef("Icon", "getSheetHeight", "()I");
            methods.add(getX0);
            methods.add(getY0);
        }

        methods.add(getMinU);
        methods.add(getMaxU);
        methods.add(getInterpolatedU);
        methods.add(getMinV);
        methods.add(getMaxV);
        methods.add(getInterpolatedV);
        methods.add(getIconName);

        if (!ResourceLocationMod.haveClass()) {
            methods.add(getSheetWidth);
            methods.add(getSheetHeight);
        }

        addClassSignature(new InterfaceSignature(methods.toArray(new InterfaceMethodRef[methods.size()]))
                .setInterfaceOnly(true)
        );
    }
}

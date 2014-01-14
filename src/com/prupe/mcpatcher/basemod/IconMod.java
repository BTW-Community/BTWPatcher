package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Maps Icon interface.
 */
public class IconMod extends com.prupe.mcpatcher.ClassMod {
    public IconMod(Mod mod) {
        super(mod);

        final InterfaceMethodRef getWidth = new InterfaceMethodRef(getDeobfClass(), "getWidth", "()I");
        final InterfaceMethodRef getHeight = new InterfaceMethodRef(getDeobfClass(), "getHeight", "()I");
        final InterfaceMethodRef getMinU = new InterfaceMethodRef(getDeobfClass(), "getMinU", "()F");
        final InterfaceMethodRef getMaxU = new InterfaceMethodRef(getDeobfClass(), "getMaxU", "()F");
        final InterfaceMethodRef getInterpolatedU = new InterfaceMethodRef(getDeobfClass(), "getInterpolatedU", "(D)F");
        final InterfaceMethodRef getMinV = new InterfaceMethodRef(getDeobfClass(), "getMinV", "()F");
        final InterfaceMethodRef getMaxV = new InterfaceMethodRef(getDeobfClass(), "getMaxV", "()F");
        final InterfaceMethodRef getInterpolatedV = new InterfaceMethodRef(getDeobfClass(), "getInterpolatedV", "(D)F");
        final InterfaceMethodRef getIconName = new InterfaceMethodRef(getDeobfClass(), "getIconName", "()Ljava/lang/String;");

        addClassSignature(new InterfaceSignature(
            getWidth,
            getHeight,
            getMinU,
            getMaxU,
            getInterpolatedU,
            getMinV,
            getMaxV,
            getInterpolatedV,
            getIconName
        ).setInterfaceOnly(true));
    }
}

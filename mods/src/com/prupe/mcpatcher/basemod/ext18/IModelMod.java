package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

public class IModelMod extends ClassMod {
    public static final InterfaceMethodRef getFaces = new InterfaceMethodRef("IModel", "getFaces", "(LDirection;)Ljava/util/List;");
    public static final InterfaceMethodRef getDefaultFaces = new InterfaceMethodRef("IModel", "getDefaultFaces", "()Ljava/util/List;");
    public static final InterfaceMethodRef useAO = new InterfaceMethodRef("IModel", "useAO", "()Z");
    public static final InterfaceMethodRef randomizePosition = new InterfaceMethodRef("IModel", "randomizePosition", "()Z");
    public static final InterfaceMethodRef rotate180 = new InterfaceMethodRef("IModel", "rotate180", "()Z");
    public static final InterfaceMethodRef getSprite = new InterfaceMethodRef("IModel", "getSprite", "()LTextureAtlasSprite;");
    public static final InterfaceMethodRef getBounds = new InterfaceMethodRef("IModel", "getBounds", "()LBoundingBox;"); // TODO

    public IModelMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            getFaces,
            getDefaultFaces,
            useAO,
            randomizePosition,
            rotate180,
            getSprite,
            getBounds
        ).setInterfaceOnly(true));
    }
}

package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

public class ModelFaceMod extends ClassMod {
    public static final MethodRef constructor = new MethodRef("ModelFace", "<init>", "([IILDirection;)V");
    public static final MethodRef getIntBuffer = new MethodRef("ModelFace", "getIntBuffer", "()[I");
    public static final MethodRef useColormap = new MethodRef("ModelFace", "useColormap", "()Z");
    public static final MethodRef getColor = new MethodRef("ModelFace", "getColor", "()I");
    public static final MethodRef getDirection = new MethodRef("ModelFace", "getDirection", "()LDirection;");

    public ModelFaceMod(Mod mod) {
        super(mod);
        addPrerequisiteClass("ModelFaceSprite");

        addClassSignature(new InterfaceSignature(
            constructor,
            getIntBuffer,
            useColormap,
            getColor,
            getDirection
        ).setInterfaceOnly(false));
    }
}

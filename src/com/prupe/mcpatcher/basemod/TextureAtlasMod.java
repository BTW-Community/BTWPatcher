package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.anyASTORE;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps TextureAtlas class.
 */
public class TextureAtlasMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef basePath = new FieldRef(getDeobfClass(), "basePath", "Ljava/lang/String;");
    protected final FieldRef texturesByName = new FieldRef(getDeobfClass(), "texturesByName", "Ljava/util/Map;");
    protected final MethodRef refreshTextures1 = new MethodRef(getDeobfClass(), "refreshTextures1", "(LResourceManager;)V");
    protected final MethodRef refreshTextures2 = new MethodRef(getDeobfClass(), "refreshTextures2", "(LResourceManager;)V");
    protected final MethodRef registerIcon = new MethodRef(getDeobfClass(), "registerIcon", "(Ljava/lang/String;)LIcon;");

    public TextureAtlasMod(Mod mod) {
        super(mod);
        setParentClass("AbstractTexture");
        setInterfaces("TickableTextureObject", "IconRegister");

        final InterfaceMethodRef mapEntrySet = new InterfaceMethodRef("java/util/Map", "entrySet", "()Ljava/util/Set;");
        final InterfaceMethodRef setIterator = new InterfaceMethodRef("java/util/Set", "iterator", "()Ljava/util/Iterator;");

        addClassSignature(new ConstSignature("missingno"));
        addClassSignature(new ConstSignature(".png"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    captureReference(GETFIELD),
                    reference(INVOKEINTERFACE, mapEntrySet),
                    reference(INVOKEINTERFACE, setIterator),
                    anyASTORE
                );
            }
        }
            .setMethod(refreshTextures2)
            .addXref(1, texturesByName)
        );

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    ALOAD_2,
                    captureReference(PUTFIELD)
                );
            }
        }
            .matchConstructorOnly(true)
            .addXref(1, basePath)
        );

        addMemberMapper(new MethodMapper(registerIcon));
        addMemberMapper(new MethodMapper(refreshTextures1, refreshTextures2));
    }
}

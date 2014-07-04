package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BytecodeMatcher.anyASTORE;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps TextureAtlas class.
 */
public class TextureAtlasMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef basePath = new FieldRef("TextureAtlas", "basePath", "Ljava/lang/String;");
    public static final FieldRef texturesByName = new FieldRef("TextureAtlas", "texturesByName", "Ljava/util/Map;");
    public static MethodRef refreshTextures1;
    public static MethodRef refreshTextures2;
    public static final MethodRef registerIcon = new MethodRef("TextureAtlas", "registerIcon", "(Ljava/lang/String;)LIcon;");

    public TextureAtlasMod(Mod mod) {
        super(mod);

        final int basePathOpcode;
        if (ResourceLocationMod.haveClass()) {
            setParentClass("AbstractTexture");
            if (IconMod.haveClass()) {
                setInterfaces("TickableTextureObject", "IconRegister");
                basePathOpcode = ALOAD_2;
            } else {
                setInterfaces("TickableIconRegister");
                basePathOpcode = ALOAD_1;
            }
            refreshTextures1 = new MethodRef("TextureAtlas", "refreshTextures1", "(LResourceManager;)V");
            refreshTextures2 = new MethodRef("TextureAtlas", "refreshTextures2", "(LResourceManager;)V");
            addMemberMapper(new MethodMapper(refreshTextures1, refreshTextures2));
        } else {
            setInterfaces("IconRegister");
            refreshTextures1 = new MethodRef("TextureAtlas", "refreshTextures", "()V");
            refreshTextures2 = null;
            addMemberMapper(new MethodMapper(refreshTextures1));
            basePathOpcode = ALOAD_3;
        }

        final InterfaceMethodRef mapEntrySet = new InterfaceMethodRef("java/util/Map", "entrySet", "()Ljava/util/Set;");
        final InterfaceMethodRef setIterator = new InterfaceMethodRef("java/util/Set", "iterator", "()Ljava/util/Iterator;");
        final InterfaceMethodRef mapClear = new InterfaceMethodRef("java/util/Map", "clear", "()V");

        addClassSignature(new ConstSignature("missingno"));
        addClassSignature(new ConstSignature(".png"));

        if (ResourceLocationMod.haveClass()) {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(refreshTextures2);
                    addXref(1, texturesByName);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // iterator = this.texturesByName.entrySet().iterator();
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, mapEntrySet),
                        reference(INVOKEINTERFACE, setIterator),
                        anyASTORE
                    );
                }
            });
        } else {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(refreshTextures1);
                    addXref(1, texturesByName);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.texturesByName.clear();
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEINTERFACE, mapClear)
                    );
                }
            });
        }

        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                addXref(1, basePath);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.basePath = basePath;
                    ALOAD_0,
                    basePathOpcode,
                    captureReference(PUTFIELD)
                );
            }
        });
    }
}

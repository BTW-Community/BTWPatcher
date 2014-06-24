package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Matches NBTTagCompound class.
 */
public class NBTTagCompoundMod extends com.prupe.mcpatcher.ClassMod {
    private final InterfaceMethodRef containsKey = new InterfaceMethodRef("java/util/Map", "containsKey", "(Ljava/lang/Object;)Z");
    private final InterfaceMethodRef mapRemove = new InterfaceMethodRef("java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;");
    protected final FieldRef tagMap = new FieldRef(getDeobfClass(), "tagMap", "Ljava/util/Map;");

    public NBTTagCompoundMod(Mod mod) {
        super(mod);
        setParentClass("NBTBase");

        addClassSignature(new OrSignature(
            new ConstSignature(new ClassRef("java.util.HashMap")),
            new ConstSignature(new ClassRef("java.util.Map"))
        ));
        if (Mod.getMinecraftVersion().compareTo("13w36a") >= 0) {
            addClassSignature(new ConstSignature(new InterfaceMethodRef("java/io/DataInput", "readByte", "()B")));
            addClassSignature(new ConstSignature(new InterfaceMethodRef("java/io/DataInput", "readUTF", "()Ljava/lang/String;")));
        } else {
            addClassSignature(new ConstSignature(":["));
            addClassSignature(new ConstSignature(":"));
            addClassSignature(new ConstSignature(","));
            addClassSignature(new ConstSignature("]"));
        }

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(new MethodRef(getDeobfClass(), "hasKey", "(Ljava/lang/String;)Z"));
                addXref(1, tagMap);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    captureReference(GETFIELD),
                    ALOAD_1,
                    reference(INVOKEINTERFACE, containsKey),
                    IRETURN
                );
            }
        });

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(new MethodRef(getDeobfClass(), "removeTag", "(Ljava/lang/String;)V"));
                addXref(1, tagMap);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    captureReference(GETFIELD),
                    ALOAD_1,
                    reference(INVOKEINTERFACE, mapRemove)
                );
            }
        });

        mapNBTMethod("Byte", "B");
        mapNBTMethod("ByteArray", "[B");
        mapNBTMethod("Double", "D");
        mapNBTMethod("Float", "F");
        mapNBTMethod("IntArray", "[I");
        mapNBTMethod("Integer", "I");
        mapNBTMethod("Long", "J");
        mapNBTMethod("Short", "S");
        mapNBTMethod("String", "Ljava/lang/String;");

        addMemberMapper(new MethodMapper(null, new MethodRef(getDeobfClass(), "getBoolean", "(Ljava/lang/String;)Z")));
        addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "setBoolean", "(Ljava/lang/String;Z)V")));
        addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getCompoundTag", "(Ljava/lang/String;)L" + getDeobfClass() + ";")));
        addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getTag", "(Ljava/lang/String;)LNBTBase;")));
    }

    public NBTTagCompoundMod mapGetTagList() {
        addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getTagList", "(Ljava/lang/String;)LNBTTagList;")));
        return this;
    }

    protected void mapNBTMethod(String type, String desc) {
        final MethodRef get = new MethodRef(getDeobfClass(), "get" + type, "(Ljava/lang/String;)" + desc);
        final MethodRef set = new MethodRef(getDeobfClass(), "set" + type, "(Ljava/lang/String;" + desc + ")V");

        addMemberMapper(new MethodMapper(get));
        addMemberMapper(new MethodMapper(set));
    }
}

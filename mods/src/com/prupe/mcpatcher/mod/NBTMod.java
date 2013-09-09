package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class NBTMod extends Mod {
    private final boolean newBaseClass;

    public NBTMod() {
        name = MCPatcherUtils.NBT_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.1";

        newBaseClass = getMinecraftVersion().compareTo("13w36a") >= 0;

        addClassMod(new NBTBaseMod());
        addClassMod(new BaseMod.NBTTagCompoundMod(this));
        addClassMod(new BaseMod.NBTTagListMod(this));
        if (newBaseClass) {
            addClassMod(new NBTTagScalarMod());
        }
        addClassMod(new NBTTagNumberMod(1, "Byte", "B"));
        addClassMod(new NBTTagNumberMod(2, "Short", "S"));
        addClassMod(new NBTTagNumberMod(3, "Int", "I"));
        addClassMod(new NBTTagNumberMod(4, "Long", "J"));
        addClassMod(new NBTTagNumberMod(5, "Float", "F"));
        addClassMod(new NBTTagNumberMod(6, "Double", "D"));

        addClassFile(MCPatcherUtils.NBT_RULE_CLASS);
        addClassFile(MCPatcherUtils.NBT_RULE_CLASS + "$Exact");
        addClassFile(MCPatcherUtils.NBT_RULE_CLASS + "$Regex");
        addClassFile(MCPatcherUtils.NBT_RULE_CLASS + "$Glob");
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class NBTBaseMod extends ClassMod {
        NBTBaseMod() {
            final MethodRef getId = new MethodRef(getDeobfClass(), "getId", "()B");

            addClassSignature(new ConstSignature("END"));
            addClassSignature(new ConstSignature("BYTE"));
            addClassSignature(new ConstSignature("SHORT"));

            addMemberMapper(new MethodMapper(getId)
                .accessFlag(AccessFlag.ABSTRACT, true)
            );
        }
    }

    private class NBTTagScalarMod extends ClassMod {
        NBTTagScalarMod() {
            setParentClass("NBTBase");

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "(Ljava/lang/String;)V"),
                new MethodRef(getDeobfClass(), "getLong", "()J"),
                new MethodRef(getDeobfClass(), "getInt", "()I"),
                new MethodRef(getDeobfClass(), "getShort", "()S"),
                new MethodRef(getDeobfClass(), "getByte", "()B"),
                new MethodRef(getDeobfClass(), "getDouble", "()D"),
                new MethodRef(getDeobfClass(), "getFloat", "()F")
            ).setAbstractOnly(true));
        }
    }

    private class NBTTagNumberMod extends ClassMod {
        private final String name;

        NBTTagNumberMod(final int id, String name, String desc) {
            this.name = name;
            setParentClass(newBaseClass ? "NBTTagScalar" : "NBTBase");

            final FieldRef data = new FieldRef(getDeobfClass(), "data", desc);
            final MethodRef getId = new MethodRef(getDeobfClass(), "getId", "()B");
            final MethodRef getValue = new MethodRef(getDeobfClass(), "get" + name, "()" + desc);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(id),
                        IRETURN,
                        end()
                    );
                }
            }.setMethod(getId));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD)
                    );
                }
            }
                .setMethod(getValue)
                .addXref(1, data)
            );

            addPatch(new MakeMemberPublicPatch(data));
        }

        @Override
        public String getDeobfClass() {
            return "NBTTag" + name;
        }
    }
}

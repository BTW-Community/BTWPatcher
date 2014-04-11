package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.NBTTagListMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
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
        if (newBaseClass) {
            addClassMod(new NBTTagScalarMod());
        }
        addClassMod(new NBTTagNumberMod(1, "Byte", "B"));
        addClassMod(new NBTTagNumberMod(2, "Short", "S"));
        addClassMod(new NBTTagNumberMod(3, "Int", "I"));
        addClassMod(new NBTTagNumberMod(4, "Long", "J"));
        addClassMod(new NBTTagNumberMod(5, "Float", "F"));
        addClassMod(new NBTTagNumberMod(6, "Double", "D"));
        addClassMod(new NBTSubclassMod(7, "ByteArray", "[B"));
        addClassMod(new NBTSubclassMod(8, "String", "Ljava/lang/String;"));
        addClassMod(new NBTTagListMod(this)); // id=9
        addClassMod(new NBTTagCompoundMod()); // id=10
        addClassMod(new NBTSubclassMod(11, "IntArray", "[I"));

        addClassFiles("com.prupe.mcpatcher.mal.nbt.*");
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
                new MethodRef(getDeobfClass(), "<init>",
                    getMinecraftVersion().compareTo("13w38a") < 0 ? "(Ljava/lang/String;)V" : "()V"),
                new MethodRef(getDeobfClass(), "getLong", "()J"),
                new MethodRef(getDeobfClass(), "getInt", "()I"),
                new MethodRef(getDeobfClass(), "getShort", "()S"),
                new MethodRef(getDeobfClass(), "getByte", "()B"),
                new MethodRef(getDeobfClass(), "getDouble", "()D"),
                new MethodRef(getDeobfClass(), "getFloat", "()F")
            ).setAbstractOnly(true));
        }
    }

    private class NBTSubclassMod extends ClassMod {
        protected final String name;
        protected final FieldRef data;
        protected final MethodRef getId;
        protected final MethodRef getValue;

        NBTSubclassMod(final int id, String name, String desc) {
            this.name = name;
            data = new FieldRef(getDeobfClass(), "data", desc);
            getId = new MethodRef(getDeobfClass(), "getId", "()B");
            getValue = new MethodRef(getDeobfClass(), "get" + name, "()" + desc);

            setParentClass("NBTBase");

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

            addMemberMapper(new FieldMapper(data));

            addPatch(new MakeMemberPublicPatch(data));
        }

        @Override
        public String getDeobfClass() {
            return "NBTTag" + name;
        }
    }

    private class NBTTagNumberMod extends NBTSubclassMod {
        NBTTagNumberMod(final int id, String name, String desc) {
            super(id, name, desc);
            if (newBaseClass) {
                setParentClass("NBTTagScalar");
            }
        }
    }

    private class NBTTagCompoundMod extends com.prupe.mcpatcher.basemod.NBTTagCompoundMod {
        NBTTagCompoundMod() {
            super(NBTMod.this);

            final MethodRef setCompoundTag = new MethodRef(getDeobfClass(), "setCompoundTag", "(Ljava/lang/String;L" + getDeobfClass() + ";)V");
            final MethodRef getTags = new MethodRef(getDeobfClass(), "getTags", "()Ljava/util/Collection;");

            if (getMinecraftVersion().compareTo("13w38a") < 0) {
                addMemberMapper(new MethodMapper(setCompoundTag));
                addMemberMapper(new MethodMapper(getTags));
            } else {
                addPatch(new AddMethodPatch(getTags) {
                    @Override
                    public byte[] generateMethod() {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, tagMap),
                            reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/Map", "values", "()Ljava/util/Collection;")),
                            ARETURN
                        );
                    }
                });
            }
        }
    }
}

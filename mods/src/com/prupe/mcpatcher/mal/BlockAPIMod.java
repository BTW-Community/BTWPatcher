package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;

public class BlockAPIMod extends Mod {
    private final int malVersion;

    public BlockAPIMod() {
        name = MCPatcherUtils.BLOCK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            malVersion = 1;
        } else {
            malVersion = 2;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("block", malVersion);

        addClassMod(new BlockMod());
        addClassMod(new BaseMod.IBlockAccessMod(this));
        if (malVersion >= 2) {
            addClassMod(new RegistryBaseMod());
            addClassMod(new RegistryMod());
        }

        addClassFile(MCPatcherUtils.BLOCK_API_CLASS);
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V" + malVersion);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            super(BlockAPIMod.this);

            if (malVersion == 1) {
                return;
            }

            final FieldRef blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry", "LRegistry;");

            addMemberMapper(new FieldMapper(blockRegistry));
        }
    }

    private class RegistryBaseMod extends ClassMod {
        RegistryBaseMod() {
            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "newHashMap", "()Ljava/util/HashMap;"),
                new MethodRef(getDeobfClass(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getKeys", "()Ljava/util/Set;")
            ).setInterfaceOnly(false));
        }
    }

    private class RegistryMod extends ClassMod {
        RegistryMod() {
            setParentClass("RegistryBase");

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "register", "(ILjava/lang/String;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getId", "(Ljava/lang/Object;)I"),
                new MethodRef(getDeobfClass(), "getById", "(I)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "getAll", "()Ljava/util/List;")
            ).setInterfaceOnly(false));
        }
    }
}

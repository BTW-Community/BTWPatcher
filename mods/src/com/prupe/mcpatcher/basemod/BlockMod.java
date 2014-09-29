package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.MinecraftVersion;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.ext18.DirectionMod;
import com.prupe.mcpatcher.basemod.ext18.PositionMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Matches Block class and maps blockID and blockList fields.
 */
public class BlockMod extends com.prupe.mcpatcher.ClassMod {
    private static final MinecraftVersion MIN_VERSION_REGISTRY = MinecraftVersion.parseVersion("13w36a");

    public static MethodRef getBlockIcon;
    public static MethodRef getBlockIconFromSideAndMetadata;
    public static MethodRef getSecondaryBlockIcon;
    public static MethodRef useColorMultiplierOnFace;
    public static MethodRef shouldSideBeRendered;
    public static FieldRef blockRegistry;

    public static final MethodRef getShortName = new MethodRef("Block", "getShortName", "()Ljava/lang/String;");
    public static final FieldRef lightValue = new FieldRef("Block", "lightValue", "[I");
    public static final MethodRef getLightValue = new MethodRef("Block", "getLightValue", "()I");
    public static final MethodRef getRenderType = new MethodRef("Block", "getRenderType", "()I");
    public static final FieldRef blockMaterial = new FieldRef("Block", "blockMaterial", "LMaterial;");

    public static boolean haveBlockRegistry() {
        return Mod.getMinecraftVersion().compareTo(MIN_VERSION_REGISTRY) >= 0;
    }

    public BlockMod(Mod mod) {
        super(mod);

        getBlockIcon = new MethodRef("Block", "getBlockIcon", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")LIcon;");
        getBlockIconFromSideAndMetadata = new MethodRef("Block", "getBlockIconFromSideAndMetadata", "(" + DirectionMod.getDescriptor() + "I)LIcon;");
        getSecondaryBlockIcon = RenderBlocksMod.haveSubclasses() ? new MethodRef("Block", "getSecondaryBlockIcon", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")LIcon;") : null;
        useColorMultiplierOnFace = DirectionMod.haveDirectionClass() ? new MethodRef("Block", "useColorMultiplierOnFace", "(" + DirectionMod.getDescriptor() + ")Z") : null;
        shouldSideBeRendered = new MethodRef("Block", "shouldSideBeRendered", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")Z");
        if (BlockRegistryMod.haveClass()) {
            blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry1", "LBlockRegistry;");
        } else if (haveBlockRegistry()) {
            blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry", "LRegistry;");
        } else {
            blockRegistry = null;
        }

        if (haveBlockRegistry()) {
            addClassSignature(new ConstSignature("stone"));
            addClassSignature(new ConstSignature("grass"));
            addClassSignature(new ConstSignature("dirt"));
            addClassSignature(new ConstSignature(".name"));
        } else {
            addClassSignature(new ConstSignature(" is already occupied by "));

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blockID", "I"))
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, true)
            );

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blocksList", "[LBlock;"))
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
                    .accessFlag(AccessFlag.FINAL, true)
            );
        }

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push("tile.")
                );
            }
        }.setMethod(getShortName));
    }

    public BlockMod mapBlockIconMethods() {
        if (getSecondaryBlockIcon == null) {
            addMemberMapper(new MethodMapper(getBlockIcon));
        } else {
            addMemberMapper(new MethodMapper(getSecondaryBlockIcon, getBlockIcon));
        }
        addMemberMapper(new MethodMapper(getBlockIconFromSideAndMetadata));
        return this;
    }

    public BlockMod mapBlockMaterial() {
        final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(" + (haveBlockRegistry() ? "" : "I") + "LMaterial;)V");

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(constructor);
                addXref(1, blockMaterial);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    haveBlockRegistry() ? ALOAD_1 : ALOAD_2,
                    captureReference(PUTFIELD)
                );
            }
        });

        return this;
    }
}

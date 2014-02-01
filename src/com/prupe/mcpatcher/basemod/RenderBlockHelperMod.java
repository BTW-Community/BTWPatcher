package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.MinecraftVersion;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.end;
import static javassist.bytecode.Opcode.*;

public class RenderBlockHelperMod extends com.prupe.mcpatcher.ClassMod {
    private static final MinecraftVersion MIN_VERSION = MinecraftVersion.parseVersion("14w05a");

    public static final MethodRef mixAOBrightness = new MethodRef("RenderBlockHelper", "mixAOBrightness", "(IIII)I");
    public static final MethodRef computeVertexColors = new MethodRef("RenderBlockHelper", "computeVertexColors", "(LBlock;LPosition;LDirection;IFFFZ)V");
    public static final FieldRef renderBlocks = new FieldRef("RenderBlockHelper", "renderBlocks", "LRenderBlocks;");

    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo(MIN_VERSION) >= 0;
    }

    public RenderBlockHelperMod(Mod mod) {
        super(mod);

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // return ((var1 + var2 + var3 + var4) >> 2) & 0xff00ff;
                    ILOAD_1,
                    ILOAD_2,
                    IADD,
                    ILOAD_3,
                    IADD,
                    ILOAD, 4,
                    IADD,
                    push(2),
                    ISHR,
                    push(0xff00ff),
                    IAND,
                    IRETURN,
                    end()
                );
            }
        }.setMethod(mixAOBrightness));

        addMemberMapper(new FieldMapper(renderBlocks));
        addMemberMapper(new MethodMapper(computeVertexColors));
    }
}

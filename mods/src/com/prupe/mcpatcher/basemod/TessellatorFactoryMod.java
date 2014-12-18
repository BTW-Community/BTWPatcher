package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

public class TessellatorFactoryMod extends ClassMod {
    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("14w25a") >= 0;
    }

    public TessellatorFactoryMod(Mod mod) {
        super(mod);

        final ClassRef tessellatorClass = new ClassRef("Tessellator");
        final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellatorFactory;");
        final MethodRef getInstance = new MethodRef(getDeobfClass(), "getInstance", "()LTessellatorFactory;");
        final MethodRef getTessellator = new MethodRef(getDeobfClass(), "getTessellator", "()LTessellator;");
        final MethodRef draw;

        if (TessellatorMod.drawReturnsInt()) {
            draw = new MethodRef(getDeobfClass(), "drawInt", "()I");
        } else {
            draw = new MethodRef(getDeobfClass(), "drawVoid", "()V");
        }

        addClassSignature(new BytecodeSignature() {
            {
                matchStaticInitializerOnly(true);
                addXref(1, tessellatorClass);
                addXref(2, instance);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // TessellatorFactory.instance = new TessellatorFactory(0x200000);
                    captureReference(NEW),
                    DUP,
                    push(0x200000),
                    anyReference(INVOKESPECIAL),
                    captureReference(PUTSTATIC)
                );
            }
        });

        addMemberMapper(new MethodMapper(getInstance)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
        );

        addMemberMappers("public !static", getTessellator, draw);
    }
}

package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.*;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class VertexFormatMod extends ClassMod {
    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("1.8.2-pre5") >= 0;
    }

    public VertexFormatMod(Mod mod) {
        super(mod);

        final MethodRef addElement = new MethodRef("VertexFormat", "addElement", "(LVertexFormatElement;)LVertexFormat;");

        addClassSignature(new ConstSignature("format: "));
        addClassSignature(new ConstSignature(" elements: "));

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(addElement);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // switch (...[...])
                    anyReference(GETSTATIC),
                    ALOAD_1,
                    anyReference(INVOKEVIRTUAL),
                    anyReference(INVOKEVIRTUAL),
                    IALOAD,
                    TABLESWITCH
                );
            }
        });
    }
}

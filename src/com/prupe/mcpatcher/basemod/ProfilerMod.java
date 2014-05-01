package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps Profiler class and start/endSection methods.
 */
public class ProfilerMod extends com.prupe.mcpatcher.ClassMod {
    public ProfilerMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("root"));
        addClassSignature(new ConstSignature("[UNKNOWN]"));
        addClassSignature(new ConstSignature(100.0));

        final MethodRef startSection = new MethodRef(getDeobfClass(), "startSection", "(Ljava/lang/String;)V");
        final MethodRef endSection = new MethodRef(getDeobfClass(), "endSection", "()V");
        final MethodRef endStartSection = new MethodRef(getDeobfClass(), "endStartSection", "(Ljava/lang/String;)V");

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(endStartSection);
                addXref(1, endSection);
                addXref(2, startSection);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    captureReference(INVOKEVIRTUAL),
                    ALOAD_0,
                    ALOAD_1,
                    captureReference(INVOKEVIRTUAL)
                );
            }
        });
    }
}

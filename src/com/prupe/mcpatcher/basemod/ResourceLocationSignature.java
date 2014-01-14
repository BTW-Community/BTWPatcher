package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.BytecodeSignature;
import com.prupe.mcpatcher.ClassRef;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Signature for matching a fixed ResourceLocation path field.
 */
public class ResourceLocationSignature extends BytecodeSignature {
    protected static final ClassRef resourceLocationClass = new ClassRef("ResourceLocation");
    protected static final MethodRef resourceLocationInit1 = new MethodRef("ResourceLocation", "<init>", "(Ljava/lang/String;)V");
    protected final FieldRef mappedField;
    protected final String path;

    public ResourceLocationSignature(com.prupe.mcpatcher.ClassMod classMod, FieldRef mappedField, String path) {
        super(classMod);
        this.mappedField = mappedField;
        this.path = path;

        matchStaticInitializerOnly(true);
        addXref(1, resourceLocationClass);
        addXref(2, resourceLocationInit1);
        addXref(3, mappedField);
    }

    @Override
    public String getMatchExpression() {
        return buildExpression(
            captureReference(NEW),
            DUP,
            push(getResourcePath()),
            captureReference(INVOKESPECIAL),
            captureReference(PUTSTATIC)
        );
    }

    protected String getResourcePath() {
        return path;
    }
}

package com.prupe.mcpatcher;

/**
 * Fixed BytecodeSignature that does not require any method information.  Offers better
 * performance when the target bytecode sequence does not contain any references.
 */
public class FixedBytecodeSignature extends BytecodeSignature {
    /**
     * @param objects BinaryRegex expressions representing a fixed signature
     * @see BinaryRegex#build(Object...)
     */
    public FixedBytecodeSignature(ClassMod classMod, Object... objects) {
        super(classMod);
        matcher = new BytecodeMatcher(objects);
    }

    @Override
    final public String getMatchExpression() {
        throw new AssertionError("Unreachable");
    }

    @Override
    void initMatcher() {
    }
}

package com.prupe.mcpatcher;

import javassist.bytecode.*;

import java.util.*;

import static javassist.bytecode.Opcode.*;

/**
 * ClassPatch that matches a replaces and specific bytecode sequence.  The full power of Java
 * regular expressions is available.  Wildcards, capturing, and backreferences can be used via
 * their wrappers in the BinaryRegex class.
 *
 * @see BinaryRegex
 */
abstract public class BytecodePatch extends ClassPatch {
    private final Set<MethodRef> targetMethods = new HashSet<MethodRef>();
    private final Set<MethodRef> skipMethods = new HashSet<MethodRef>();
    boolean constructorOnly;
    boolean staticInitializerOnly;
    private final List<BytecodeSignature> preMatchSignatures = new ArrayList<BytecodeSignature>();
    private boolean insertBefore;
    private boolean insertAfter;
    private int methodMatchCount;
    int labelOffset;

    public BytecodePatch(ClassMod classMod) {
        super(classMod);
    }

    /**
     * Matcher object.
     *
     * @see BytecodeMatcher
     */
    protected BytecodeMatcher matcher;

    /**
     * Restricts the patch to ONLY a specific set of methods.
     *
     * @param targetMethods one or more deobfuscated methods
     * @return this
     * @see #getTargetMethods()
     */
    public BytecodePatch targetMethod(MethodRef... targetMethods) {
        Collections.addAll(this.targetMethods, targetMethods);
        return this;
    }

    /**
     * Adds to a list of methods NOT to be considered for patching.
     *
     * @param skipMethods one or more deobfuscated methods to be compared against
     * @return this
     * @see #getTargetMethods()
     */
    public BytecodePatch skipMethod(MethodRef... skipMethods) {
        Collections.addAll(this.skipMethods, skipMethods);
        return this;
    }

    /**
     * Sets whether only constructors should be considered for matching.
     * Shorthand for a filterMethod that returns getMethodInfo().isConstructor().
     *
     * @param only true to enable constructor filter.
     * @return this
     */
    public BytecodePatch matchConstructorOnly(boolean only) {
        constructorOnly = only;
        return this;
    }

    /**
     * Sets whether only static initializers should be considered for matching.
     * Shorthand for a filterMethod that returns getMethodInfo().isStaticInitializer().
     *
     * @param only true to enable static initializer filter.
     * @return this
     */
    public BytecodePatch matchStaticInitializerOnly(boolean only) {
        staticInitializerOnly = only;
        return this;
    }

    /**
     * Sets whether to insert the replacement bytes before the matching code rather than
     * replacing it.
     *
     * @param insertBefore true to insert before match
     * @return this
     */
    public BytecodePatch setInsertBefore(boolean insertBefore) {
        this.insertBefore = insertBefore;
        if (insertBefore) {
            insertAfter = false;
        }
        return this;
    }

    /**
     * Sets whether to insert the replacement bytes after the matching code rather than
     * replacing it.
     *
     * @param insertAfter true to insert after match
     * @return this
     */
    public BytecodePatch setInsertAfter(boolean insertAfter) {
        this.insertAfter = insertAfter;
        if (insertAfter) {
            insertBefore = false;
        }
        return this;
    }

    /**
     * Add a bytecode signature that must match the current method before the patch is applied.  It is
     * tested against each method after targetMethod but before getMatchExpression.  This can be used
     * to capture parts of the method (e.g., register numbers) in different locations from the main
     * match expression.
     *
     * @return this
     */
    public BytecodePatch addPreMatchSignature(BytecodeSignature signature) {
        preMatchSignatures.add(signature);
        return this;
    }

    /**
     * Can be overridden in lieu of calling targetMethod() to set the target method at patch time.
     *
     * @return target method refs
     */
    public Collection<MethodRef> getTargetMethods() {
        return targetMethods;
    }

    /**
     * Can be overridden to skip certain methods during patching.
     *
     * @param methodInfo current method
     * @return true if method should be considered for patching
     * @see #filterMethod()
     * @deprecated
     */
    public boolean filterMethod(MethodInfo methodInfo) {
        return true;
    }

    /**
     * Can be overridden to skip certain methods during patching.
     *
     * @return true if method should be considered for patching
     */
    @SuppressWarnings("deprecation")
    public boolean filterMethod() {
        return filterMethod(getMethodInfo());
    }

    private boolean filterMethod1(MethodInfo methodInfo) {
        if (constructorOnly && !methodInfo.isConstructor()) {
            return false;
        }
        if (staticInitializerOnly && !methodInfo.isStaticInitializer()) {
            return false;
        }
        Collection<MethodRef> targetMethods = getTargetMethods();
        if (targetMethods != null && !targetMethods.isEmpty()) {
            boolean found = false;
            for (MethodRef method : targetMethods) {
                JavaRef mappedMethod = map(method);
                if (methodInfo.getDescriptor().equals(mappedMethod.getType()) && methodInfo.getName().equals(mappedMethod.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        for (MethodRef method : skipMethods) {
            JavaRef mappedMethod = map(method);
            if (methodInfo.getDescriptor().equals(mappedMethod.getType()) && methodInfo.getName().equals(mappedMethod.getName())) {
                return false;
            }
        }
        for (BytecodeSignature signature : preMatchSignatures) {
            if (signature.match(null, methodInfo, null) == signature.negate) {
                return false;
            }
        }
        return filterMethod();
    }

    /**
     * Get a regular expression to look for within the target method's bytecode.  The expression
     * can contain opcodes (IADD, IF_ICMPGE, etc.).  Use the push method to generate bytecode
     * for loading constants and the reference method for getting/setting fields and invoking methods.
     *
     * @return string using BinaryRegex methods
     * @see #push(Object)
     * @see #reference(int, JavaRef)
     */
    abstract public String getMatchExpression();

    /**
     * Get replacement bytecode after a match.  May be shorter or longer than the original bytecode;
     * MCPatcher will take care of padding.  Use the buildCode method to generate the return byte
     * array.
     *
     * @return replacement bytes
     */
    abstract public byte[] getReplacementBytes();

    private boolean apply(MethodInfo mi) throws BadBytecode {
        boolean patched = false;
        CodeAttribute ca = mi.getCodeAttribute();
        if (ca == null) {
            return patched;
        }
        matcher = new BytecodeMatcher(getMatchExpression());
        CodeIterator ci = ca.iterator();
        int oldStackSize = ca.computeMaxStack();
        int oldMaxLocals = ca.getMaxLocals();
        int offset = 0;
        ArrayList<String> txtBefore = null;
        methodMatchCount = 0;

        while (matcher.match(mi, offset)) {
            byte repl[];
            classMod.addToConstPool = true;
            classMod.resetLabels();
            labelOffset = 0;
            repl = getReplacementBytes();
            methodMatchCount++;
            classMod.addToConstPool = false;
            if (repl == null) {
                while (offset < matcher.getEnd() && ci.hasNext()) {
                    offset = ci.next();
                }
                continue;
            }

            recordPatch(String.format("%s%s@%d", mi.getName(), mi.getDescriptor(), matcher.getStart()));

            if ((insertBefore || insertAfter) && matcher.getMatchLength() > 0) {
                byte[] match = getMatch();
                byte[] newRepl = new byte[repl.length + match.length];
                if (insertBefore) {
                    System.arraycopy(repl, 0, newRepl, 0, repl.length);
                    System.arraycopy(match, 0, newRepl, repl.length, match.length);
                } else if (insertAfter) {
                    labelOffset = matcher.getMatchLength();
                    System.arraycopy(match, 0, newRepl, 0, match.length);
                    System.arraycopy(repl, 0, newRepl, match.length, repl.length);
                }
                repl = newRepl;
            }

            if (Logger.isLogLevel(Logger.LOG_BYTECODE)) {
                txtBefore = bytecodeToString(ca, matcher.getStart(), matcher.getEnd());
            }
            int gap = repl.length - matcher.getMatchLength();
            int skip = 0;
            if (gap > 0) {
                skip = ci.insertGap(matcher.getStart(), gap) - gap;
            } else if (gap < 0) {
                skip = -gap;
                gap = 0;
            }
            for (int i = 0; i < skip; i++) {
                ci.writeByte(Opcode.NOP, matcher.getStart() + i);
            }
            classMod.resolveLabels(repl, matcher.getStart() + skip, labelOffset);
            ci.write(repl, matcher.getStart() + skip);
            offset = matcher.getStart() + repl.length + skip;
            if (Logger.isLogLevel(Logger.LOG_BYTECODE)) {
                ArrayList<String> txtAfter = bytecodeToString(ca, matcher.getStart(), offset);
                logBytecodePatch(txtBefore, txtAfter);
            }
            patched = true;
            ci.move(offset);
        }

        if (patched) {
            int newStackSize = ca.computeMaxStack();
            if (oldStackSize < newStackSize) {
                Logger.log(Logger.LOG_METHOD, "increasing stack size from %d to %d", oldStackSize, newStackSize);
                ca.setMaxStack(newStackSize);
            }
            int newMaxLocals = computeMaxLocals(ca);
            if (oldMaxLocals < newMaxLocals) {
                Logger.log(Logger.LOG_METHOD, "increasing max locals from %d to %d", oldMaxLocals, newMaxLocals);
                ca.setMaxLocals(newMaxLocals);
            }
        }

        return patched;
    }

    static int computeMaxLocals(CodeAttribute ca) throws BadBytecode {
        CodeIterator ci = ca.iterator();
        int maxLocals = 0;
        while (ci.hasNext()) {
            int offset = ci.next();
            int local;
            int wideReg = 0;
            switch (ci.byteAt(offset)) {
                case LLOAD_0:
                case DLOAD_0:
                case LSTORE_0:
                case DSTORE_0:
                    wideReg = 1;
                    /* fall through */

                case ALOAD_0:
                case ILOAD_0:
                case FLOAD_0:
                case ASTORE_0:
                case ISTORE_0:
                case FSTORE_0:
                    local = 0;
                    break;

                case LLOAD_1:
                case DLOAD_1:
                case LSTORE_1:
                case DSTORE_1:
                    wideReg = 1;
                    /* fall through */

                case ALOAD_1:
                case ILOAD_1:
                case FLOAD_1:
                case ASTORE_1:
                case ISTORE_1:
                case FSTORE_1:
                    local = 1;
                    break;

                case LLOAD_2:
                case DLOAD_2:
                case LSTORE_2:
                case DSTORE_2:
                    wideReg = 1;
                    /* fall through */

                case ALOAD_2:
                case ILOAD_2:
                case FLOAD_2:
                case ASTORE_2:
                case ISTORE_2:
                case FSTORE_2:
                    local = 2;
                    break;

                case LLOAD_3:
                case DLOAD_3:
                case LSTORE_3:
                case DSTORE_3:
                    wideReg = 1;
                    /* fall through */

                case ALOAD_3:
                case ILOAD_3:
                case FLOAD_3:
                case ASTORE_3:
                case ISTORE_3:
                case FSTORE_3:
                    local = 3;
                    break;

                case LLOAD:
                case DLOAD:
                case LSTORE:
                case DSTORE:
                    wideReg = 1;
                    /* fall through */

                case ALOAD:
                case ILOAD:
                case FLOAD:
                case ASTORE:
                case ISTORE:
                case FSTORE:
                    local = ci.byteAt(offset + 1) & 0xff;
                    break;

                case WIDE:
                    switch (ci.byteAt(++offset)) {
                        case LLOAD:
                        case DLOAD:
                        case LSTORE:
                        case DSTORE:
                            wideReg = 1;
                            /* fall through */

                        case ALOAD:
                        case ILOAD:
                        case FLOAD:
                        case ASTORE:
                        case ISTORE:
                        case FSTORE:
                            local = Util.demarshal(new byte[]{(byte) ci.byteAt(offset + 1), (byte) ci.byteAt(offset + 2)});
                            break;

                        default:
                            continue;
                    }
                    break;

                default:
                    continue;
            }
            maxLocals = Math.max(maxLocals, local + wideReg + 1);
        }
        return maxLocals;
    }

    @Override
    boolean apply(ClassFile classFile) throws BadBytecode {
        boolean patched = false;
        for (Object o : classFile.getMethods()) {
            MethodInfo methodInfo = (MethodInfo) o;
            classMod.methodInfo = methodInfo;
            if (filterMethod1(methodInfo)) {
                if (apply(methodInfo)) {
                    patched = true;
                }
            }
            classMod.methodInfo = null;
        }
        return patched;
    }

    private static ArrayList<String> bytecodeToString(CodeAttribute ca, int start, int end) {
        ArrayList<String> as = new ArrayList<String>();
        CodeIterator ci = ca.iterator();
        try {
            ci.move(start);
            int pos = ci.next();
            while (pos < end && ci.hasNext()) {
                int next = ci.next();
                String s = Mnemonic.OPCODE[ci.byteAt(pos++)].toUpperCase();
                for (; pos < next; pos++) {
                    s += String.format(" 0x%02x", (int) ci.byteAt(pos));
                }
                as.add(s);
            }
        } catch (Exception e) {
            as.add(e.toString());
        }
        return as;
    }

    private static void logBytecodePatch(ArrayList<String> before, ArrayList<String> after) {
        final String format = "%-24s  %s";
        int max = Math.max(before.size(), after.size());
        for (int i = 0; i < max; i++) {
            Logger.log(Logger.LOG_BYTECODE, format,
                (i < before.size() ? before.get(i) : ""),
                (i < after.size() ? after.get(i) : "")
            );
        }
    }

    /**
     * Get a captured subexpression after a match.  Can only be called in getReplacementBytes.
     *
     * @param group number of capture group, starting at 1
     * @return byte array
     */
    final protected byte[] getCaptureGroup(int group) {
        return matcher.getCaptureGroup(group);
    }

    /**
     * Get matching bytecode string after a match.  Can only be called in getReplacementBytes.
     *
     * @return byte array
     */
    final protected byte[] getMatch() {
        return matcher.getMatch();
    }

    final protected ClassMod.Label label(String key) {
        return new ClassMod.Label(key, true);
    }

    final protected ClassMod.Label branch(String key) {
        return new ClassMod.Label(key, false);
    }

    /**
     * Returns index of this match within a getReplacementBytes method.  Starts at 0 for the first match in
     * a method and resets for each new method.  Incremented even if getReplacementBytes returns null.
     *
     * @return 0-based index
     */
    final protected int getMethodMatchCount() {
        return methodMatchCount;
    }
}

package com.prupe.mcpatcher;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.MethodInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class of all class patches.
 */
abstract public class ClassPatch implements PatchComponent {
    final ClassMod classMod;
    final Map<String, Integer> numMatches = new HashMap<String, Integer>();
    final Set<String> patchedClasses = new HashSet<String>();
    boolean optional;

    public ClassPatch(ClassMod classMod) {
        this.classMod = classMod;
    }

    /**
     * Returns a text description of the patch.  This string will be displayed in the log window and
     * in the patch summary.
     *
     * @return String description
     */
    abstract public String getDescription();

    /**
     * Applies patch to a class file.
     *
     * @param classFile target class file
     * @return true if changes were made
     * @throws BadBytecode              propagated by javassist.bytecode
     * @throws DuplicateMemberException propagated by javassist.bytecode
     * @throws IOException              if an error occurs while writing new bytecode
     */
    abstract boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException, IOException;

    private void addPatch(String desc) {
        int val = 0;
        if (numMatches.containsKey(desc)) {
            val = numMatches.get(desc);
        }
        numMatches.put(desc, val + 1);
    }

    /**
     * Writes a patch to the log and adds it to the patch summary.
     */
    protected void recordPatch() {
        String desc = getDescription();
        addPatch(desc);
        Logger.log(Logger.LOG_PATCH, "%s", desc);
    }

    /**
     * Writes a patch to the log and adds it to the patch summary.
     *
     * @param extra additional text to add to the log output
     */
    protected void recordPatch(String extra) {
        String desc = getDescription();
        addPatch(desc);
        Logger.log(Logger.LOG_PATCH, "%s %s", desc, extra);
    }

    // PatchComponent methods

    final public ClassFile getClassFile() {
        return classMod.getClassFile();
    }

    final public MethodInfo getMethodInfo() {
        return classMod.getMethodInfo();
    }

    final public String buildExpression(Object... objects) {
        return classMod.buildExpression(objects);
    }

    final public byte[] buildCode(Object... objects) {
        return classMod.buildCode(objects);
    }

    final public Object push(Object value) {
        return classMod.push(value);
    }

    final public byte[] reference(int opcode, JavaRef ref) {
        return classMod.reference(opcode, ref);
    }

    final public Mod getMod() {
        return classMod.getMod();
    }

    final public ClassMap getClassMap() {
        return classMod.getClassMap();
    }

    final public JavaRef map(JavaRef ref) {
        return classMod.map(ref);
    }
}

package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Represents a set of patches to be applied to a class.
 * <p/>
 * During the first "analyzing" phase of MCPatcher, each ClassMod is tested against each class file
 * in the input minecraft jar.  With the exception of ones marked 'global,' each ClassMod must
 * match exactly one class.  A Mod can be applied only if all of its ClassMods have target classes.
 * MCPatcher maintains a mapping of deobfuscated class names to their obfuscated names in minecraft.jar.
 * By convention, a ClassMod subclass should have a name ending in "Mod".  The deobfuscated class name
 * is generated from the name of the ClassMod subclass itself by removing the "Mod" from the end.
 * <p/>
 * During the second analyzing phase, MCPatcher resolves each FieldMapper and MethodMapper object.  These
 * mappings are also stored.  The reason for a second pass is so that all classes are resolved before
 * attempting to map any fields or methods.  For example, the TexturePackList class in Minecraft contains a
 * TexturePackBase field, but it cannot be reliably identified until the obfuscated name of TexturePackBase
 * is known.
 * <p/>
 * During patching, MCPatcher applies each ClassPatch within the ClassMod to the target class file.  These
 * do the work of patching bytecode, adding methods, and making members public.  There are also prePatch and
 * postPatch hooks for doing additional processing not covered by one of the ClassPatch subclasses.
 * <p/>
 * The mapping from deobfuscated names to obfuscated names is stored in a ClassMap object accessible to
 * the ClassMod.
 */
abstract public class ClassMod implements PatchComponent {
    final Mod mod;
    final List<String> prerequisiteClasses = new ArrayList<String>();
    final List<com.prupe.mcpatcher.ClassSignature> classSignatures = new ArrayList<com.prupe.mcpatcher.ClassSignature>();
    final List<com.prupe.mcpatcher.ClassPatch> patches = new ArrayList<com.prupe.mcpatcher.ClassPatch>();
    final List<com.prupe.mcpatcher.MemberMapper> memberMappers = new ArrayList<com.prupe.mcpatcher.MemberMapper>();
    boolean global;
    private String parentClass;
    private String[] interfaces;
    final Collection<String> targetClasses = new HashSet<String>();
    final List<String> errors = new ArrayList<String>();
    boolean addToConstPool = false;
    ClassFile classFile;
    MethodInfo methodInfo;
    int bestMatchCount;
    String bestMatch;
    final private List<Label> labels = new ArrayList<Label>();
    private final Map<String, Integer> labelPositions = new HashMap<String, Integer>();
    boolean matchAddedFiles;

    public ClassMod(Mod mod) {
        this.mod = mod;
    }

    boolean matchClassFile(String filename, ClassFile classFile) {
        addToConstPool = false;
        this.classFile = classFile;
        if (!filterFile(filename)) {
            return false;
        }

        ClassMap newMap = new ClassMap();
        String deobfName = getDeobfClass();

        int sigIndex = 0;
        for (com.prupe.mcpatcher.ClassSignature cs : classSignatures) {
            boolean found = false;

            if (cs.match(filename, classFile, newMap)) {
                found = true;
            }

            if (found == cs.negate) {
                return false;
            }
            newMap.addClassMap(deobfName, ClassMap.filenameToClassName(filename));
            if (bestMatch == null || sigIndex > bestMatchCount) {
                bestMatch = filename;
                bestMatchCount = sigIndex;
            }
            sigIndex++;
        }

        targetClasses.add(classFile.getName());
        if (targetClasses.size() == 1 && !global) {
            mod.classMap.merge(newMap);
            if (parentClass != null) {
                mod.classMap.addClassMap(parentClass, classFile.getSuperclass());
                mod.classMap.addInheritance(parentClass, deobfName);
            }
            if (interfaces != null) {
                String[] obfInterfaces = classFile.getInterfaces();
                for (int i = 0; i < Math.min(interfaces.length, obfInterfaces.length); i++) {
                    mod.classMap.addClassMap(interfaces[i], obfInterfaces[i]);
                    mod.classMap.addInterface(interfaces[i], deobfName);
                }
            }
        }

        return true;
    }

    /**
     * Get deobfuscated name of target class.  The default implementation simply strips "Mod" from the end
     * of the ClassMod subclass name itself.
     *
     * @return deobfuscated class name
     */
    public String getDeobfClass() {
        return getClass().getSimpleName().replaceFirst("^_", "").replaceFirst("Mod$", "");
    }

    boolean okToApply() {
        return errors.size() == 0;
    }

    void addError(String error) {
        errors.add(error);
    }

    void pruneTargetClasses() {
        for (com.prupe.mcpatcher.ClassSignature signature : classSignatures) {
            for (Iterator<String> iterator = targetClasses.iterator(); iterator.hasNext(); ) {
                String name = iterator.next();
                if (!signature.confirmMatch(name)) {
                    iterator.remove();
                }
            }
        }
    }

    List<String> getTargetClasses() {
        ArrayList<String> sortedList = new ArrayList<String>(targetClasses.size());
        sortedList.addAll(targetClasses);
        Collections.sort(sortedList);
        return sortedList;
    }

    /**
     * Used to quickly rule out candidate class files based on filename alone.  The default implementation
     * allows only default minecraft classes in the default package or in net.minecraft.
     *
     * @param filename full path of .class file within the .jar
     * @return true if a class file should be considered for patching
     */
    protected boolean filterFile(String filename) {
        String className = ClassMap.filenameToClassName(filename);
        if (global) {
            return !className.startsWith("com.jcraft.") &&
                !className.startsWith("paulscode.") &&
                !className.startsWith("com.fasterxml.") &&
                !className.startsWith("javax.");
        } else {
            return className.startsWith("net.minecraft.") || className.matches("^[a-z]{1,4}$");
        }
    }

    boolean mapClassMembers(String filename, ClassFile classFile) throws Exception {
        boolean ok = true;

        for (MemberMapper mapper : memberMappers) {
            String mapperType = mapper.getMapperType();
            mapper.mapDescriptor(mod.getClassMap());
            List<?> matchingObjects = mapper.getMatchingObjects(classFile);
            if (mapper.reverse) {
                matchingObjects = new ArrayList<Object>(matchingObjects);
                Collections.reverse(matchingObjects);
            }
            for (Object o : matchingObjects) {
                if (o instanceof MethodInfo) {
                    methodInfo = (MethodInfo) o;
                }
                if (mapper.match(o)) {
                    mapper.updateClassMap(getClassMap(), classFile, o);
                    mapper.afterMatch();
                }
                if (o instanceof MethodInfo) {
                    methodInfo = null;
                }
            }
            if (!mapper.allMatched()) {
                addError(String.format("no match found for %s %s", mapperType, mapper.getName()));
                Logger.log(Logger.LOG_METHOD, "no match found for %s %s", mapperType, mapper.getName());
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Pre-patch hook to do any additional processing on the target class before any ClassPatches
     * are applied.
     *
     * @param filename  full path of .class file within the .jar
     * @param classFile current class file
     * @throws Exception
     */
    public void prePatch(String filename, ClassFile classFile) throws Exception {
    }

    /**
     * Post-patch hook to do any additional processing on the target class after all ClassPatches
     * have been applied.
     *
     * @param filename  full path of .class file within the .jar
     * @param classFile current class file
     * @throws Exception
     */
    public void postPatch(String filename, ClassFile classFile) throws Exception {
    }

    public void addPrerequisiteClass(String className) {
        prerequisiteClasses.add(className);
    }

    public void addClassSignature(com.prupe.mcpatcher.ClassSignature classSignature) {
        classSignatures.add(classSignature);
    }

    public void addPatch(com.prupe.mcpatcher.ClassPatch classPatch) {
        patches.add(classPatch);
    }

    public void addMemberMapper(com.prupe.mcpatcher.MemberMapper memberMapper) {
        memberMappers.add(memberMapper);
    }

    public void setMultipleMatchesAllowed(boolean match) {
        global = match;
    }

    public void setMatchAddedFiles(boolean match) {
        if (match) {
            matchAddedFiles = true;
            setMultipleMatchesAllowed(true);
        } else {
            matchAddedFiles = false;
        }
    }

    public void setParentClass(String className) {
        parentClass = className;
    }

    public void setInterfaces(String... interfaces) {
        this.interfaces = interfaces.clone();
    }

    final public static class Label {
        final String name;
        final boolean save;
        int from;

        Label(String name, boolean save) {
            this.name = name;
            this.save = save;
        }
    }

    final public Label label(String key) {
        return new Label(key, true);
    }

    final public Label branch(String key) {
        return new Label(key, false);
    }

    void resetLabels() {
        labels.clear();
        labelPositions.clear();
    }

    void resolveLabels(byte[] code, int start, int labelOffset) {
        for (Map.Entry<String, Integer> e : labelPositions.entrySet()) {
            Logger.log(Logger.LOG_BYTECODE, "label %s -> instruction %d", e.getKey(), start + e.getValue());
        }
        for (Label label : labels) {
            if (!labelPositions.containsKey(label.name)) {
                throw new RuntimeException("no label " + label.name + " defined");
            }
            int to = labelPositions.get(label.name);
            int diff = to - label.from + 1;
            int codepos = label.from + labelOffset;
            Logger.log(Logger.LOG_BYTECODE, "branch offset %s %s -> %+d @%d",
                Mnemonic.OPCODE[code[codepos - 1] & 0xff].toUpperCase(), label.name, diff, label.from - 1 + start
            );
            code[codepos] = Util.b(diff, 1);
            code[codepos + 1] = Util.b(diff, 0);
        }
    }

    // PatchComponent methods

    final public ClassFile getClassFile() {
        return classFile;
    }

    final public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    final public String buildExpression(Object... objects) {
        return BinaryRegex.build(objects);
    }

    final public byte[] buildCode(Object... objects) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            buildCode1(baos, objects);
        } catch (NullPointerException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    private void buildCode1(ByteArrayOutputStream baos, Object[] objects) throws IOException {
        for (Object o : objects) {
            if (o instanceof Byte) {
                baos.write((Byte) o);
            } else if (o instanceof byte[]) {
                baos.write((byte[]) o);
            } else if (o instanceof Integer) {
                baos.write((Integer) o);
            } else if (o instanceof int[]) {
                for (int i : (int[]) o) {
                    baos.write(i);
                }
            } else if (o instanceof Label) {
                Label label = (Label) o;
                if (label.save) {
                    int offset = baos.size();
                    if (labelPositions.containsKey(label.name)) {
                        throw new RuntimeException("label " + label.name + " already defined");
                    }
                    labelPositions.put(label.name, offset);
                } else {
                    label.from = baos.size();
                    labels.add(label);
                    baos.write(0);
                    baos.write(0);
                }
            } else if (o instanceof Object[]) {
                buildCode1(baos, (Object[]) o);
            } else {
                throw new AssertionError("invalid type: " + o.getClass().toString());
            }
        }
    }

    final public Object push(Object value) {
        return ConstPoolUtils.push(getMethodInfo().getConstPool(), value, addToConstPool);
    }

    final public byte[] reference(int opcode, JavaRef ref) {
        return ConstPoolUtils.reference(getMethodInfo().getConstPool(), opcode, map(ref), addToConstPool);
    }

    final public Mod getMod() {
        return mod;
    }

    final public ClassMap getClassMap() {
        return mod.getClassMap();
    }

    final public JavaRef map(JavaRef ref) {
        return mod.getClassMap().map(ref);
    }

    // inner class versions of ClassSignature and its subclasses

    abstract public class ClassSignature extends com.prupe.mcpatcher.ClassSignature {
        public ClassSignature() {
            super(ClassMod.this);
        }
    }

    public class AncestorClassSignature extends com.prupe.mcpatcher.AncestorClassSignature {
        public AncestorClassSignature(String baseClass) {
            super(ClassMod.this, baseClass);
        }
    }

    public class ConstSignature extends com.prupe.mcpatcher.ConstSignature {
        public ConstSignature(Object value) {
            super(ClassMod.this, value);
        }
    }

    public class FilenameSignature extends com.prupe.mcpatcher.FilenameSignature {
        public FilenameSignature(String filename) {
            super(ClassMod.this, filename);
        }
    }

    abstract public class BytecodeSignature extends com.prupe.mcpatcher.BytecodeSignature {
        public BytecodeSignature() {
            super(ClassMod.this);
        }
    }

    public class FixedBytecodeSignature extends com.prupe.mcpatcher.FixedBytecodeSignature {
        public FixedBytecodeSignature(Object... objects) {
            super(ClassMod.this, objects);
        }
    }

    public class InterfaceSignature extends com.prupe.mcpatcher.InterfaceSignature {
        public InterfaceSignature(JavaRef... methods) {
            super(ClassMod.this, methods);
        }

        public InterfaceSignature(List<JavaRef> methods) {
            super(ClassMod.this, methods);
        }
    }

    public class OrSignature extends com.prupe.mcpatcher.OrSignature {
        public OrSignature(com.prupe.mcpatcher.ClassSignature... signatures) {
            super(ClassMod.this, signatures);
        }
    }

    // inner class versions of MemberMapper and its subclasses

    public class FieldMapper extends com.prupe.mcpatcher.FieldMapper {
        public FieldMapper(FieldRef... refs) {
            super(ClassMod.this, refs);
        }
    }

    public class MethodMapper extends com.prupe.mcpatcher.MethodMapper {
        public MethodMapper(MethodRef... refs) {
            super(ClassMod.this, refs);
        }
    }

    // inner class versions of ClassPatch and its subclasses

    abstract public class ClassPatch extends com.prupe.mcpatcher.ClassPatch {
        public ClassPatch() {
            super(ClassMod.this);
        }
    }

    abstract public class BytecodePatch extends com.prupe.mcpatcher.BytecodePatch {
        public BytecodePatch() {
            super(ClassMod.this);
        }
    }

    public class AddFieldPatch extends com.prupe.mcpatcher.AddFieldPatch {
        public AddFieldPatch(FieldRef fieldRef) {
            super(ClassMod.this, fieldRef);
        }

        public AddFieldPatch(FieldRef fieldRef, int accessFlags) {
            super(ClassMod.this, fieldRef, accessFlags);
        }

        public AddFieldPatch(String name) {
            super(ClassMod.this, name);
        }

        public AddFieldPatch(String name, int accessFlags) {
            super(ClassMod.this, name, accessFlags);
        }
    }

    abstract public class AddMethodPatch extends com.prupe.mcpatcher.AddMethodPatch {
        public AddMethodPatch(MethodRef methodRef) {
            super(ClassMod.this, methodRef);
        }

        public AddMethodPatch(MethodRef methodRef, int accessFlags) {
            super(ClassMod.this, methodRef, accessFlags);
        }

        public AddMethodPatch(String name) {
            super(ClassMod.this, name);
        }

        public AddMethodPatch(String name, int accessFlags) {
            super(ClassMod.this, name, accessFlags);
        }
    }

    public class MakeMemberPublicPatch extends com.prupe.mcpatcher.MakeMemberPublicPatch {
        public MakeMemberPublicPatch(JavaRef ref) {
            super(ClassMod.this, ref);
        }
    }
}
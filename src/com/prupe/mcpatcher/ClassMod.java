package com.prupe.mcpatcher;

import javassist.bytecode.*;

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
    final List<ClassPatch> patches = new ArrayList<ClassPatch>();
    final List<MemberMapper> memberMappers = new ArrayList<MemberMapper>();
    boolean global = false;
    String parentClass;
    String[] interfaces;
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

    ClassMod(Mod mod) {
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
            for (Object o : mapper.getMatchingObjects(classFile)) {
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

    protected void addPrerequisiteClass(String className) {
        prerequisiteClasses.add(className);
    }

    protected void addClassSignature(com.prupe.mcpatcher.ClassSignature classSignature) {
        classSignatures.add(classSignature);
    }

    protected void addPatch(ClassPatch classPatch) {
        classPatch.classMod = this;
        patches.add(classPatch);
    }

    protected void addMemberMapper(MemberMapper memberMapper) {
        memberMappers.add(memberMapper);
    }

    protected void setMultipleMatchesAllowed(boolean match) {
        global = match;
    }

    protected void setMatchAddedFiles(boolean match) {
        if (match) {
            matchAddedFiles = true;
            setMultipleMatchesAllowed(true);
        } else {
            matchAddedFiles = false;
        }
    }

    protected void setParentClass(String className) {
        parentClass = className;
    }

    protected void setInterfaces(String... interfaces) {
        this.interfaces = interfaces.clone();
    }

    final public static class Label {
        String name;
        boolean save;
        int from;

        Label(String name, boolean save) {
            this.name = name;
            this.save = save;
        }
    }

    final protected Label label(String key) {
        return new Label(key, true);
    }

    final protected Label branch(String key) {
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

    /**
     * Represents a field or method to be located within a class.  By default,
     * the match is done by type signature, but this can be overridden.
     */
    public abstract class MemberMapper {
        /**
         * Deobfuscated members.
         */
        protected JavaRef[] refs;
        /**
         * Java type descriptor, e.g.,<br>
         * "[B" represents an array of bytes.<br>
         * "(I)Lnet/minecraft/client/Minecraft;" represents a method taking an int and returning a Minecraft object.
         */
        protected String descriptor;

        private int mapSuperclass;
        int mapInterface = -1;

        private int setAccessFlags;
        private int clearAccessFlags;
        private int count;

        MemberMapper(JavaRef... refs) {
            this.refs = refs.clone();
            for (JavaRef ref : refs) {
                if (ref != null && ref.getType() != null) {
                    return;
                }
            }
            throw new RuntimeException("refs list has no descriptor");
        }

        /**
         * Specify a required access flag.
         *
         * @param flags access flags
         * @param set   if true, flags are required; if false, flags are forbidden
         * @return this
         * @see AccessFlag
         */
        public MemberMapper accessFlag(int flags, boolean set) {
            if (set) {
                setAccessFlags |= flags;
            } else {
                clearAccessFlags |= flags;
            }
            return this;
        }

        public MemberMapper mapToSuperclass(int ancestry) {
            if (ancestry > 1) {
                throw new IllegalArgumentException("ancestry " + ancestry + " is not supported");
            }
            this.mapSuperclass = ancestry;
            return this;
        }

        void mapDescriptor(ClassMap classMap) {
            count = 0;
            for (JavaRef ref : refs) {
                if (ref != null && ref.getType() != null) {
                    descriptor = classMap.mapTypeString(ref.getType());
                    return;
                }
            }
        }

        boolean matchInfo(String descriptor, int flags) {
            return descriptor.equals(this.descriptor) &&
                (flags & setAccessFlags) == setAccessFlags &&
                (flags & clearAccessFlags) == 0;
        }

        JavaRef getRef() {
            return count < refs.length ? refs[count] : null;
        }

        String getClassName() {
            JavaRef ref = getRef();
            if (ref == null)
                return null;
            else if (ref.getClassName() == null || ref.getClassName().equals("")) {
                return getDeobfClass();
            } else {
                return ref.getClassName();
            }
        }

        String getName() {
            JavaRef ref = getRef();
            return ref == null ? null : ref.getName();
        }

        void afterMatch() {
            count++;
        }

        boolean allMatched() {
            return count >= refs.length;
        }

        abstract protected String getMapperType();

        abstract protected List getMatchingObjects(ClassFile classFile);

        abstract protected boolean match(Object o);

        abstract protected JavaRef getObfRef(String className, Object o);

        abstract protected String[] describeMatch(Object o);

        protected void updateClassMap(ClassMap classMap, ClassFile classFile, Object o) {
            JavaRef ref = getRef();
            if (ref != null) {
                String obfClassName;
                String prefix;
                if (mapSuperclass == 1) {
                    obfClassName = classFile.getSuperclass();
                    prefix = getClassName() + '.';
                } else if (mapInterface >= 0) {
                    obfClassName = classFile.getInterfaces()[mapInterface];
                    prefix = getClassName() + '.';
                } else {
                    obfClassName = classFile.getName();
                    prefix = "";
                }
                JavaRef obfRef = getObfRef(obfClassName, o);
                String[] s = describeMatch(o);
                Logger.log(Logger.LOG_FIELD, "%s %s matches %s%s %s", getMapperType(), ref.getName(), prefix, s[0], s[1]);
                classMap.addMap(ref, obfRef);
            }
        }
    }

    /**
     * Represents a field to be located within a class.  By default,
     * the match is done by type signature, but this can be overridden.
     */
    public class FieldMapper extends MemberMapper {
        public FieldMapper(FieldRef... refs) {
            super(refs);
        }

        protected final String getMapperType() {
            return "field";
        }

        protected boolean match(Object o) {
            FieldInfo fieldInfo = (FieldInfo) o;
            return matchInfo(fieldInfo.getDescriptor(), fieldInfo.getAccessFlags());
        }

        protected JavaRef getObfRef(String className, Object o) {
            FieldInfo fieldInfo = (FieldInfo) o;
            return new FieldRef(className, fieldInfo.getName(), fieldInfo.getDescriptor());
        }

        protected String[] describeMatch(Object o) {
            FieldInfo fieldInfo = (FieldInfo) o;
            return new String[]{fieldInfo.getName(), fieldInfo.getDescriptor()};
        }

        protected List getMatchingObjects(ClassFile classFile) {
            return classFile.getFields();
        }
    }

    /**
     * Represents a method to be located within a class.  By default,
     * the match is done by type signature, but this can be overridden.
     */
    public class MethodMapper extends MemberMapper {
        public MethodMapper(MethodRef... refs) {
            super(refs);
        }

        public MethodMapper mapToInterface(int mapInterface) {
            this.mapInterface = mapInterface;
            return this;
        }

        protected final String getMapperType() {
            return "method";
        }

        protected boolean match(Object o) {
            MethodInfo methodInfo = (MethodInfo) o;
            return !methodInfo.isConstructor() && !methodInfo.isStaticInitializer() &&
                matchInfo(methodInfo.getDescriptor(), methodInfo.getAccessFlags());
        }

        protected JavaRef getObfRef(String className, Object o) {
            MethodInfo methodInfo = (MethodInfo) o;
            return new MethodRef(className, methodInfo.getName(), methodInfo.getDescriptor());
        }

        protected String[] describeMatch(Object o) {
            MethodInfo methodInfo = (MethodInfo) o;
            return new String[]{methodInfo.getName(), methodInfo.getDescriptor()};
        }

        protected List getMatchingObjects(ClassFile classFile) {
            return classFile.getMethods();
        }
    }

    abstract public class ClassSignature extends com.prupe.mcpatcher.ClassSignature {
        public ClassSignature() {
            super(ClassMod.this);
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
    }

    public class OrSignature extends com.prupe.mcpatcher.OrSignature {
        public OrSignature(com.prupe.mcpatcher.ClassSignature... signatures) {
            super(ClassMod.this, signatures);
        }
    }
}
package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterfaceSignature extends ClassSignature {
    private final JavaRef[] methods;
    private int classType = 1;
    private boolean exactMatch;

    public InterfaceSignature(ClassMod classMod, JavaRef... methods) {
        this(classMod, Arrays.asList(methods));
    }

    public InterfaceSignature(ClassMod classMod, List<JavaRef> methods) {
        super(classMod);
        List<JavaRef> tmpMethods = new ArrayList<JavaRef>();
        for (JavaRef method : methods) {
            if (method == null) {
                // nothing
            } else if (method instanceof MethodRef || method instanceof InterfaceMethodRef) {
                tmpMethods.add(method);
            } else {
                throw new IllegalArgumentException("invalid type " + method.getClass().getSimpleName());
            }
        }
        this.methods = tmpMethods.toArray(new JavaRef[tmpMethods.size()]);
    }

    public InterfaceSignature setInterfaceOnly(boolean interfaceOnly) {
        classType = interfaceOnly ? 1 : 0;
        return this;
    }

    public InterfaceSignature setAbstractOnly(boolean abstractOnly) {
        classType = abstractOnly ? 2 : 0;
        return this;
    }

    public InterfaceSignature setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        if (classType == 1 && !classFile.isInterface()) {
            return false;
        }
        if (classType == 2 && !classFile.isAbstract()) {
            return false;
        }
        final List obfMethods = classFile.getMethods();
        if (obfMethods.size() < methods.length) {
            return false;
        }
        if (exactMatch && obfMethods.size() > methods.length) {
            return false;
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < methods.length; j++) {
                JavaRef method = methods[j];
                MethodInfo obfMethod = (MethodInfo) obfMethods.get(j);
                MethodRef obfMethodRef = new MethodRef(classFile.getName(), obfMethod.getName(), obfMethod.getDescriptor());
                switch (i) {
                    case 0:
                        if (!isPotentialTypeMatch(getClassMap(), method.getParsedDescriptor(), obfMethodRef.getParsedDescriptor())) {
                            return false;
                        }
                        break;

                    case 1:
                        String source = mapSource + " InterfaceSignature";
                        tempClassMap.addClassMap(classMod.getDeobfClass(), classFile.getName(), source);
                        tempClassMap.addMethodMap(classMod.getDeobfClass(), method.getName(), obfMethod.getName(), obfMethod.getDescriptor(), source);
                        tempClassMap.addTypeDescriptorMap(method.getType(), obfMethod.getDescriptor(), source);
                        break;

                    default:
                        break;
                }
            }
        }
        return true;
    }

    static boolean isPotentialTypeMatch(ClassMap classMap, ArrayList<String> deobfTypes, ArrayList<String> obfTypes) {
        if (deobfTypes == null && obfTypes == null) {
            return true;
        } else if (deobfTypes == null || obfTypes == null) {
            return false;
        } else if (deobfTypes.size() != obfTypes.size()) {
            return false;
        }
        for (int i = 0; i < deobfTypes.size(); i++) {
            String deobfType = deobfTypes.get(i);
            String obfType = obfTypes.get(i);
            String deobfClass = deobfType.replaceFirst("^\\[+", "");
            String obfClass = obfType.replaceFirst("^\\[+", "");
            if (deobfType.length() - deobfClass.length() != obfType.length() - obfClass.length()) {
                return false;
            }
            if (deobfClass.charAt(0) == 'L' && obfClass.charAt(0) == 'L') {
                deobfClass = ClassMap.descriptorToClassName(deobfClass);
                obfClass = ClassMap.descriptorToClassName(obfClass);
                boolean deobfIsMC = !deobfClass.contains(".") || deobfClass.startsWith("net.minecraft.");
                boolean obfIsMC = !obfClass.matches(".*[^a-z$0-9].*") || obfClass.startsWith("net.minecraft.");
                if (deobfIsMC != obfIsMC) {
                    return false;
                } else if (deobfIsMC) {
                    if (classMap.hasMap(deobfClass)) {
                        String deobfMapping = classMap.map(deobfClass).replace('/', '.');
                        if (!deobfMapping.equals(obfClass)) {
                            return false;
                        }
                    }
                } else if (!deobfClass.equals(obfClass)) {
                    return false;
                }
            } else if (!deobfClass.equals(obfClass)) {
                return false;
            }
        }
        return true;
    }

    static boolean isPotentialTypeMatch(ClassMap classMap, String deobfDesc, String obfDesc) {
        return (deobfDesc == null && obfDesc == null) ||
            isPotentialTypeMatch(classMap, ConstPoolUtils.parseDescriptor(deobfDesc), ConstPoolUtils.parseDescriptor(obfDesc));
    }
}

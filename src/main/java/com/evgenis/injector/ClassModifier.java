package com.evgenis.injector;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

public class ClassModifier {
    private static final String FULL_NAME_DELIMITER = "/";

    private ClassNode classNode;

    public ClassModifier(byte[] bytes) {
        updateClassNode(bytes);
        modifyClassAccess();
        modifyMethodsAccess();
    }

    private void updateClassNode(byte[] bytes) {
        classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, ClassReader.EXPAND_FRAMES);
    }

    public void renameClass(String newName) {
        Map<String, String> mappings = new HashMap<>();
        String[] segments = classNode.name.split(FULL_NAME_DELIMITER);
        segments[segments.length - 1] = newName;
        String newFullName = String.join(FULL_NAME_DELIMITER, segments);
        mappings.put(classNode.name, newFullName);
        Remapper mapper = new SimpleRemapper(mappings);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(new ClassRemapper(cw, mapper));
        updateClassNode(cw.toByteArray());
    }

    private void modifyClassAccess() {
        classNode.access &= ~Opcodes.ACC_FINAL;
        updateClassNode(getBytes());
    }

    private void modifyMethodsAccess() {
        for (MethodNode methodNode : classNode.methods) {
            methodNode.access &= ~Opcodes.ACC_PRIVATE;
            methodNode.access &= ~Opcodes.ACC_FINAL;
        }
        updateClassNode(getBytes());
    }

    public byte[] getBytes() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}

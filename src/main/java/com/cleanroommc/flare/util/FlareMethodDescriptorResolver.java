package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.node.type.StackTraceNode;
import com.cleanroommc.flare.api.util.ClassFinder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FlareMethodDescriptorResolver extends MethodDescriptorResolver {

    protected final Map<Class<?>, Map<String, List<MethodNode>>> cache = new Object2ObjectOpenHashMap<>();

    @Override
    @Nullable
    public String resolve(StackTraceNode node) {
        if (node.methodDescription() != null) {
            return node.methodDescription();
        }
        String resourceName = node.className().replace('/', '.');
        Class<?> clazz = ClassFinder.findClass(resourceName);
        if (clazz == null) {
            return null;
        }
        Map<String, List<MethodNode>> cachedMethods = cache.get(clazz);
        if (cachedMethods == null) {
            cache.put(clazz, cachedMethods = new Object2ObjectOpenHashMap<>());
            try (InputStream is = openInputStreamForClass(clazz, resourceName)) {
                if (is != null) {
                    ClassNode classNode = new ClassNode();
                    ClassReader reader = new ClassReader(is);
                    reader.accept(classNode, 0);
                    for (MethodNode methodNode : classNode.methods) {
                        cachedMethods.computeIfAbsent(methodNode.name, k -> new ArrayList<>()).add(methodNode);
                    }
                }
            } catch (IOException ignored) { }
        }
        List<MethodNode> methodNodes = cachedMethods.get(node.methodName());
        if (methodNodes == null) {
            return null;
        }
        for (MethodNode methodNode : methodNodes) {
            for (Iterator<AbstractInsnNode> iter = methodNode.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insnNode = iter.next();
                if (insnNode instanceof LineNumberNode && node.lineNumber() == ((LineNumberNode) insnNode).line) {
                    return methodNode.desc;
                }
            }
        }
        return null;
    }

}

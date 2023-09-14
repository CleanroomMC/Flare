package com.cleanroommc.flare.api.sampler.source;

import com.cleanroommc.flare.api.sampler.node.type.SamplingStackNode;
import com.cleanroommc.flare.api.sampler.node.type.StackTraceNode;
import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.api.util.ClassFinder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A function which defines the source of given {@link Class}es or (Mixin) method calls.
 */
public interface ClassSourceLookup {

    /**
     * Identify the given class.
     *
     * @param className the class
     * @return the source of the class
     */
    @Nullable String identify(String className) throws Exception;

    /**
     * Identify the given method call.
     *
     * @param methodCall the method call info
     * @return the source of the method call
     */
    default @Nullable String identify(MethodCall methodCall) throws Exception {
        return null;
    }

    /**
     * Identify the given method call.
     *
     * @param methodCall the method call info
     * @return the source of the method call
     */
    default @Nullable String identify(MethodCallByLine methodCall) throws Exception {
        return null;
    }

    /**
     * A no-operation {@link ClassSourceLookup}.
     */
    ClassSourceLookup NO_OP = new ClassSourceLookup() {

        @Override
        public @Nullable String identify(String className) {
            return null;
        }

    };

    /**
     * A {@link ClassSourceLookup} which identifies classes based on their {@link ClassLoader}.
     */
    abstract class ByClassLoader implements ClassSourceLookup {

        public abstract @Nullable String identify(ClassLoader loader) throws Exception;

        @Override
        public final @Nullable String identify(String className) throws Exception {
            Class<?> clazz = ClassFinder.findClass(className);
            if (clazz == null) {
                return null;
            }
            ClassLoader loader = clazz.getClassLoader();
            while (loader != null) {
                String source = identify(loader);
                if (source != null) {
                    return source;
                }
                loader = loader.getParent();
            }
            return null;
        }

    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on URL.
     */
    interface ByUrl extends ClassSourceLookup {

        default String identifyUrl(URL url) throws URISyntaxException, MalformedURLException {
            Path path = null;
            String protocol = url.getProtocol();
            if (protocol.equals("file")) {
                path = Paths.get(url.toURI());
            } else if (protocol.equals("jar")) {
                URL innerUrl = new URL(url.getPath());
                path = Paths.get(innerUrl.getPath().split("!")[0]);
            }

            if (path != null) {
                return identifyFile(path.toAbsolutePath().normalize());
            }
            return null;
        }

        default String identifyFile(Path path) {
            return identifyFileName(path.getFileName().toString());
        }

        default String identifyFileName(String fileName) {
            return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : null;
        }

    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on the first URL in a {@link URLClassLoader}.
     */
    class ByFirstUrlSource extends ClassSourceLookup.ByClassLoader implements ClassSourceLookup.ByUrl {

        @Override
        public @Nullable String identify(ClassLoader loader) throws IOException, URISyntaxException {
            if (loader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) loader;
                URL[] urls = urlClassLoader.getURLs();
                if (urls.length == 0) {
                    return null;
                }
                return identifyUrl(urls[0]);
            }
            return null;
        }

    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on their {@link ProtectionDomain#getCodeSource()}.
     */
    class ByCodeSource implements ClassSourceLookup, ClassSourceLookup.ByUrl {

        @Override
        public @Nullable String identify(String className) throws URISyntaxException, MalformedURLException {
            Class<?> clazz = ClassFinder.findClass(className);
            if (clazz == null) {
                return null;
            }
            ProtectionDomain protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain == null) {
                return null;
            }
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource == null) {
                return null;
            }

            URL url = codeSource.getLocation();
            return url == null ? null : identifyUrl(url);
        }

    }

    interface Visitor {

        void visit(ThreadNode node);

        boolean hasClassSourceMappings();

        Map<String, String> getClassSourceMapping();

        boolean hasMethodSourceMappings();

        Map<String, String> getMethodSourceMapping();

        boolean hasLineSourceMappings();

        Map<String, String> getLineSourceMapping();

    }

    static Visitor createVisitor(ClassSourceLookup lookup) {
        if (lookup == ClassSourceLookup.NO_OP) {
            return NoOpVisitor.INSTANCE; // Don't bother!
        }
        return new VisitorImpl(lookup);
    }

    final class NoOpVisitor implements Visitor {

        public static final NoOpVisitor INSTANCE = new NoOpVisitor();

        private NoOpVisitor() { }

        @Override
        public void visit(ThreadNode node) { }

        @Override
        public boolean hasClassSourceMappings() {
            return false;
        }

        @Override
        public Map<String, String> getClassSourceMapping() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasMethodSourceMappings() {
            return false;
        }

        @Override
        public Map<String, String> getMethodSourceMapping() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasLineSourceMappings() {
            return false;
        }

        @Override
        public Map<String, String> getLineSourceMapping() {
            return Collections.emptyMap();
        }

    }

    /**
     * Visitor which scans {@link StackTraceNode}s and accumulates class/method call identities.
     */
    class VisitorImpl implements Visitor {

        private final ClassSourceLookup lookup;

        private final SourcesMap<String> classSources = new SourcesMap<>(Function.identity());
        private final SourcesMap<MethodCall> methodSources = new SourcesMap<>(MethodCall::toString);
        private final SourcesMap<MethodCallByLine> lineSources = new SourcesMap<>(MethodCallByLine::toString);

        VisitorImpl(ClassSourceLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public void visit(ThreadNode node) {
            Queue<SamplingStackNode> queue = new ArrayDeque<>(node.children());
            for (SamplingStackNode n = queue.poll(); n != null; n = queue.poll()) {
                if (n instanceof StackTraceNode) {
                    visitStackNode((StackTraceNode) n);
                    queue.addAll(n.children());
                }
            }
        }

        private void visitStackNode(StackTraceNode node) {
            this.classSources.computeIfAbsent(node.className(), this.lookup::identify);
            if (node.methodDescription() != null) {
                MethodCall methodCall = new MethodCall(node.className(), node.methodName(), node.methodDescription());
                this.methodSources.computeIfAbsent(methodCall, this.lookup::identify);
            } else {
                MethodCallByLine methodCall = new MethodCallByLine(node.className(), node.methodName(), node.lineNumber());
                this.lineSources.computeIfAbsent(methodCall, this.lookup::identify);
            }

        }

        @Override
        public boolean hasClassSourceMappings() {
            return this.classSources.hasMappings();
        }

        @Override
        public Map<String, String> getClassSourceMapping() {
            return this.classSources.export();
        }

        @Override
        public boolean hasMethodSourceMappings() {
            return this.methodSources.hasMappings();
        }

        @Override
        public Map<String, String> getMethodSourceMapping() {
            return this.methodSources.export();
        }

        @Override
        public boolean hasLineSourceMappings() {
            return this.lineSources.hasMappings();
        }

        @Override
        public Map<String, String> getLineSourceMapping() {
            return this.lineSources.export();
        }

    }

    final class SourcesMap<T> {

        // <key> --> identifier (plugin name)
        private final Map<T, String> map = new Object2ObjectOpenHashMap<>();
        private final Function<? super T, String> keyToStringFunction;

        private SourcesMap(Function<? super T, String> keyToStringFunction) {
            this.keyToStringFunction = keyToStringFunction;
        }

        public void computeIfAbsent(T key, ComputeSourceFunction<T> function) {
            if (!this.map.containsKey(key)) {
                try {
                    this.map.put(key, function.compute(key));
                } catch (Throwable e) {
                    this.map.put(key, null);
                }
            }
        }

        public boolean hasMappings() {
            this.map.values().removeIf(Objects::isNull);
            return !this.map.isEmpty();
        }

        public Map<String, String> export() {
            this.map.values().removeIf(Objects::isNull);
            if (this.keyToStringFunction.equals(Function.identity())) {
                //noinspection unchecked
                return (Map<String, String>) this.map;
            } else {
                return this.map.entrySet().stream().collect(Collectors.toMap(
                        e -> this.keyToStringFunction.apply(e.getKey()),
                        Map.Entry::getValue
                ));
            }
        }

        private interface ComputeSourceFunction<T> {

            String compute(T key) throws Exception;

        }

    }

    /**
     * Encapsulates information about a given method call using the name + method description.
     */
    final class MethodCall {

        private final String className;
        private final String methodName;
        private final String methodDescriptor;

        public MethodCall(String className, String methodName, String methodDescriptor) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
        }

        public String getClassName() {
            return this.className;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public String getMethodDescriptor() {
            return this.methodDescriptor;
        }

        @Override
        public String toString() {
            return this.className + ";" + this.methodName + ";" + this.methodDescriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCall)) return false;
            MethodCall that = (MethodCall) o;
            return this.className.equals(that.className) &&
                    this.methodName.equals(that.methodName) &&
                    this.methodDescriptor.equals(that.methodDescriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.className, this.methodName, this.methodDescriptor);
        }

    }

    /**
     * Encapsulates information about a given method call using the name + line number.
     */
    final class MethodCallByLine {

        private final String className;
        private final String methodName;
        private final int lineNumber;

        public MethodCallByLine(String className, String methodName, int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }

        public String getClassName() {
            return this.className;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public int getLineNumber() {
            return this.lineNumber;
        }

        @Override
        public String toString() {
            return this.className + ";" + this.lineNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCallByLine)) return false;
            MethodCallByLine that = (MethodCallByLine) o;
            return this.lineNumber == that.lineNumber && this.className.equals(that.className);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.className, this.lineNumber);
        }

    }

}

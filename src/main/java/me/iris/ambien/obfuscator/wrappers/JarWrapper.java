package me.iris.ambien.obfuscator.wrappers;

import lombok.Getter;
import me.iris.ambien.obfuscator.Ambien;
import me.iris.ambien.obfuscator.transformers.TransformerManager;
import me.iris.ambien.obfuscator.transformers.implementations.exploits.Crasher;
import me.iris.ambien.obfuscator.transformers.implementations.miscellaneous.Remapper;
import me.iris.ambien.obfuscator.transformers.implementations.packaging.DuplicateResources;
import me.iris.ambien.obfuscator.transformers.implementations.packaging.FolderClasses;
import me.iris.ambien.obfuscator.transformers.implementations.packaging.Metadata;
import me.iris.ambien.obfuscator.utilities.IOUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.Deflater;

@SuppressWarnings("resource")
public class JarWrapper {
    @Getter
    private final List<String> directories;

    @Getter
    private final List<ClassWrapper> classes;

    @Getter
    private final HashMap<String, byte[]> resources;

    @Getter
    private final List<ByteArrayOutputStream> outputStreams;

    public JarWrapper() {
        this.directories = new ArrayList<>();
        this.classes = new ArrayList<>();
        this.resources = new HashMap<>();
        this.outputStreams = new ArrayList<>();
    }

    public JarWrapper from(final File file) throws IOException {
        if (!file.exists())
            throw new RuntimeException("Input jar file doesn't exist.");

        if (!file.getName().endsWith(".jar"))
            throw new RuntimeException("Input jar isn't a jar file.");

        // Convert file to jar file
        final JarFile jarFile = new JarFile(file);
        Ambien.LOGGER.info("Loading jar: " + jarFile.getName());

        // Get jar file entries
        final Enumeration<JarEntry> entries = jarFile.entries();

        // Enumerate
        while (entries.hasMoreElements()) {
            // Get element
            final JarEntry entry = entries.nextElement();
            final String name = entry.getName();
            final InputStream stream = jarFile.getInputStream(entry);

            // Load entry
            if (name.endsWith(".class")) {
                // Read stream into node
                final ClassReader reader = new ClassReader(stream);
                final ClassNode node = new ClassNode();
                reader.accept(node, ClassReader.SKIP_FRAMES);

                classes.add(new ClassWrapper(name, node, false));
                Ambien.LOGGER.debug("Loaded class: {}", name);
            } else if (name.endsWith("/"))
                directories.add(name);
            else {
                final byte[] bytes = IOUtil.streamToArray(stream);
                resources.put(name, bytes);
                Ambien.LOGGER.info("Loaded resource: {}", name);
            }
        }

        // Return wrapper
        return this;
    }

    public JarWrapper importLibrary(final String path) throws IOException {
        final File file = new File(path);

        if (!file.exists())
            throw new RuntimeException(String.format("Library \"%s\" doesn't exist.", path));

        if (file.isDirectory()) {
            File[] jarFiles = file.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null) {
                throw new RuntimeException(String.format("No .jar files found in directory \"%s\".", path));
            }

            for (File jarFile : jarFiles) {
                importJar(jarFile);
            }
        } else if (file.getName().endsWith(".jar")) {
            importJar(file);
        } else {
            throw new RuntimeException(String.format("Library \"%s\" isn't a .jar file or a directory.", path));
        }

        return this;
    }

    private void importJar(File jarFile) throws IOException {
        final JarFile jar = new JarFile(jarFile);
        Ambien.LOGGER.info("Loading library: " + jar.getName());

        final Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String name = entry.getName();
            final InputStream stream = jar.getInputStream(entry);

            if (name.endsWith(".class")) {
                final ClassReader reader = new ClassReader(stream);
                final ClassNode node = new ClassNode();
                reader.accept(node, ClassReader.SKIP_FRAMES);

                classes.add(new ClassWrapper(name, node, true));
                Ambien.LOGGER.debug("Loaded class: {}", name);
            }
        }
    }

    public void to() throws IOException {
        // File writer for all our output streams
        final FileOutputStream fileOutputStream = new FileOutputStream(Ambien.get.outputJar);

        // Write custom output streams
        if (!outputStreams.isEmpty()) {
            for (ByteArrayOutputStream outputStream : outputStreams) {
                fileOutputStream.write(outputStream.toByteArray());
            }

            Ambien.LOGGER.debug("Added {} extra output streams", outputStreams.size());
        }

        // Write main jar stream
        // Create output stream
        final JarOutputStream stream = new JarOutputStream(fileOutputStream);

        // Set compression level
        if (Ambien.get.transformerManager.getTransformer("aggressive-compression").isEnabled())
            stream.setLevel(Deflater.BEST_COMPRESSION);
        else
            stream.setLevel(Deflater.DEFAULT_COMPRESSION);

        // Add directories
        /*directories.forEach(directory -> {
            try {
                IOUtil.writeDirectoryEntry(stream, directory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });*/ // ш did it simply so that there were no empty folders after the remap. Nothing seems to break ;)

        // Add resources
        resources.forEach((name, bytes) -> {
            try {
                TransformerManager t = Ambien.get.transformerManager;
                AtomicReference<String> modifiedBytes = new AtomicReference<>(new String(bytes, StandardCharsets.UTF_8));

                if (t.getTransformer("remapper").isEnabled() && Remapper.classes.isEnabled() && !Remapper.map.isEmpty()) {
                    if (!(name.contains(".jar") || (name.contains(".dll") && !name.endsWith("\u0000")))) {
                        Remapper.map.forEach((oldName, newName) -> {
                            modifiedBytes.set(modifiedBytes.get()
                                    .replace(oldName, newName)
                                    .replace(oldName.replace('/', '.'), newName.replace('/', '.')));
                        });
                        if (t.getTransformer("folder-classes").isEnabled() && FolderClasses.folderResources.isEnabled()) {
                            IOUtil.writeEntry(stream, name + "/", modifiedBytes.get().getBytes()); // "resource.yml/"
                        }
                        IOUtil.writeEntry(stream, name, modifiedBytes.get().getBytes()); // "resource.yml"
                    }
                    else IOUtil.writeEntry(stream, name, bytes);
                } else {
                    if (t.getTransformer("folder-classes").isEnabled() && FolderClasses.folderResources.isEnabled()) {
                        name += "/";
                    }
                    IOUtil.writeEntry(stream, name, bytes); // "resource.yml"
                }

                if (t.getTransformer("duplicate-resources").isEnabled() && DuplicateResources.dupResources.isEnabled()) {
                    int dupAmount = DuplicateResources.dupAmount.getValue();
                    for (int x = 1; x <= dupAmount; x++) {
                        String modifiedName = name + "\u0000".repeat(x);
                        byte[] duplicatedData = IOUtil.duplicateData(bytes);
                        IOUtil.writeEntry(stream, modifiedName, duplicatedData); // "resource.yml   "
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Add classes
        Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
        classes.stream()
                .filter(classWrapper -> !classWrapper.isLibraryClass())
                .forEach(classWrapper -> {
                    classWrapper.getNode().methods.forEach(methodNode -> {
                        try {
                            analyzer.analyzeAndComputeMaxs(methodNode.name, methodNode);
                        } catch (AnalyzerException e) {
                            e.addSuppressed(new Throwable("Found exception ClassName: " + classWrapper.getNode().name + " MethodName:" + methodNode.name));
                        }
                    });

                    try {
                        String name = classWrapper.getName(); // "Class.class"
                        boolean remapperEnabled = Ambien.get.transformerManager.getTransformer("remapper").isEnabled();
                        boolean folderClassesEnabled = Ambien.get.transformerManager.getTransformer("folder-classes").isEnabled();
                        boolean duplicateResourcesEnabled = Ambien.get.transformerManager.getTransformer("duplicate-resources").isEnabled();
                        boolean dupClassesEnabled = DuplicateResources.dupClasses.isEnabled();

                        if (remapperEnabled && Remapper.classes.isEnabled() && Remapper.map.containsKey(name.replace(".class", ""))) {

                            if (folderClassesEnabled && FolderClasses.folderClasses.isEnabled()) {
                                name += "/"; // "Class.class/"
                            }
                            if (duplicateResourcesEnabled && dupClassesEnabled) {
                                for (int x = 1; x <= DuplicateResources.dupAmount.getValue(); x++) {
                                    // "Asdasd.class   "
                                    IOUtil.writeEntry(
                                            stream,
                                            Remapper.map.get(
                                                    name.replace(".class/", "")
                                                    .replace(".class", ""))
                                                + ".class"
                                                + "\u0000".repeat(x),
                                            IOUtil.duplicateData(classWrapper.toByteArray())
                                    );
                                }
                            }
                            IOUtil.writeEntry(stream, Remapper.map.get(name.replace(".class", "")) + ".class", classWrapper.toByteArray());
                        } else {
                            if (folderClassesEnabled && FolderClasses.folderClasses.isEnabled()) {
                                name += "/";
                            }
                            if (duplicateResourcesEnabled && dupClassesEnabled) {
                                for (int x = 1; x <= DuplicateResources.dupAmount.getValue(); x++) {
                                    IOUtil.writeEntry(stream, name + "\u0000".repeat(x), IOUtil.duplicateData(classWrapper.toByteArray()));
                                }
                            }
                            IOUtil.writeEntry(stream, name, classWrapper.toByteArray());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });


        if (Ambien.get.transformerManager.getTransformer("crasher").isEnabled() && Crasher.shitClasses.isEnabled()) {
            for (int i = 0; i < Crasher.shitAmount.getValue(); i++) {
                Crasher.addShitClasses(null, stream);
                Crasher.addShitClasses("META-INF/", stream);
            }
        }

        // Set zip comment
        if (
            Ambien.get.transformerManager.getTransformer("metadata").isEnabled() &&
            !Metadata.commentText.getValue().equals("")
        )
            stream.setComment(Metadata.commentText.getValue());

        // Close stream
        stream.close();
    }
}

package me.iris.ambien.obfuscator.transformers.implementations.miscellaneous;

import me.iris.ambien.obfuscator.Ambien;
import me.iris.ambien.obfuscator.settings.data.implementations.BooleanSetting;
import me.iris.ambien.obfuscator.settings.data.implementations.ListSetting;
import me.iris.ambien.obfuscator.settings.data.implementations.StringSetting;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Ordinal;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.utilities.StringUtil;
import me.iris.ambien.obfuscator.wrappers.ClassWrapper;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;
import me.iris.ambien.obfuscator.wrappers.MethodWrapper;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static me.iris.ambien.obfuscator.utilities.StringUtil.getNewName;

@TransformerInfo(
        name = "remapper",
        category = Category.PACKAGING,
        stability = Stability.EXPERIMENTAL,
        ordinal = Ordinal.HIGH,
        description = "Renames your shit to random shit :)"
)
public class Remapper extends Transformer {
    public static final StringSetting forcePackage = new StringSetting("force-package", "");
    public static final BooleanSetting classes = new BooleanSetting("classes", true);
    public final ListSetting excl = new ListSetting("name-exclude", new ArrayList<>(List.of("onEnable", "onDisable", "onInitialize", "onInitializeClient")));
    public final BooleanSetting methods = new BooleanSetting("methods", true);
    public final BooleanSetting fields = new BooleanSetting("fields", true);
    public final BooleanSetting localVariables = new BooleanSetting("local-variables", true);
    /**
     * Options/modes:
     * random ~ Makes names random characters at a random length
     * barcode ~ Example: IllllIIIIlIlIIll
     */
    public final StringSetting dictionary = new StringSetting("dictionary", "random");
    public static final BooleanSetting fabricMixins = new BooleanSetting("mixin-remap", false);
    public static final StringSetting mixinsPackage = new StringSetting("mixin-package", "com/example/mixins/");
    public static final StringSetting targetMixinsPackage = new StringSetting("target-mixin-package", "rapapa/parara/mixins/");

    public static final Map<String, String> cmap = new HashMap<>();
    public static final Map<String, String> map = new HashMap<>();
    public static final Map<String, ClassWrapper> wrappers = new HashMap<>();

    public static final List<String> excludedAnnotations = List.of("Lorg/spongepowered/asm/mixin/Shadow;", "Lorg/spongepowered/asm/mixin/gen/Accessor;");
    public static final List<String> excludedFields = List.of("this", "$assertionsDisabled");

    @Override
    public void transform(JarWrapper wrapper) {
        remap(wrapper);
    }

    private void remap(JarWrapper wrapper) {
        if (methods.isEnabled()) remapMethods(wrapper);
        if (fields.isEnabled()) remapFields(wrapper);
        if (localVariables.isEnabled()) remapLocalVariables(wrapper);
        if (classes.isEnabled()) remapClasses(wrapper);

        // Apply map
        final SimpleRemapper remapper = new SimpleRemapper(map);
        for (ClassWrapper cw : wrappers.values()) {
            // Remap
            final ClassNode remappedClass = new ClassNode();
            final ClassRemapper classRemapper = new ClassRemapper(remappedClass, remapper);
            cw.getNode().accept(classRemapper);
            cw.setNode(remappedClass);
        }
    }

    private void remapClasses(JarWrapper wrapper) {
        getClasses(wrapper).forEach(classWrapper -> {
            final ClassNode node = classWrapper.getNode();

            if (!StringUtil.containsNonAlphabeticalChars(node.name)) return; // idk, it's just always returns true. wtf, iris?
            if (classWrapper.isLibraryClass() || Ambien.get.exclusionManager.isClassExcluded(node.name, classWrapper.getName())) return;

            AtomicReference<String> newName = new AtomicReference<>(getNewName(dictionary.getValue(), forcePackage.getValue()));
            if (node.invisibleAnnotations != null && !node.invisibleAnnotations.isEmpty()) {
                final List<AnnotationNode> copy = new ArrayList<>(node.invisibleAnnotations);
                copy.forEach(annotationNode -> {
                    if (annotationNode.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                        newName.set(getNewName(dictionary.getValue(), targetMixinsPackage.getValue()));
                });
            }

            Ambien.LOGGER.debug(node.name+" | "+newName.get());
            cmap.put(node.name, newName.get());
            map.put(node.name, newName.get());
            wrappers.put(node.name, classWrapper);
        });
    }

    private void remapMethods(JarWrapper wrapper) {
        getClasses(wrapper).forEach(classWrapper -> classWrapper.getTransformableMethods().stream()
                .filter(methodWrapper -> !isExcluded(methodWrapper))
                .forEach(methodWrapper -> {
                    String str = getNewName(dictionary.getValue());
                    map.put(methodWrapper.getNode().name, str);
                    methodWrapper.getNode().name = str;
                }));
    }

    private boolean isExcluded(MethodWrapper mw) {
        List<AnnotationNode> annotations = mw.getNode().visibleAnnotations;

        if (excl.getOptions().contains(mw.getNode().name)) return true;
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (excludedAnnotations.contains(annotation.desc)) {
                    return true;
                }
            }
        }
        if (mw.isInitializer()) return true;
        if (Modifier.isAbstract(mw.getNode().access)) return true;
        return false;
    }

    private void remapFields(JarWrapper wrapper) {
        getClasses(wrapper).stream()
                .filter(classWrapper -> !(classWrapper.isEnum() || classWrapper.isLibraryClass() || Ambien.get.exclusionManager.isClassExcluded(classWrapper.getNode().name, classWrapper.getName())))
                .forEach(classWrapper -> classWrapper.getFields().stream()
                        .filter(fieldNode -> !isExcluded(fieldNode))
                        .forEach(fieldNode -> {
                            String str = getNewName(dictionary.getValue());
                            map.put(fieldNode.name, str);
                            fieldNode.name = str;
                        }));
    }

    private boolean isExcluded(FieldNode fn) {
        List<AnnotationNode> annotations = fn.visibleAnnotations;

        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (excludedAnnotations.contains(annotation.desc)) {
                    return true;
                }
            }
        }

        if (excludedFields.contains(fn.name)) return true;
        if (Modifier.isPrivate(fn.access)) return true;
        if (Modifier.isProtected(fn.access)) return true;

        return false;
    }

    private void remapLocalVariables(JarWrapper wrapper) {
        getClasses(wrapper).forEach(classWrapper -> classWrapper.getTransformableMethods().forEach(methodWrapper -> {
            if (!methodWrapper.hasLocalVariables()) return;
            for (Object localVarObj : methodWrapper.getNode().localVariables) {
                //noinspection CastCanBeRemovedNarrowingVariableType
                final LocalVariableNode localVarNode = (LocalVariableNode)localVarObj;
                if (localVarNode.name.equals("this")) continue;
                localVarNode.name = getNewName(dictionary.getValue());
            }
        }));
    }
}

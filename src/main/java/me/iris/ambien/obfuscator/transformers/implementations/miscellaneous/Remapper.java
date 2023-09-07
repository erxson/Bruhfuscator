package me.iris.ambien.obfuscator.transformers.implementations.miscellaneous;

import me.iris.ambien.obfuscator.Ambien;
import me.iris.ambien.obfuscator.settings.data.implementations.BooleanSetting;
import me.iris.ambien.obfuscator.settings.data.implementations.StringSetting;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Ordinal;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.utilities.StringUtil;
import me.iris.ambien.obfuscator.wrappers.ClassWrapper;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;

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
    public static final BooleanSetting classes = new BooleanSetting("classes", true);
    public final BooleanSetting localVariables = new BooleanSetting("local-variables", true);
    /**
     * Options/modes:
     * random ~ Makes names random characters at a random length
     * barcode ~ Example: IllllIIIIlIlIIll
     */
    public final StringSetting dictionary = new StringSetting("dictionary", "random");
    public static final StringSetting prefix = new StringSetting("prefix", "");
    public static final BooleanSetting fabricMixins = new BooleanSetting("mixin-remap", false);
    public static final StringSetting mixinsPackage = new StringSetting("mixin-package", "com/example/mixins/");
    public static final StringSetting targetMixinsPackage = new StringSetting("target-mixin-package", "rapapa/parara/mixins/");

    public static final Map<String, String> map = new HashMap<>();
    public static final Map<String, ClassWrapper> wrappers = new HashMap<>();

    @Override
    public void transform(JarWrapper wrapper) {
        if (classes.isEnabled()) remapClasses(wrapper);
        if (localVariables.isEnabled()) remapLocalVariables(wrapper);
    }

    private void remapClasses(JarWrapper jarWrapper) {

        // Generate map
        getClasses(jarWrapper).forEach(classWrapper -> {
            final ClassNode node = classWrapper.getNode();

            if (!StringUtil.containsNonAlphabeticalChars(node.name)) return; // idk, it's just always returns true. wtf, iris?
            if (classWrapper.isLibraryClass() || Ambien.get.exclusionManager.isClassExcluded(node.name, classWrapper.getName())) return;

            AtomicReference<String> newName = new AtomicReference<>(getNewName(dictionary.getValue(), prefix.getValue()));
            if (node.invisibleAnnotations != null && !node.invisibleAnnotations.isEmpty()) {
                final List<AnnotationNode> copy = new ArrayList<>(node.invisibleAnnotations);
                copy.forEach(annotationNode -> {
                    if (annotationNode.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                        newName.set(getNewName(dictionary.getValue(), targetMixinsPackage.getValue()));
                });
            }

            Ambien.LOGGER.debug(node.name+" | "+newName.get());
            map.put(node.name, newName.get());
            wrappers.put(node.name, classWrapper);
        });

        // Apply map
        final SimpleRemapper remapper = new SimpleRemapper(map);
        for (ClassWrapper wrapper : wrappers.values()) {
            // Remap
            final ClassNode remappedNode = new ClassNode();
            final ClassRemapper classRemapper = new ClassRemapper(remappedNode, remapper);
            wrapper.getNode().accept(classRemapper);
            wrapper.setNode(remappedNode);
        }
    }

    private void remapLocalVariables(JarWrapper wrapper) {
        getClasses(wrapper).forEach(classWrapper -> {
            classWrapper.getTransformableMethods().forEach(methodWrapper -> {
                if (!methodWrapper.hasLocalVariables()) return;
                for (Object localVarObj : methodWrapper.getNode().localVariables) {
                    //noinspection CastCanBeRemovedNarrowingVariableType
                    final LocalVariableNode localVarNode = (LocalVariableNode)localVarObj;
                    if (localVarNode.name.equals("this")) continue;
                    localVarNode.name = getNewName(dictionary.getValue(), prefix.getValue());
                }
            });
        });
    }
}

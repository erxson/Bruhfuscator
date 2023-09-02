package me.iris.ambien.obfuscator.transformers.implementations.flow;

import me.iris.ambien.obfuscator.settings.data.implementations.BooleanSetting;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Ordinal;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.transformers.implementations.flow.universal.GimJeongeunFlow;
import me.iris.ambien.obfuscator.transformers.implementations.flow.universal.GotoFlow;
import me.iris.ambien.obfuscator.wrappers.ClassWrapper;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;
import me.iris.ambien.obfuscator.wrappers.MethodWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@TransformerInfo(
        name = "flow",
        category = Category.CONTROL_FLOW,
        stability = Stability.STABLE,
        ordinal = Ordinal.HIGH,
        description = "Flow."
)
public class Flow extends Transformer {
    public static final Map<ClassWrapper, List<MethodWrapper>> classMethodsMap = new ConcurrentHashMap<>();
    public final BooleanSetting gotoFlow = new BooleanSetting("goto-flow", false);
    public final BooleanSetting gimJeongeunFlow = new BooleanSetting("myj2c-flow", false);

    @Override
    public void transform(JarWrapper wrapper) {
        getClasses(wrapper)
                .forEach(classWrapper -> {
                    List<MethodWrapper> methods = new ArrayList<>(classWrapper.getTransformableMethods());
                    classMethodsMap.put(classWrapper, methods);
                });
        if (gimJeongeunFlow.isEnabled()) GimJeongeunFlow.gimJeongeunFlow();
        if (gotoFlow.isEnabled()) GotoFlow.gotoFlow();
    }
}

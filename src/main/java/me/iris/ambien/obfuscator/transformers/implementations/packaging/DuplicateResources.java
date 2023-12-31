package me.iris.ambien.obfuscator.transformers.implementations.packaging;

import me.iris.ambien.obfuscator.settings.data.implementations.BooleanSetting;
import me.iris.ambien.obfuscator.settings.data.implementations.NumberSetting;
import me.iris.ambien.obfuscator.settings.data.implementations.StringSetting;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;

@TransformerInfo(
        name = "duplicate-resources",
        category = Category.PACKAGING,
        stability = Stability.STABLE,
        description = "Makes duplicates of files in jar."
)
public class DuplicateResources extends Transformer {
    public static final BooleanSetting dupClasses = new BooleanSetting("classes", false);
    public static final BooleanSetting dupResources = new BooleanSetting("resources", false);
    public static final StringSetting dupText = new StringSetting("text", "");
    public static final BooleanSetting dupSpoofSize = new BooleanSetting("spoof-size", false);
    public static final NumberSetting<Integer> dupAmount = new NumberSetting<>("amount", 10);

    @Override
    public void transform(JarWrapper wrapper) {
    }
}

package me.iris.ambien.obfuscator.transformers.implementations.data;

import me.iris.ambien.obfuscator.settings.data.implementations.BooleanSetting;
import me.iris.ambien.obfuscator.settings.data.implementations.ListSetting;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Ordinal;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;

import java.util.ArrayList;

import static me.iris.ambien.obfuscator.transformers.implementations.data.string.AmbienStringEncryption.ambienEncryption;
import static me.iris.ambien.obfuscator.transformers.implementations.data.string.ColonialStringEncryption.colonialEncryption;
import static me.iris.ambien.obfuscator.transformers.implementations.data.string.GotoStringEncryption.gotoEncryption;
import static me.iris.ambien.obfuscator.transformers.implementations.data.string.SouvenirStringEncryption.souvenirEncryption;

@TransformerInfo(
        name = "string-encryption",
        category = Category.DATA,
        stability = Stability.STABLE,
        ordinal = Ordinal.HIGH,
        description = "Encrypts string using xor & random keys."
)
public class StringEncryption extends Transformer {
    // TODO: Randomize descriptor argument order & add decoy args

    /**
     * List of string that won't be encrypted
     */
    public final BooleanSetting ambienEncryption = new BooleanSetting("ambien", true);
    public final BooleanSetting colonialEncryption = new BooleanSetting("colonial", false);
    public final BooleanSetting gotoEncryption = new BooleanSetting("goto", false);
    public final BooleanSetting souvenirEncryption = new BooleanSetting("souvenir", false);
    public static final ListSetting stringBlacklist = new ListSetting("string-blacklist", new ArrayList<>());

    @Override
    public void transform(JarWrapper wrapper) {
        getClasses(wrapper).forEach(classWrapper -> {
            if (colonialEncryption.isEnabled()) colonialEncryption(classWrapper);
            if (souvenirEncryption.isEnabled()) souvenirEncryption(classWrapper);
            if (ambienEncryption.isEnabled()) ambienEncryption(classWrapper);
            if (gotoEncryption.isEnabled()) gotoEncryption(classWrapper.getNode());
        });
    }
}

package me.iris.ambien.obfuscator.transformers.implementations.data.string;

import me.iris.ambien.obfuscator.transformers.implementations.data.StringEncryption;
import me.iris.ambien.obfuscator.utilities.StringUtil;
import me.iris.ambien.obfuscator.utilities.kek.souvenir.CaesarEncryption;
import me.iris.ambien.obfuscator.utilities.kek.souvenir.IStringEncryptionMethod;
import me.iris.ambien.obfuscator.utilities.kek.souvenir.XorEncryption;
import me.iris.ambien.obfuscator.wrappers.ClassWrapper;
import me.iris.ambien.obfuscator.wrappers.MethodWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.BIPUSH;

public class SouvenirStringEncryption {
    private static final Map<ClassWrapper, List<MethodWrapper>> classMethodsMap = new ConcurrentHashMap<>();
    public static List<IStringEncryptionMethod> methods = Arrays.asList(new CaesarEncryption(), new XorEncryption());
    static SecureRandom r = new SecureRandom();

    public static void souvenirEncryption(ClassWrapper classWrapper) {
        List<MethodWrapper> methods = classWrapper.getTransformableMethods().stream()
                .filter(MethodWrapper::hasInstructions)
                .collect(Collectors.toList());
        classMethodsMap.put(classWrapper, methods);

        classMethodsMap.forEach((cw, m) ->
                        run(cw.getNode())
                );
    }

    public static void run(ClassNode cn) {
        if((cn.access & ACC_INTERFACE) == ACC_INTERFACE) {
            return;
        }
        AtomicBoolean has = new AtomicBoolean(false);
        IStringEncryptionMethod encryption = methods.get(r.nextInt(methods.size()));
        MethodNode decryptMethod = encryption.createDecrypt(StringUtil.genName(0));
        cn.methods.forEach(mn -> {
            Arrays.stream(mn.instructions.toArray()).forEach(insn -> {
                if(insn.getOpcode() == Opcodes.LDC && ((LdcInsnNode)insn).cst instanceof String && !StringEncryption.stringBlacklist.getOptions().contains((String)((LdcInsnNode)insn).cst)){
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if(ldc.cst instanceof String){
                        int decryptValue = r.nextInt(30) + 6;
                        has.set(true);
                        ldc.cst = encryption.encrypt((String)ldc.cst, decryptValue);
                        mn.instructions.insert(ldc, new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, decryptMethod.name, "(Ljava/lang/String;I)Ljava/lang/String;", false));
                        mn.instructions.insert(ldc, new IntInsnNode(BIPUSH, decryptValue));

                    }
                }
            });
        });

        if(has.get()){
            cn.methods.add(decryptMethod);
        }
    }
}

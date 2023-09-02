package me.iris.ambien.obfuscator.utilities.kek.souvenir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public interface IStringEncryptionMethod extends Opcodes {

    String encrypt(String v, int key);

    MethodNode createDecrypt(String name);

}

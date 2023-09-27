package me.iris.ambien.obfuscator.transformers.implementations.data.string;

import me.iris.ambien.obfuscator.utilities.StringUtil;
import me.iris.ambien.obfuscator.utilities.kek.colonial.BytecodeHelper;
import me.iris.ambien.obfuscator.utilities.kek.colonial.NodeUtils;
import me.iris.ambien.obfuscator.wrappers.ClassWrapper;
import me.iris.ambien.obfuscator.wrappers.MethodWrapper;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ColonialStringEncryption {

    public static void colonialEncryption(ClassWrapper classWrapper) {
        Map<ClassWrapper, List<MethodWrapper>> classMethodsMap = new ConcurrentHashMap<>();
        List<MethodWrapper> methods = classWrapper.getTransformableMethods();
        classMethodsMap.put(classWrapper, methods);

        classMethodsMap.forEach((cw, m) -> colonialString(classWrapper.getNode()));
    }

    private static String XOR(final int i, final int j, final String string, final int k, final int l, final char[] s) {
        final StringBuilder sb = new StringBuilder();
        int i2 = 0;
        for (final char c : string.toCharArray()) {
            sb.append((char)(c ^ s[i2 % s.length] ^ (i ^ k + i2) ^ j ^ l));
            ++i2;
        }
        return sb.toString();
    }

    private static String EncryptKey(final String s, final char[] b) {
        final char[] charArray = s.toCharArray();
        for (int i = charArray.length, n = 0; i > n; ++n) {
            final char c = charArray[n];
            char c2;
            switch (n % 7) {
                case 0 -> c2 = b[0];
                case 1 -> c2 = b[1];
                case 2 -> c2 = b[2];
                case 3 -> c2 = b[3];
                case 4 -> c2 = b[4];
                case 5 -> c2 = b[5];
                default -> c2 = b[6];
            }
            charArray[n] = (char)(c ^ c2);
        }
        return new String(charArray).intern();
    }


    public static void colonialString(ClassNode node) {
        if ((node.access & 0x200) != 0x0) {
            return;
        }
        try {
            if (node.methods != null && node.methods.size() > 0) {
                final String key = StringUtil.genName(100);
                final Random ran = new Random();
                final char[] key2 = { (char)ran.nextInt(126), (char)ran.nextInt(126), (char)ran.nextInt(126), (char)ran.nextInt(126), (char)ran.nextInt(126), (char)ran.nextInt(126), (char)ran.nextInt(126) };
                final String NAME3 = StringUtil.genName(40);
                final FieldVisitor fieldVisitor = node.visitField(8, NAME3, "[C", null, null);
                fieldVisitor.visitEnd();
                final String name = StringUtil.genName(40);
                for (final MethodNode mn : node.methods) {
                    BytecodeHelper.forEach(mn.instructions, LdcInsnNode.class, ldc -> {
                        if (ldc.cst instanceof String s) {
                            final int k1 = new Random().nextInt();
                            final int k2 = new Random().nextInt();
                            final int k3 = new Random().nextInt();
                            final int k4 = new Random().nextInt();
                            final InsnList il = new InsnList();
                            il.add(new LdcInsnNode(k1));
                            il.add(new LdcInsnNode(k2));
                            il.add(new LdcInsnNode(XOR(k1, k2, s, k3, k4, key.toCharArray())));
                            il.add(new LdcInsnNode(k3));
                            il.add(new LdcInsnNode(k4));
                            il.add(new MethodInsnNode(184, node.name, name, "(IILjava/lang/String;II)Ljava/lang/String;", false));
                            mn.instructions.insert(ldc, il);
                            mn.instructions.remove(ldc);
                        }
                    });
                }
                final MethodVisitor methodVisitor = node.visitMethod(10, name, "(IILjava/lang/String;II)Ljava/lang/String;", null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                methodVisitor.visitLabel(label0);
                methodVisitor.visitTypeInsn(187, "java/lang/StringBuilder");
                methodVisitor.visitInsn(89);
                methodVisitor.visitMethodInsn(183, "java/lang/StringBuilder", "<init>", "()V", false);
                methodVisitor.visitVarInsn(58, 5);
                Label label2 = new Label();
                methodVisitor.visitLabel(label2);
                methodVisitor.visitInsn(3);
                methodVisitor.visitVarInsn(54, 6);
                Label label3 = new Label();
                methodVisitor.visitLabel(label3);
                methodVisitor.visitVarInsn(25, 2);
                methodVisitor.visitMethodInsn(182, "java/lang/String", "toCharArray", "()[C", false);
                methodVisitor.visitInsn(89);
                methodVisitor.visitVarInsn(58, 10);
                methodVisitor.visitInsn(190);
                methodVisitor.visitVarInsn(54, 9);
                methodVisitor.visitInsn(3);
                methodVisitor.visitVarInsn(54, 8);
                Label label4 = new Label();
                methodVisitor.visitJumpInsn(167, label4);
                Label label5 = new Label();
                methodVisitor.visitLabel(label5);
                methodVisitor.visitFrame(0, 11, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/String", Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/StringBuilder", Opcodes.INTEGER, Opcodes.TOP, Opcodes.INTEGER, Opcodes.INTEGER, "[C" }, 0, new Object[0]);
                methodVisitor.visitVarInsn(25, 10);
                methodVisitor.visitVarInsn(21, 8);
                methodVisitor.visitInsn(52);
                methodVisitor.visitVarInsn(54, 7);
                Label label6 = new Label();
                methodVisitor.visitLabel(label6);
                methodVisitor.visitVarInsn(25, 5);
                methodVisitor.visitVarInsn(21, 7);
                methodVisitor.visitFieldInsn(178, node.name, NAME3, "[C");
                methodVisitor.visitVarInsn(21, 6);
                methodVisitor.visitFieldInsn(178, node.name, NAME3, "[C");
                methodVisitor.visitInsn(190);
                methodVisitor.visitInsn(112);
                methodVisitor.visitInsn(52);
                methodVisitor.visitInsn(130);
                methodVisitor.visitVarInsn(21, 0);
                methodVisitor.visitVarInsn(21, 3);
                methodVisitor.visitVarInsn(21, 6);
                methodVisitor.visitInsn(96);
                methodVisitor.visitInsn(130);
                methodVisitor.visitInsn(130);
                methodVisitor.visitVarInsn(21, 1);
                methodVisitor.visitInsn(130);
                methodVisitor.visitVarInsn(21, 4);
                methodVisitor.visitInsn(130);
                methodVisitor.visitInsn(146);
                methodVisitor.visitMethodInsn(182, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false);
                methodVisitor.visitInsn(87);
                Label label7 = new Label();
                methodVisitor.visitLabel(label7);
                methodVisitor.visitIincInsn(6, 1);
                Label label8 = new Label();
                methodVisitor.visitLabel(label8);
                methodVisitor.visitIincInsn(8, 1);
                methodVisitor.visitLabel(label4);
                methodVisitor.visitFrame(3, 0, null, 0, null);
                methodVisitor.visitVarInsn(21, 8);
                methodVisitor.visitVarInsn(21, 9);
                methodVisitor.visitJumpInsn(161, label5);
                Label label9 = new Label();
                methodVisitor.visitLabel(label9);
                methodVisitor.visitVarInsn(25, 5);
                methodVisitor.visitMethodInsn(182, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                methodVisitor.visitInsn(176);
                Label label10 = new Label();
                methodVisitor.visitLabel(label10);
                methodVisitor.visitMaxs(5, 11);
                methodVisitor.visitEnd();
                final MethodNode methodVisitor2 = new MethodNode();
                methodVisitor2.visitCode();
                methodVisitor2.visitLdcInsn(EncryptKey(key, key2));
                methodVisitor2.visitInsn(2);
                label0 = new Label();
                methodVisitor2.visitJumpInsn(167, label0);
                label2 = new Label();
                methodVisitor2.visitLabel(label2);
                methodVisitor2.visitFrame(4, 0, null, 1, new Object[] { "java/lang/String" });
                methodVisitor2.visitMethodInsn(182, "java/lang/String", "toCharArray", "()[C", false);
                methodVisitor2.visitFieldInsn(179, node.name, NAME3, "[C");
                label3 = new Label();
                methodVisitor2.visitJumpInsn(167, label3);
                methodVisitor2.visitLabel(label0);
                methodVisitor2.visitFrame(0, 0, new Object[0], 2, new Object[] { "java/lang/String", Opcodes.INTEGER });
                methodVisitor2.visitInsn(95);
                methodVisitor2.visitMethodInsn(182, "java/lang/String", "toCharArray", "()[C", false);
                methodVisitor2.visitInsn(89);
                methodVisitor2.visitInsn(190);
                methodVisitor2.visitInsn(95);
                methodVisitor2.visitInsn(3);
                methodVisitor2.visitVarInsn(54, 0);
                label4 = new Label();
                methodVisitor2.visitJumpInsn(167, label4);
                label5 = new Label();
                methodVisitor2.visitLabel(label5);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 3, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C" });
                methodVisitor2.visitInsn(89);
                methodVisitor2.visitVarInsn(21, 0);
                label6 = new Label();
                methodVisitor2.visitLabel(label6);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 5, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER });
                methodVisitor2.visitInsn(92);
                methodVisitor2.visitInsn(52);
                methodVisitor2.visitVarInsn(21, 0);
                methodVisitor2.visitIntInsn(16, 7);
                methodVisitor2.visitInsn(112);
                label7 = new Label();
                label8 = new Label();
                label9 = new Label();
                label10 = new Label();
                final Label label11 = new Label();
                final Label label12 = new Label();
                final Label label13 = new Label();
                methodVisitor2.visitTableSwitchInsn(0, 5, label13, label7, label8, label9, label10, label11, label12);
                methodVisitor2.visitLabel(label7);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[0]);
                final Label label14 = new Label();
                methodVisitor2.visitJumpInsn(167, label14);
                methodVisitor2.visitLabel(label8);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[1]);
                methodVisitor2.visitJumpInsn(167, label14);
                methodVisitor2.visitLabel(label9);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[2]);
                methodVisitor2.visitJumpInsn(167, label14);
                methodVisitor2.visitLabel(label10);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[3]);
                methodVisitor2.visitJumpInsn(167, label14);
                methodVisitor2.visitLabel(label11);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[4]);
                methodVisitor2.visitJumpInsn(167, label14);
                methodVisitor2.visitLabel(label12);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[5]);
                methodVisitor2.visitJumpInsn(167, label14);
                methodVisitor2.visitLabel(label13);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 6, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitIntInsn(16, key2[6]);
                methodVisitor2.visitLabel(label14);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 7, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C", "[C", Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER });
                methodVisitor2.visitInsn(130);
                methodVisitor2.visitInsn(146);
                methodVisitor2.visitInsn(85);
                methodVisitor2.visitIincInsn(0, 1);
                methodVisitor2.visitLabel(label4);
                methodVisitor2.visitFrame(0, 1, new Object[] { Opcodes.INTEGER }, 3, new Object[] { Opcodes.INTEGER, Opcodes.INTEGER, "[C" });
                methodVisitor2.visitInsn(95);
                methodVisitor2.visitInsn(90);
                methodVisitor2.visitVarInsn(21, 0);
                methodVisitor2.visitJumpInsn(163, label5);
                methodVisitor2.visitTypeInsn(187, "java/lang/String");
                methodVisitor2.visitInsn(90);
                methodVisitor2.visitInsn(95);
                methodVisitor2.visitMethodInsn(183, "java/lang/String", "<init>", "([C)V", false);
                methodVisitor2.visitMethodInsn(182, "java/lang/String", "intern", "()Ljava/lang/String;", false);
                methodVisitor2.visitInsn(95);
                methodVisitor2.visitInsn(87);
                methodVisitor2.visitInsn(95);
                methodVisitor2.visitInsn(87);
                methodVisitor2.visitJumpInsn(167, label2);
                methodVisitor2.visitLabel(label3);
                methodVisitor2.visitFrame(2, 1, null, 0, null);
                MethodNode clInit = NodeUtils.getMethod(node, "<clinit>");
                if (clInit == null) {
                    clInit = new MethodNode(8, "<clinit>", "()V", null, new String[0]);
                    node.methods.add(clInit);
                }
                if (clInit.instructions == null) {
                    clInit.instructions = new InsnList();
                }
                if (clInit.instructions.getFirst() == null) {
                    clInit.instructions.add(methodVisitor2.instructions);
                    clInit.instructions.add(new InsnNode(177));
                }
                else {
                    clInit.instructions.insertBefore(clInit.instructions.getFirst(), methodVisitor2.instructions);
                }
            }
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
}

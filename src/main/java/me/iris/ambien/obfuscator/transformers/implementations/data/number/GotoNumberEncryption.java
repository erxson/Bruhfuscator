package me.iris.ambien.obfuscator.transformers.implementations.data.number;

import me.iris.ambien.obfuscator.builders.InstructionModifier;
import me.iris.ambien.obfuscator.utilities.ASMUtils;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.concurrent.ThreadLocalRandom;

public class GotoNumberEncryption {
    public static void gotoEncryption(JarWrapper wrapper) {
        wrapper.getClasses().forEach(classWrapper ->
                classWrapper.getTransformableMethods().forEach(methodWrapper ->
                        obf(methodWrapper.getNode())
                ));
    }
    public static void obf(MethodNode method) {
        InstructionModifier modifier = new InstructionModifier();

        for (AbstractInsnNode instruction : method.instructions) {
            if (ASMUtils.isNumber(instruction)) {
                Number number = ASMUtils.getNumber(instruction);
                if (number instanceof Double) {
                    long bits = Double.doubleToLongBits((Double) number);
                    InsnList list = processLong(bits);
                    list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "java/lang/Double",
                            "longBitsToDouble",
                            "(J)D",
                            false
                    ));
                    modifier.replace(instruction, list);
                } else if (number instanceof Float) {
                    int bits = Float.floatToIntBits((Float) number);
                    InsnList list = processInt(bits);
                    list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "java/lang/Float",
                            "intBitsToFloat",
                            "(I)F",
                            false
                    ));
                    modifier.replace(instruction, list);
                } else if (number instanceof Long) {
                    modifier.replace(instruction, processLong((Long) number));
                } else if (number instanceof Integer || number instanceof Short || number instanceof Byte) {
                    modifier.replace(instruction, processInt(number.intValue()));
                }
            }
        }

        modifier.apply(method);
    }

    private static InsnList processInt(int i) {
        int random = ThreadLocalRandom.current().nextInt();
        int xor = i ^ random;
        InsnList list = new InsnList();
        boolean leftToLong = ThreadLocalRandom.current().nextBoolean();

        list.add(ASMUtils.createNumberNode(random));

        if (leftToLong) {
            list.add(new InsnNode(Opcodes.I2L));
        }

        if (ThreadLocalRandom.current().nextBoolean()) { // ~
            list.add(ASMUtils.createNumberNode(~xor));
            list.add(new InsnNode(Opcodes.ICONST_M1));
            list.add(new InsnNode(Opcodes.IXOR));
        } else {
            list.add(ASMUtils.createNumberNode(xor));
        }

        if (leftToLong) {
            list.add(new InsnNode(Opcodes.I2L));
            list.add(new InsnNode(Opcodes.LXOR));
            list.add(new InsnNode(Opcodes.L2I));
        } else {
            list.add(new InsnNode(Opcodes.IXOR));
        }

        return list;
    }

    private static InsnList processLong(long l) {
        InsnList list = new InsnList();
        long random = ThreadLocalRandom.current().nextLong();
        long xor = l ^ random;

        switch (ThreadLocalRandom.current().nextInt(0, 3)) {
            case 0 -> {
                list.add(new LdcInsnNode(random));
                list.add(new InsnNode(Opcodes.L2I));
                list.add(new InsnNode(Opcodes.I2L));
            }
            case 1 -> {
                list.add(new LdcInsnNode((int) random));
                list.add(new InsnNode(Opcodes.I2L));
            }
            case 2 -> list.add(new LdcInsnNode(random));
        }

        if (ThreadLocalRandom.current().nextBoolean()) { // ~
            list.add(new LdcInsnNode(~xor));
            list.add(new LdcInsnNode(-1L));
            list.add(new InsnNode(Opcodes.LXOR));
        } else {
            list.add(new LdcInsnNode(xor));
        }

        list.add(new InsnNode(Opcodes.LXOR));

        return list;
    }
}

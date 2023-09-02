package me.iris.ambien.obfuscator.transformers.implementations.data;

import me.iris.ambien.obfuscator.Ambien;
import me.iris.ambien.obfuscator.asm.SizeEvaluator;
import me.iris.ambien.obfuscator.builders.InstructionModifier;
import me.iris.ambien.obfuscator.settings.data.implementations.BooleanSetting;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Ordinal;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.utilities.ASMUtils;
import me.iris.ambien.obfuscator.utilities.MathUtil;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;
import me.iris.ambien.obfuscator.wrappers.MethodWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@TransformerInfo(
        name = "number-obfuscation",
        category = Category.DATA,
        stability = Stability.STABLE,
        ordinal = Ordinal.HIGH,
        description = "Replaces numbers with random math operations to get the original value."
)
public class NumberObfuscation extends Transformer {
    public final BooleanSetting gotoEncryption = new BooleanSetting("goto", false);
    public final BooleanSetting ambienEncryption = new BooleanSetting("ambien", false);
    /**
     * Adds additional math operations on top of generic xoring
     * Probably shouldn't use this if you care about performance
     */
    public final BooleanSetting aggressive = new BooleanSetting("aggressive", false);

    /**
     * Obfuscates LDC instructions
     */
    public final BooleanSetting ldc = new BooleanSetting("obfuscate-ldc", false);

    /**
     * Calls the .reverse function for the respective integral type
     */
    public final BooleanSetting reverseIntegralNumbers = new BooleanSetting("reverse-integral-numbers", false);

    @Override
    public void transform(JarWrapper wrapper) {
        if (gotoEncryption.isEnabled()) getClasses(wrapper).forEach(classWrapper ->
                classWrapper.getTransformableMethods().forEach(methodWrapper ->
                                gotoEncryption(methodWrapper.getNode())
                        ));
        if (!ambienEncryption.isEnabled()) return;
        getClasses(wrapper).stream()
                .filter(classWrapper -> !classWrapper.isEnum() && !classWrapper.isInterface())
                .forEach(classWrapper -> classWrapper.getTransformableMethods().stream()
                        .filter(MethodWrapper::hasInstructions)
                        .forEach(methodWrapper -> methodWrapper.getInstructions()
                                .filter(insn -> insn.getOpcode() == BIPUSH || insn.getOpcode() == SIPUSH || insn.getOpcode() == LDC)
                                .forEach(insn -> {
                                    final InsnList list = new InsnList();

                                    // TODO: Add custom rand next seed for xor keys as an option

                                    // Obfuscate number
                                    if (insn.getOpcode() == LDC) {
                                        if (!ldc.isEnabled()) return;
                                        final LdcInsnNode ldc = (LdcInsnNode) insn;
                                        if (!(ldc.cst instanceof Number)) return;
                                        list.add(obfuscateLDC(ldc));
                                    } else
                                        list.add(obfuscatePUSH(insn.getOpcode(), ((IntInsnNode) insn).operand));

                                    // Replace instructions
                                    if (list.size() > 0) {
                                        if (SizeEvaluator.willOverflow(methodWrapper, list))
                                            Ambien.LOGGER.error("Can't obfuscate number without method overflowing. Class: {} | Method: {}", classWrapper.getName(), methodWrapper.getNode().name);
                                        else
                                            methodWrapper.replaceInstruction(insn, list);
                                    }
                                })));
    }

    private InsnList obfuscateLDC(final LdcInsnNode node) {
        final InsnList list = new InsnList();

        final Number number = (Number)node.cst;
        final Class<?> numCls = number.getClass();
        final int xorKey = MathUtil.randomInt();

        if (numCls == Integer.class) {
            int val = number.intValue();
            if (reverseIntegralNumbers.isEnabled())
                val = Integer.reverse(val);

            if (aggressive.isEnabled()) {
                final int orKey = MathUtil.randomInt();
                final int xor = val ^ (xorKey | orKey);

                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new LdcInsnNode(orKey));
                list.add(new InsnNode(IOR));
                list.add(new InsnNode(IXOR));

                // negate the value twice
                for (int i = 0; i < 2; i++)
                    list.add(new InsnNode(INEG));
            } else {
                final int xor = val ^ xorKey;
                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new InsnNode(IXOR));

                // negate the value twice
                for (int i = 0; i < 2; i++)
                    list.add(new InsnNode(INEG));
            }

            // Add reverse call
            if (reverseIntegralNumbers.isEnabled())
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "reverse", "(I)I", false));
        } else if (numCls == Long.class) {
            long val = number.longValue();
            if (reverseIntegralNumbers.isEnabled())
                val = Long.reverse(val);

            if (aggressive.isEnabled()) {
                final int orKey = MathUtil.randomInt();

                final long xor = val ^ (xorKey | orKey);

                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new InsnNode(I2L));
                list.add(new LdcInsnNode(orKey));
                list.add(new InsnNode(I2L));
                list.add(new InsnNode(LOR));
                list.add(new InsnNode(LXOR));
            } else {
                final long xor = val ^ (long)xorKey;
                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new InsnNode(I2L));
                list.add(new InsnNode(LXOR));

                // negate the value twice
                for (int i = 0; i < 2; i++)
                    list.add(new InsnNode(LNEG));
            }

            // Add reverse call
            if (reverseIntegralNumbers.isEnabled())
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "reverse", "(J)J", false));
        } else if (numCls == Float.class) {
            final float origVal = number.floatValue();
            final float onlyDec = origVal % 1.f;

            int noDec = (int)Float.parseFloat(String.format("%.0f", origVal));
            if (reverseIntegralNumbers.isEnabled())
                noDec = Integer.reverse(noDec);

            if (aggressive.isEnabled()) {
                final int orKey = MathUtil.randomInt();
                final int xor = noDec ^ (xorKey | orKey);

                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new LdcInsnNode(orKey));
                list.add(new InsnNode(IOR));
                list.add(new InsnNode(IXOR));

                // Add reverse call
                if (reverseIntegralNumbers.isEnabled())
                    list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "reverse", "(I)I", false));

                list.add(new InsnNode(I2F));
                list.add(new LdcInsnNode(onlyDec));
                list.add(new InsnNode(FADD));
            } else {
                final int xor = noDec ^ xorKey;

                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new InsnNode(IXOR));

                // Add reverse call
                if (reverseIntegralNumbers.isEnabled())
                    list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "reverse", "(I)I", false));

                list.add(new InsnNode(I2F));
                list.add(new LdcInsnNode(onlyDec));
                list.add(new InsnNode(FADD));
            }

            // negate the value twice
            for (int i = 0; i < 2; i++)
                list.add(new InsnNode(FNEG));
        } else if (numCls == Double.class) {
            final double origVal = number.doubleValue();
            final double onlyDec = origVal % 1.d;

            long noDec = (long)Double.parseDouble(String.format("%.0f", origVal));
            if (reverseIntegralNumbers.isEnabled())
                noDec = Long.reverse(noDec);

            if (aggressive.isEnabled()) {
                final int orKey = MathUtil.randomInt();
                final long xor = noDec ^ (xorKey | orKey);

                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new LdcInsnNode(orKey));
                list.add(new InsnNode(IOR));
                list.add(new InsnNode(I2L));
                list.add(new InsnNode(LXOR));

                // Add reverse call
                if (reverseIntegralNumbers.isEnabled())
                    list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "reverse", "(J)J", false));

                list.add(new InsnNode(L2D));
                list.add(new LdcInsnNode(onlyDec));
                list.add(new InsnNode(DADD));
            } else {
                final long xor = noDec ^ xorKey;

                list.add(new LdcInsnNode(xor));
                list.add(new LdcInsnNode(xorKey));
                list.add(new InsnNode(I2L));
                list.add(new InsnNode(LXOR));

                // Add reverse call
                if (reverseIntegralNumbers.isEnabled())
                    list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "reverse", "(J)J", false));

                list.add(new InsnNode(L2D));
                list.add(new LdcInsnNode(onlyDec));
                list.add(new InsnNode(DADD));
            }

            // negate the value twice
            for (int i = 0; i < 2; i++)
                list.add(new InsnNode(DNEG));
        }

        return list;
    }

    private InsnList obfuscatePUSH(final int pushOpcode, final int operand) {
        final InsnList list = new InsnList();

        if (aggressive.isEnabled()) {
            // get or keys
            final int[] orKeys = MathUtil.getTwoRandomInts(1, Short.MAX_VALUE);
            final int orKey = orKeys[0] | orKeys[1];

            // get xor key
            final int xorKey = MathUtil.randomInt(1, Short.MAX_VALUE);

            // perform operations on original value
            final int val = operand ^ (xorKey | orKey);

            // perform or operation on or keys
            list.add(new IntInsnNode(pushOpcode, orKeys[0]));
            list.add(new IntInsnNode(pushOpcode, orKeys[1]));
            list.add(new InsnNode(IOR));

            // perform or operation on xor key
            list.add(new IntInsnNode(pushOpcode, xorKey));
            list.add(new InsnNode(IOR));

            // perform xor operation on value
            list.add(new IntInsnNode(pushOpcode, val));
            list.add(new InsnNode(IXOR));
        } else {
            // perform basic xor operation
            final int key = MathUtil.randomInt(1, Short.MAX_VALUE);
            final int xorVal = operand ^ key;

            list.add(new IntInsnNode(pushOpcode, key)); // push key to stack
            list.add(new IntInsnNode(pushOpcode, xorVal)); // push xor'd value to stack
            list.add(new InsnNode(IXOR)); // perform xor operation
        }

        // negate the value twice
        for (int i = 0; i < 2; i++)
            list.add(new InsnNode(INEG));

        return list;
    }

    public void gotoEncryption(MethodNode method) {
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

    private InsnList processInt(int i) {
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

    private InsnList processLong(long l) {
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

package me.iris.ambien.obfuscator.transformers.implementations.flow.universal;

import me.iris.ambien.obfuscator.builders.InstructionBuilder;
import me.iris.ambien.obfuscator.builders.InstructionModifier;
import me.iris.ambien.obfuscator.utilities.kek.UnicodeDictionary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.concurrent.ThreadLocalRandom;

import static me.iris.ambien.obfuscator.transformers.implementations.flow.Flow.classMethodsMap;

public class GotoFlow {

    public static void gotoFlow() {
        classMethodsMap.forEach((classWrapper, methods) -> {
            transform(classWrapper.getNode());
        });
    }

    private static void transform(ClassNode node) {
        if (Modifier.isInterface(node.access)) return;

        UnicodeDictionary dictionary = new UnicodeDictionary(10);

        for (FieldNode field : node.fields) {
            dictionary.addUsed(field.name);
        }

        String fieldName = dictionary.get();
        boolean setupField = false;

        for (MethodNode method : node.methods) {
            if (method.instructions.size() == 0) continue;

            InstructionModifier modifier = new InstructionModifier();

            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof JumpInsnNode jumpInsn) {
                    if (jumpInsn.getOpcode() == Opcodes.GOTO) {
                        InstructionBuilder builder = new InstructionBuilder();
                        builder.fieldInsn(Opcodes.GETSTATIC, node.name, fieldName, "I");
                        builder.jump(Opcodes.IFLT, jumpInsn.label);

                        boolean pop = false;
                        int randomInt = ThreadLocalRandom.current().nextInt(0, 5);
                        switch (randomInt) {
                            case 0 -> {
                                builder.number(ThreadLocalRandom.current().nextInt());
                                pop = true;
                            }
                            case 1 -> builder.ldc(ThreadLocalRandom.current().nextLong());
                            case 2 -> {
                                builder.insn(Opcodes.ACONST_NULL);
                                pop = true;
                            }
                            case 3 -> {
                                builder.ldc(ThreadLocalRandom.current().nextFloat());
                                pop = true;
                            }
                            case 4 -> builder.ldc(ThreadLocalRandom.current().nextDouble());
                        }

                        if (pop) {
                            builder.insn(Opcodes.POP);
                        } else {
                            builder.insn(Opcodes.POP2);
                        }

                        builder.insn(Opcodes.ACONST_NULL);
                        builder.insn(Opcodes.ATHROW);

                        modifier.replace(jumpInsn, builder.getList());
                        setupField = true;
                    }
                } else if (instruction instanceof VarInsnNode varInsn) {
                    switch (varInsn.getOpcode()) {
                        case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> {
                            LabelNode label = new LabelNode();
                            method.maxLocals = method.maxLocals + (varInsn.getOpcode() == Opcodes.LLOAD || varInsn.getOpcode() == Opcodes.DLOAD ? 2 : 1);

                            int index = method.maxLocals;

                            InstructionBuilder builder = new InstructionBuilder();
                            builder.varInsn(varInsn.getOpcode() + 33, index);
                            builder.varInsn(varInsn.getOpcode(), index);
                            builder.fieldInsn(Opcodes.GETSTATIC, node.name, fieldName, "I");
                            builder.jump(Opcodes.IFLT, label);

                            builder.fieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                            builder.ldc(ThreadLocalRandom.current().nextLong());
                            builder.methodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(J)V", false);

                            builder.insn(Opcodes.ACONST_NULL);
                            builder.insn(Opcodes.ATHROW);
                            builder.label(label);

                            modifier.append(varInsn, builder.getList());
                            setupField = true;
                        }
                        case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> {
                            InstructionBuilder builder = new InstructionBuilder();
                            builder.varInsn(varInsn.getOpcode() - 33, varInsn.var);

                            if (varInsn.getOpcode() == Opcodes.DSTORE || varInsn.getOpcode() == Opcodes.LSTORE) {
                                builder.insn(Opcodes.POP2);
                            } else {
                                builder.insn(Opcodes.POP);
                            }

                            modifier.append(varInsn, builder.getList());
                        }
                    }
                }
            }

            modifier.apply(method);
        }

        if (setupField) {
            FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, fieldName, "I", null, ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 0));
            node.fields.add(field);
        }
    }
}

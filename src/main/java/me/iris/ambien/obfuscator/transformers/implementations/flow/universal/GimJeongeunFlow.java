package me.iris.ambien.obfuscator.transformers.implementations.flow.universal;

import me.iris.ambien.obfuscator.utilities.kek.myj2c.ASMUtils;
import me.iris.ambien.obfuscator.utilities.kek.myj2c.InsnBuilder;
import me.iris.ambien.obfuscator.wrappers.ClassWrapper;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.iris.ambien.obfuscator.transformers.implementations.flow.Flow.classMethodsMap;
import static org.objectweb.asm.Opcodes.*;

public class GimJeongeunFlow {
    private static final String FLOW_FIELD_NAME = "a";
    private static long flowFieldValue = 0;
    protected static final Random random = new Random();
    private static final int[] accessArr = new int[]{0, ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED};

    public static void gimJeongeunFlow() {
        classMethodsMap.forEach((classWrapper, methods) -> {
                    transformClass(classWrapper);
                    methods.stream()
                            .filter(methodWrapper -> classWrapper.getTransformableMethods().contains(methodWrapper))
                            .forEach(methodWrapper -> transformMethod(classWrapper.getNode(), methodWrapper.getNode()));
                }
        );
    }

    public static void transformClass(ClassWrapper classWrapper) {
        if (classWrapper.isInterface()) return;
        ClassNode classNode = classWrapper.getNode();
        if (!ASMUtils.isClassEligibleToModify(classNode)) {
            return;
        }
        flowFieldValue = ThreadLocalRandom.current().nextLong();
        classNode.fields.add(new FieldNode(accessArr[ThreadLocalRandom.current().nextInt(accessArr.length)] | 8, FLOW_FIELD_NAME, "J", null, flowFieldValue));
    }

    public static void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if (!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) {
            return;
        }
        Arrays.stream(methodNode.instructions.toArray()).filter(abstractInsnNode -> ASMUtils.isInvokeMethod(abstractInsnNode, true) || abstractInsnNode.getOpcode() == 187 || ASMUtils.isFieldInsn(abstractInsnNode)).forEach(abstractInsnNode -> {
            LabelNode labelNode = new LabelNode();
            LabelNode labelNode2 = new LabelNode();
            LabelNode labelNode3 = new LabelNode();
            LabelNode labelNode4 = new LabelNode();
            LabelNode labelNode5 = new LabelNode();
            LabelNode labelNode6 = new LabelNode();
            LabelNode labelNode7 = new LabelNode();
            InsnList insnList = new InsnList();
            InsnList insnList2 = new InsnList();
            switch (ThreadLocalRandom.current().nextInt(2)) {
                case 0: {
                    long l;
                    insnList.add(new JumpInsnNode(167, labelNode4));
                    insnList.add(labelNode3);
                    insnList.add(new InsnNode(87));
                    insnList.add(labelNode4);
                    insnList.add(new FieldInsnNode(178, classNode.name, FLOW_FIELD_NAME, "J"));
                    while ((l = ThreadLocalRandom.current().nextLong()) == flowFieldValue) {
                    }
                    insnList.add(ASMUtils.pushLong(l));
                    insnList.add(new InsnNode(148));
                    insnList.add(new InsnNode(89));
                    insnList.add(new JumpInsnNode(153, labelNode3));
                    insnList.add(ASMUtils.pushInt(flowFieldValue > l ? 1 : -1));
                    insnList.add(new JumpInsnNode(160, labelNode6));
                    insnList2.add(new JumpInsnNode(167, labelNode7));
                    insnList2.add(labelNode6);
                    insnList2.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                    insnList2.add(new JumpInsnNode(167, labelNode3));
                    insnList2.add(labelNode7);
                    break;
                }
                case 1: {
                    insnList.add(new FieldInsnNode(178, classNode.name, FLOW_FIELD_NAME, "J"));
                    insnList.add(new JumpInsnNode(167, labelNode2));
                    insnList.add(labelNode);
                    insnList.add(ASMUtils.pushLong(ThreadLocalRandom.current().nextLong()));
                    insnList.add(new InsnNode(109));
                    insnList.add(labelNode2);
                    insnList.add(new InsnNode(136));
                    insnList.add(getRandomLookupSwitch(2 + ThreadLocalRandom.current().nextInt(3), (int) flowFieldValue, new SwitchBlock(InsnBuilder.createEmpty().insn(new JumpInsnNode(167, labelNode5)).getInsnList()), () -> new SwitchBlock(InsnBuilder.createEmpty().insn(ASMUtils.pushLong(ThreadLocalRandom.current().nextLong()), new JumpInsnNode(167, labelNode)).getInsnList()), InsnBuilder.createEmpty().getInsnList()));
                    insnList.add(labelNode5);
                }
            }
            methodNode.instructions.insertBefore( abstractInsnNode, insnList);
            methodNode.instructions.insert( abstractInsnNode, insnList2);
        });
    }

    static final class SwitchBlock {
        private final LabelNode labelNode;
        private final InsnList insnList;

        public SwitchBlock() {
            this.labelNode = new LabelNode();
            this.insnList = new InsnList();
            this.insnList.add(getRandomLongDiv());
        }

        public SwitchBlock(InsnList insnList) {
            this.labelNode = new LabelNode();
            this.insnList = insnList;
        }

        public LabelNode getLabelNode() {
            return labelNode;
        }

        public InsnList getInsnList() {
            return insnList;
        }

    }


    static InsnList getRandomLookupSwitch(final int switchSize, final int targetKey, final SwitchBlock targetBlock, final Supplier<SwitchBlock> dummyBlock, final InsnList defInstructions) {
        final InsnList il = new InsnList();
        LabelNode switchDefaultLabel = new LabelNode();
        LabelNode switchEndLabel = new LabelNode();
        List<SwitchBlock> switchBlocks = IntStream.range(0, switchSize).mapToObj(v -> dummyBlock.get()).collect(Collectors.toList());
        List<Integer> keyList = getUniqueRandomIntArray(switchSize - 1);

        {
            keyList.add(targetKey);
            Collections.sort(keyList);
            switchBlocks.set(keyList.indexOf(targetKey), targetBlock);
        }

        il.add(new LookupSwitchInsnNode(switchDefaultLabel, keyList.stream().mapToInt(j -> j).toArray(), switchBlocks.stream().map(SwitchBlock::getLabelNode).toArray(LabelNode[]::new)));
        switchBlocks.forEach(switchBlock -> {
            il.add(switchBlock.getLabelNode());
            il.add(switchBlock.getInsnList());
            il.add(new JumpInsnNode(GOTO, switchEndLabel));
        });
        il.add(switchDefaultLabel);
        il.add(defInstructions);
        il.add(switchEndLabel);
        return il;
    }

    static List<Integer> getUniqueRandomIntArray(int size) {
        List<Integer> baseList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int j;
            do {
                j = ThreadLocalRandom.current().nextInt();
            } while (baseList.contains(j));
            baseList.add(j);
        }
        return baseList;
    }

    static InsnList getRandomLongDiv() {
        return InsnBuilder.createEmpty().insn(ASMUtils.pushLong(new Random().nextLong()), new InsnNode(LDIV)).getInsnList();
    }
}

package me.iris.ambien.obfuscator.builders;

import me.iris.ambien.obfuscator.utilities.ASMUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

@SuppressWarnings("SpellCheckingInspection")
public class InstructionBuilder {
    private InsnList list = new InsnList();

    public void insn(int opcode) {
        list.add(new InsnNode(opcode));
    }

    public void intInsn(int opcode, int operand) {
        list.add(new IntInsnNode(opcode, operand));
    }

    public void varInsn(int opcode, int index) {
        list.add(new VarInsnNode(opcode, index));
    }

    public void type(int opcode, String type) {
        list.add(new TypeInsnNode(opcode, type));
    }

    public void fieldInsn(int opcode, String owner, String name, String desc) {
        list.add(new FieldInsnNode(opcode, owner, name, desc));
    }

    public void methodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        list.add(new MethodInsnNode(opcode, owner, name, desc, isInterface));
    }

    public void invokeDynamic(String name, String desc, Handle handle, Object... args) {
        list.add(new InvokeDynamicInsnNode(name, desc, handle, args));
    }

    public void jump(int opcode, LabelNode label) {
        list.add(new JumpInsnNode(opcode, label));
    }

    public void ldc(Object value) {
        list.add(new LdcInsnNode(value));
    }

    public void iinc(int index, int increment) {
        list.add(new IincInsnNode(index, increment));
    }

    public void tableSwitch(int min, int max, LabelNode defaultLabel, LabelNode... labels) {
        list.add(new TableSwitchInsnNode(min, max, defaultLabel, labels));
    }

    public void lookupSwitch(LabelNode defaultLabel, int[] keys, LabelNode... labels) {
        list.add(new LookupSwitchInsnNode(defaultLabel, keys, labels));
    }

    public void multiANewArray(String desc, int numDimensions) {
        list.add(new MultiANewArrayInsnNode(desc, numDimensions));
    }

    public void number(int i) {
        list.add(ASMUtils.createNumberNode(i));
    }

    public InsnList getList() {
        return this.list;
    }

    public void addList(InsnList list) {
        this.list.add(list);
    }

    public void add(AbstractInsnNode node) {
        list.add(node);
    }

    public void label(LabelNode l) {
        list.add(l);
    }

    public void addArray(AbstractInsnNode[] array) {
        for (AbstractInsnNode abstractInsnNode : array) {
            list.add(abstractInsnNode);
        }
    }
}

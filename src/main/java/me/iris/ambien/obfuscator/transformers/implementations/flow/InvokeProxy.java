package me.iris.ambien.obfuscator.transformers.implementations.flow;

import me.iris.ambien.obfuscator.builders.InstructionModifier;
import me.iris.ambien.obfuscator.transformers.data.Category;
import me.iris.ambien.obfuscator.transformers.data.Ordinal;
import me.iris.ambien.obfuscator.transformers.data.Stability;
import me.iris.ambien.obfuscator.transformers.data.Transformer;
import me.iris.ambien.obfuscator.transformers.data.annotation.TransformerInfo;
import me.iris.ambien.obfuscator.utilities.ASMUtils;
import me.iris.ambien.obfuscator.utilities.kek.UnicodeDictionary;
import me.iris.ambien.obfuscator.wrappers.JarWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@TransformerInfo(
        name = "invoke-proxy",
        category = Category.CONTROL_FLOW,
        stability = Stability.STABLE,
        ordinal = Ordinal.HIGH,
        description = "asdcfasdfasdf."
)
public class InvokeProxy extends Transformer {

    @Override
    public void transform(JarWrapper wrapper) {
        getClasses(wrapper)
                .forEach(classWrapper -> invokeProxy(classWrapper.getNode()));
    }

    public void invokeProxy(ClassNode node) {
        if (Modifier.isInterface(node.access)) return;

        List<MethodNode> syntheticMethods = new ArrayList<>();
        UnicodeDictionary dictionary = new UnicodeDictionary(2);

        for (MethodNode method : node.methods) {
            dictionary.addUsed(method.name);
        }

        for (MethodNode method : node.methods) {
            InstructionModifier modifier = new InstructionModifier();

            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode methodInsn) {
                    switch (methodInsn.getOpcode()) {
                        case Opcodes.INVOKESTATIC: {
                            String methodName = dictionary.get();
                            MethodNode methodNode = new MethodNode(
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                    methodName,
                                    methodInsn.desc,
                                    null,
                                    null
                            );
                            Type returnType = Type.getReturnType(methodInsn.desc);

                            visitArgs(0, Type.getArgumentTypes(methodInsn.desc), methodNode);
                            methodNode.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    methodInsn.owner,
                                    methodInsn.name,
                                    methodInsn.desc,
                                    methodInsn.itf
                            );
                            visitReturn(returnType, methodNode);

                            syntheticMethods.add(methodNode);
                            modifier.replace(
                                    instruction,
                                    new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            node.name,
                                            methodName,
                                            methodInsn.desc,
                                            false
                                    )
                            );

                            
                            break;
                        }
                        case Opcodes.INVOKEVIRTUAL: {
                            Type[] types = Type.getArgumentTypes(methodInsn.desc);
                            Type[] desc = new Type[types.length + 1];
                            desc[0] = Type.getObjectType(methodInsn.owner);
                            System.arraycopy(types, 0, desc, 1, types.length);

                            String methodName = dictionary.get();
                            Type returnType = Type.getReturnType(methodInsn.desc);
                            String methodDesc = Type.getMethodDescriptor(returnType, desc);
                            MethodNode methodNode = new MethodNode(
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                    methodName,
                                    methodDesc,
                                    null,
                                    null
                            );

                            methodNode.visitVarInsn(Opcodes.ALOAD, 0);
                            visitArgs(1, types, methodNode);
                            methodNode.visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    methodInsn.owner,
                                    methodInsn.name,
                                    methodInsn.desc,
                                    methodInsn.itf
                            );
                            visitReturn(returnType, methodNode);

                            syntheticMethods.add(methodNode);
                            modifier.replace(
                                    instruction,
                                    new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            node.name,
                                            methodName,
                                            methodDesc,
                                            false
                                    )
                            );

                            
                            break;
                        }
                    }
                } else if (instruction instanceof FieldInsnNode fieldInsn) {
                    switch (fieldInsn.getOpcode()) {
                        case Opcodes.GETSTATIC: {
                            Type type = Type.getType(fieldInsn.desc);
                            String methodDescriptor = Type.getMethodDescriptor(type);
                            String methodName = dictionary.get();
                            MethodNode methodNode = new MethodNode(
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                    methodName,
                                    methodDescriptor,
                                    null,
                                    null
                            );

                            methodNode.visitFieldInsn(
                                    Opcodes.GETSTATIC,
                                    fieldInsn.owner,
                                    fieldInsn.name,
                                    fieldInsn.desc
                            );
                            visitReturn(type, methodNode);

                            syntheticMethods.add(methodNode);
                            modifier.replace(
                                    instruction,
                                    new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            node.name,
                                            methodName,
                                            methodDescriptor,
                                            false
                                    )
                            );

                            
                            break;
                        }
                        case Opcodes.PUTSTATIC: {
                            Type type = Type.getType(fieldInsn.desc);
                            String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, type);
                            String methodName = dictionary.get();
                            MethodNode methodNode = new MethodNode(
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                    methodName,
                                    methodDescriptor,
                                    null,
                                    null
                            );

                            visitArgs(0, new Type[]{type}, methodNode);
                            methodNode.visitFieldInsn(
                                    Opcodes.PUTSTATIC,
                                    fieldInsn.owner,
                                    fieldInsn.name,
                                    fieldInsn.desc
                            );
                            methodNode.visitInsn(Opcodes.RETURN);

                            syntheticMethods.add(methodNode);
                            modifier.replace(
                                    instruction,
                                    new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            node.name,
                                            methodName,
                                            methodDescriptor,
                                            false
                                    )
                            );

                            
                            break;
                        }
                        case Opcodes.GETFIELD: {
                            if (!method.name.equals("<init>")) {
                                Type type = Type.getType(fieldInsn.desc);
                                Type objectType = Type.getObjectType(fieldInsn.owner);
                                String methodDescriptor = Type.getMethodDescriptor(type, objectType);
                                String methodName = dictionary.get();
                                MethodNode methodNode = new MethodNode(
                                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                        methodName,
                                        methodDescriptor,
                                        null,
                                        null
                                );

                                visitArgs(0, new Type[]{objectType}, methodNode);
                                methodNode.visitFieldInsn(
                                        Opcodes.GETFIELD,
                                        fieldInsn.owner,
                                        fieldInsn.name,
                                        fieldInsn.desc
                                );
                                visitReturn(type, methodNode);

                                syntheticMethods.add(methodNode);
                                modifier.replace(
                                        instruction,
                                        new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                node.name,
                                                methodName,
                                                methodDescriptor,
                                                false
                                        )
                                );

                                
                            }
                            break;
                        }
                        case Opcodes.PUTFIELD: {
                            if (!method.name.equals("<init>")) {
                                Type type = Type.getType(fieldInsn.desc);
                                Type objectType = Type.getObjectType(fieldInsn.owner);
                                String methodDescriptor = Type.getMethodDescriptor(
                                        Type.VOID_TYPE,
                                        objectType,
                                        type
                                );
                                String methodName = dictionary.get();
                                MethodNode methodNode = new MethodNode(
                                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                        methodName,
                                        methodDescriptor,
                                        null,
                                        null
                                );

                                visitArgs(0, new Type[]{objectType, type}, methodNode);
                                methodNode.visitFieldInsn(
                                        Opcodes.PUTFIELD,
                                        fieldInsn.owner,
                                        fieldInsn.name,
                                        fieldInsn.desc
                                );
                                methodNode.visitInsn(Opcodes.RETURN);

                                syntheticMethods.add(methodNode);
                                modifier.replace(
                                        instruction,
                                        new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                node.name,
                                                methodName,
                                                methodDescriptor,
                                                false
                                        )
                                );

                                
                            }
                            break;
                        }
                    }
                }
            }

            modifier.apply(method);
        }

        for (MethodNode syntheticMethod : syntheticMethods) {
            ASMUtils.computeMaxLocals(syntheticMethod);
        }

        node.methods.addAll(syntheticMethods);
    }

    private void visitArgs(int offset, Type[] types, MethodNode methodNode) {
        int index = offset;

        for (Type type : types) {
            if (type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE ||
                    type == Type.CHAR_TYPE || type == Type.BOOLEAN_TYPE) {
                methodNode.visitVarInsn(Opcodes.ILOAD, index);
            } else if (type == Type.LONG_TYPE) {
                methodNode.visitVarInsn(Opcodes.LLOAD, index);
            } else if (type == Type.FLOAT_TYPE) {
                methodNode.visitVarInsn(Opcodes.FLOAD, index);
            } else if (type == Type.DOUBLE_TYPE) {
                methodNode.visitVarInsn(Opcodes.DLOAD, index);
            } else {
                methodNode.visitVarInsn(Opcodes.ALOAD, index);
            }

            if (type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
                index += 2;
            } else {
                index++;
            }
        }
    }

    private void visitReturn(Type type, MethodNode methodNode) {
        if (type.getSort() == Type.METHOD) {
            methodNode.visitInsn(Opcodes.RETURN);
        } else {
            switch (type.getSort()) {
                case Type.VOID:
                    methodNode.visitInsn(Opcodes.RETURN);
                    break;
                case Type.INT:
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.SHORT:
                case Type.BYTE:
                    methodNode.visitInsn(Opcodes.IRETURN);
                    break;
                case Type.FLOAT:
                    methodNode.visitInsn(Opcodes.FRETURN);
                    break;
                case Type.DOUBLE:
                    methodNode.visitInsn(Opcodes.DRETURN);
                    break;
                case Type.LONG:
                    methodNode.visitInsn(Opcodes.LRETURN);
                    break;
                default:
                    methodNode.visitInsn(Opcodes.ARETURN);
                    break;
            }
        }
    }
}

package me.iris.ambien.obfuscator.transformers.implementations.data.string;

import me.iris.ambien.obfuscator.builders.InstructionBuilder;
import me.iris.ambien.obfuscator.builders.InstructionModifier;
import me.iris.ambien.obfuscator.transformers.implementations.data.StringEncryption;
import me.iris.ambien.obfuscator.utilities.ASMUtils;
import me.iris.ambien.obfuscator.utilities.kek.UnicodeDictionary;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GotoStringEncryption {
    private static final List<EncryptData> data = new ArrayList<>();
    private static final List<FieldData> constFieldData = new ArrayList<>();

    public static void gotoEncryption(ClassNode node) {
        UnicodeDictionary dictionary = new UnicodeDictionary(100);
        int[] key = new int[257];
        for (int i = 0; i < 257; i++) {
            key[i] = ThreadLocalRandom.current().nextInt();
        }

        data.clear();
        constFieldData.clear();

        if (ASMUtils.isInterfaceClass(node)) {
            return;
        }

        for (FieldNode field : node.fields) {
            dictionary.addUsed(field.name);
        }

        for (MethodNode method : node.methods) {
            dictionary.addUsed(method.name);
        }

        String fieldName = dictionary.get();
        String decryptMethodName = dictionary.get();

        int offset = ThreadLocalRandom.current().nextInt();

        int pos = 0;

        for (MethodNode method : node.methods) {
            InstructionModifier modifier = new InstructionModifier();

            for (AbstractInsnNode instruction : method.instructions) {
                if (ASMUtils.isString(instruction)) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) instruction;
                    if (StringEncryption.stringBlacklist.getOptions().contains((String) ldcInsnNode.cst)) return;
                    modifier.replace(instruction,
                            new FieldInsnNode(Opcodes.GETSTATIC, node.name, fieldName, "[Ljava/lang/String;"),
                            new LdcInsnNode(pos),
                            new InsnNode(Opcodes.AALOAD)
                    );

                    data.add(new EncryptData(encode(ldcInsnNode.cst.toString(), offset, key, pos), pos));

                    pos++;
                }
            }

            modifier.apply(method);
        }

        for (FieldNode field : node.fields) {
            if (Modifier.isStatic(field.access)) {
                Object value = field.value;

                if (value instanceof String) {
                    if (StringEncryption.stringBlacklist.getOptions().contains(value)) return;
                    field.value = null;

                    data.add(new EncryptData(encode((String) value, offset, key, pos), pos));
                    constFieldData.add(new FieldData(pos, field));

                    pos++;
                }
            }
        }

        if (pos == 0) return;

        FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, fieldName, "[Ljava/lang/String;", null, null);
        node.fields.add(field);

        MethodNode staticMethod = ASMUtils.getClinitMethodNodeOrCreateNew(node);
        InstructionModifier modifier = new InstructionModifier();

        InstructionBuilder staticListBuilder = new InstructionBuilder();
        LabelNode startLabel = new LabelNode();
        LabelNode foreachStartLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        staticListBuilder.label(startLabel);
        staticListBuilder.number(data.size());
        staticListBuilder.type(Opcodes.ANEWARRAY, "java/lang/String");
        staticListBuilder.fieldInsn(Opcodes.PUTSTATIC, node.name, fieldName, "[Ljava/lang/String;");

        int stringsIndex = staticMethod.maxLocals++;
        int iIndex = staticMethod.maxLocals++;

        staticListBuilder.number(data.size());
        staticListBuilder.type(Opcodes.ANEWARRAY, "java/lang/String");
        staticListBuilder.insn(Opcodes.DUP);

        List<Integer> lengthList = new ArrayList<>(data.size());

        for (EncryptData encryptData : data) {
            lengthList.add(encryptData.encryptedString.length());

            staticListBuilder.number(encryptData.pos);
            staticListBuilder.ldc(encryptData.encryptedString);
            staticListBuilder.insn(Opcodes.AASTORE);

            if (encryptData.pos != data.size() - 1) {
                staticListBuilder.insn(Opcodes.DUP);
            }
        }

        staticListBuilder.varInsn(Opcodes.ASTORE, stringsIndex);

        staticListBuilder.insn(Opcodes.ICONST_0);
        staticListBuilder.varInsn(Opcodes.ISTORE, iIndex);
        staticListBuilder.label(foreachStartLabel);
        staticListBuilder.varInsn(Opcodes.ILOAD, iIndex);
        staticListBuilder.number(lengthList.size());
        staticListBuilder.jump(Opcodes.IF_ICMPGE, endLabel);
        staticListBuilder.varInsn(Opcodes.ALOAD, stringsIndex);
        staticListBuilder.varInsn(Opcodes.ILOAD, iIndex);
        staticListBuilder.insn(Opcodes.AALOAD);
        staticListBuilder.varInsn(Opcodes.ILOAD, iIndex);
        staticListBuilder.methodInsn(Opcodes.INVOKESTATIC, node.name, decryptMethodName, "(Ljava/lang/String;I)V", false);
        staticListBuilder.iinc(iIndex, 1);
        staticListBuilder.jump(Opcodes.GOTO, foreachStartLabel);

        staticListBuilder.label(endLabel);
        for (FieldData fieldData : constFieldData) {
            staticListBuilder.fieldInsn(Opcodes.GETSTATIC, node.name, fieldName, "[Ljava/lang/String;");
            staticListBuilder.number(fieldData.pos);
            staticListBuilder.insn(Opcodes.AALOAD);
            staticListBuilder.fieldInsn(Opcodes.PUTSTATIC, node.name, fieldData.fieldNode.name, fieldData.fieldNode.desc);
        }

        modifier.prepend(staticMethod.instructions.getFirst(), staticListBuilder.getList());
        modifier.apply(staticMethod);

        MethodNode decryptMethod = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, decryptMethodName, "(Ljava/lang/String;I)V", null, null);

        Label[] labels = new Label[522];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }

        decryptMethod.visitCode();
        decryptMethod.visitLabel(labels[0]);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 0);
        decryptMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
        decryptMethod.visitVarInsn(Opcodes.ASTORE, 2);
        decryptMethod.visitLabel(labels[1]);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 2);
        decryptMethod.visitInsn(Opcodes.ARRAYLENGTH);
        decryptMethod.visitIntInsn(Opcodes.NEWARRAY, 5);
        decryptMethod.visitVarInsn(Opcodes.ASTORE, 3);
        decryptMethod.visitLabel(labels[2]);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 0);
        decryptMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        decryptMethod.visitIntInsn(Opcodes.SIPUSH, 255);
        decryptMethod.visitInsn(Opcodes.IAND);
        decryptMethod.visitTableSwitchInsn(0,255,labels[515],labels[3],labels[5],labels[7],labels[9],labels[11],labels[13],labels[15],labels[17],labels[19],labels[21],labels[23],labels[25],labels[27],labels[29],labels[31],labels[33],labels[35],labels[37],labels[39],labels[41],labels[43],labels[45],labels[47],labels[49],labels[51],labels[53],labels[55],labels[57],labels[59],labels[61],labels[63],labels[65],labels[67],labels[69],labels[71],labels[73],labels[75],labels[77],labels[79],labels[81],labels[83],labels[85],labels[87],labels[89],labels[91],labels[93],labels[95],labels[97],labels[99],labels[101],labels[103],labels[105],labels[107],labels[109],labels[111],labels[113],labels[115],labels[117],labels[119],labels[121],labels[123],labels[125],labels[127],labels[129],labels[131],labels[133],labels[135],labels[137],labels[139],labels[141],labels[143],labels[145],labels[147],labels[149],labels[151],labels[153],labels[155],labels[157],labels[159],labels[161],labels[163],labels[165],labels[167],labels[169],labels[171],labels[173],labels[175],labels[177],labels[179],labels[181],labels[183],labels[185],labels[187],labels[189],labels[191],labels[193],labels[195],labels[197],labels[199],labels[201],labels[203],labels[205],labels[207],labels[209],labels[211],labels[213],labels[215],labels[217],labels[219],labels[221],labels[223],labels[225],labels[227],labels[229],labels[231],labels[233],labels[235],labels[237],labels[239],labels[241],labels[243],labels[245],labels[247],labels[249],labels[251],labels[253],labels[255],labels[257],labels[259],labels[261],labels[263],labels[265],labels[267],labels[269],labels[271],labels[273],labels[275],labels[277],labels[279],labels[281],labels[283],labels[285],labels[287],labels[289],labels[291],labels[293],labels[295],labels[297],labels[299],labels[301],labels[303],labels[305],labels[307],labels[309],labels[311],labels[313],labels[315],labels[317],labels[319],labels[321],labels[323],labels[325],labels[327],labels[329],labels[331],labels[333],labels[335],labels[337],labels[339],labels[341],labels[343],labels[345],labels[347],labels[349],labels[351],labels[353],labels[355],labels[357],labels[359],labels[361],labels[363],labels[365],labels[367],labels[369],labels[371],labels[373],labels[375],labels[377],labels[379],labels[381],labels[383],labels[385],labels[387],labels[389],labels[391],labels[393],labels[395],labels[397],labels[399],labels[401],labels[403],labels[405],labels[407],labels[409],labels[411],labels[413],labels[415],labels[417],labels[419],labels[421],labels[423],labels[425],labels[427],labels[429],labels[431],labels[433],labels[435],labels[437],labels[439],labels[441],labels[443],labels[445],labels[447],labels[449],labels[451],labels[453],labels[455],labels[457],labels[459],labels[461],labels[463],labels[465],labels[467],labels[469],labels[471],labels[473],labels[475],labels[477],labels[479],labels[481],labels[483],labels[485],labels[487],labels[489],labels[491],labels[493],labels[495],labels[497],labels[499],labels[501],labels[503],labels[505],labels[507],labels[509],labels[511],labels[513])
        ;

        for (int i = 0; i <= 255; i++) {
            decryptMethod.visitLabel(labels[3 + i * 2]);
            decryptMethod.instructions.add(ASMUtils.createNumberNode(((i + 1) ^ (~offset)) ^ key[i]));
            decryptMethod.visitVarInsn(Opcodes.ISTORE, 4);
            decryptMethod.visitLabel(labels[4 + i * 2]);
            decryptMethod.visitJumpInsn(Opcodes.GOTO, labels[516]);
        }

        decryptMethod.visitLabel(labels[515]);
        decryptMethod.instructions.add(ASMUtils.createNumberNode(257 ^ (~offset) ^ key[256]));
        decryptMethod.visitVarInsn(Opcodes.ISTORE, 4);
        decryptMethod.visitLabel(labels[516]);
        decryptMethod.visitInsn(Opcodes.ICONST_0);
        decryptMethod.visitVarInsn(Opcodes.ISTORE, 5);
        decryptMethod.visitLabel(labels[517]);
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 5);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 2);
        decryptMethod.visitInsn(Opcodes.ARRAYLENGTH);
        decryptMethod.visitJumpInsn(Opcodes.IF_ICMPGE, labels[520]);
        decryptMethod.visitLabel(labels[518]);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 3);
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 5);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 2);
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 5);
        decryptMethod.visitInsn(Opcodes.CALOAD);
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 5);
        decryptMethod.visitInsn(Opcodes.IXOR);
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 4);
        decryptMethod.visitInsn(Opcodes.IXOR);
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 1);
        decryptMethod.visitInsn(Opcodes.IXOR);
        decryptMethod.visitInsn(Opcodes.ICONST_M1);
        decryptMethod.visitInsn(Opcodes.IXOR);
        decryptMethod.visitInsn(Opcodes.I2C);
        decryptMethod.visitInsn(Opcodes.CASTORE);
        decryptMethod.visitLabel(labels[519]);
        decryptMethod.visitIincInsn(5, 1);
        decryptMethod.visitJumpInsn(Opcodes.GOTO, labels[517]);
        decryptMethod.visitLabel(labels[520]);

        decryptMethod.visitFieldInsn(Opcodes.GETSTATIC, node.name, fieldName, "[Ljava/lang/String;");
        decryptMethod.visitVarInsn(Opcodes.ILOAD, 1);
        decryptMethod.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        decryptMethod.visitInsn(Opcodes.DUP);
        decryptMethod.visitVarInsn(Opcodes.ALOAD, 3);
        decryptMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false);
        decryptMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;", false);
        decryptMethod.visitInsn(Opcodes.AASTORE);

        decryptMethod.visitInsn(Opcodes.RETURN);
        decryptMethod.visitEnd();

        ASMUtils.computeMaxLocals(decryptMethod);

        node.methods.add(decryptMethod);
    }

    private static String encode(String s, int offset, int[] key, int pos) {
        char[] chars = s.toCharArray();
        char[] decode = new char[chars.length];

        for (int i = 0; i < chars.length; i++) {
            decode[i] = (char) (~(chars[i] ^ (i ^ ((((s.length() & 0xFF) + 1) ^ (~offset)) ^ key[s.length() & 0xFF]) ^ pos)));
        }

        return new String(decode);
    }

    private record EncryptData(String encryptedString, int pos) {}
    private record FieldData(int pos, FieldNode fieldNode) {}
}

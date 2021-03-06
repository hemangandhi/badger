package edu.cmu.sv.badger.util;

import org.objectweb.asm.Opcodes;

import gov.nasa.jpf.symbc.bytecode.AALOAD;
import gov.nasa.jpf.symbc.bytecode.AASTORE;
import gov.nasa.jpf.symbc.bytecode.BALOAD;
import gov.nasa.jpf.symbc.bytecode.BASTORE;
import gov.nasa.jpf.symbc.bytecode.CALOAD;
import gov.nasa.jpf.symbc.bytecode.CASTORE;
import gov.nasa.jpf.symbc.bytecode.DALOAD;
import gov.nasa.jpf.symbc.bytecode.DASTORE;
import gov.nasa.jpf.symbc.bytecode.FALOAD;
import gov.nasa.jpf.symbc.bytecode.FASTORE;
import gov.nasa.jpf.symbc.bytecode.IALOAD;
import gov.nasa.jpf.symbc.bytecode.IASTORE;
import gov.nasa.jpf.symbc.bytecode.LALOAD;
import gov.nasa.jpf.symbc.bytecode.LASTORE;
import gov.nasa.jpf.symbc.bytecode.SALOAD;
import gov.nasa.jpf.symbc.bytecode.SASTORE;
import gov.nasa.jpf.symbc.bytecode.SwitchInstruction;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * @author Yannic Noller <nolleryc@gmail.com> - YN
 */
public class BytecodeUtils {

    public static int getNumberOfChoices(Instruction instruction) {

        if (instruction == null) {
            return 0;
        }

        int numberOfChoices;

        ThreadInfo ti;

        switch (instruction.getByteCode()) {

        // AFL does not consider all spf generated choices as branches
        /*case Opcodes.AALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((AALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (AALOAD.lastLength >= 0) {
                numberOfChoices = AALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.AASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null && ti.getElementInfo(((AASTORE) instruction).getArrayRef(ti)) != null) {
                numberOfChoices = ti.getElementInfo(((AASTORE) instruction).getArrayRef(ti)).getArrayFields()
                        .arrayLength() + 2;
            } else if (AASTORE.lastLength >= 0) {
                numberOfChoices = AASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.BALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((BALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (BALOAD.lastLength >= 0) {
                numberOfChoices = BALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.BASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((BASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (BASTORE.lastLength >= 0) {
                numberOfChoices = BASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.CALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((CALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (CALOAD.lastLength >= 0) {
                numberOfChoices = CALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.CASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((CASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (CASTORE.lastLength >= 0) {
                numberOfChoices = CASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.DALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((DALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (DALOAD.lastLength >= 0) {
                numberOfChoices = DALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.DASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((DASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (DASTORE.lastLength >= 0) {
                numberOfChoices = DASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.FALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((FALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (FALOAD.lastLength >= 0) {
                numberOfChoices = FALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.FASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((FASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (FASTORE.lastLength >= 0) {
                numberOfChoices = FASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.IALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((IALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (IALOAD.lastLength >= 0) {
                numberOfChoices = IALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.IASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((IASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (IASTORE.lastLength >= 0) {
                numberOfChoices = IASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.LALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((LALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (LALOAD.lastLength >= 0) {
                numberOfChoices = LALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.LASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((LASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (LASTORE.lastLength >= 0) {
                numberOfChoices = LASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.SALOAD:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((SALOAD) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (SALOAD.lastLength >= 0) {
                numberOfChoices = SALOAD.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.SASTORE:
            ti = ThreadInfo.getCurrentThread();
            if (ti.getTopFrame() != null) {
                numberOfChoices = (ti.getElementInfo(((SASTORE) instruction).getArrayRef(ti)).getArrayFields())
                        .arrayLength() + 2;
            } else if (SASTORE.lastLength >= 0) {
                numberOfChoices = SASTORE.lastLength;
            } else {
                numberOfChoices = 1;
            }
            break;

        case Opcodes.IDIV:
        case Opcodes.LDIV:
        case Opcodes.FDIV:
        case Opcodes.DDIV:
        case Opcodes.IREM:
        case Opcodes.LREM:*/
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
            numberOfChoices = 2;
            break;

        case Opcodes.TABLESWITCH:
        case Opcodes.LOOKUPSWITCH:
            // +1 because the default path is not
            numberOfChoices = ((SwitchInstruction) instruction).getTargets().length + 1;
            break;

        case Opcodes.LCMP:
        case Opcodes.FCMPL:
        case Opcodes.FCMPG:
        case Opcodes.DCMPL:
        case Opcodes.DCMPG:
            numberOfChoices = 3;
            break;

        default:
            // there are only choices for bytecode jump instructions and they are covered in
            // the code above
            numberOfChoices = 0;
        }

        return numberOfChoices;
    }

}

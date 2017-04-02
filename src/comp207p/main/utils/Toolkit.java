package comp207p.main.utils;

import comp207p.main.exceptions.UnableToFetchValueException;
import org.apache.bcel.generic.*;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by aousssbai on 01/04/2017.
 */
public class Toolkit {


    //=================================DEFINITION OF TOOLBOX FUNCTIONS========================================================================================================================================


    public static String retrieve_Sign(InstructionHandle handle, ConstantPoolGen cpgen) {
        Instruction instruction = handle.getInstruction();
        if(!(instruction instanceof TypedInstruction)) {
            throw new RuntimeException();
        }

        if(instruction instanceof LoadInstruction) {
            int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

            InstructionHandle handleIterator = handle;
            while (!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {
                handleIterator = handleIterator.getPrev();
                instruction = handleIterator.getInstruction();
            }


            handleIterator = handleIterator.getPrev();
            instruction = handleIterator.getInstruction();
        }

        return ((TypedInstruction)instruction).getType(cpgen).getSignature();
    }


    public static boolean verif_sign(InstructionHandle first, InstructionHandle second, ConstantPoolGen cpgen, String sign) {
        if (first.getInstruction() instanceof LoadInstruction && second.getInstruction() instanceof LoadInstruction) {
            if (retrieve_Sign(first, cpgen).equals(sign) || retrieve_Sign(second, cpgen).equals(sign)) {
                return true;
            }
        } else if (first.getInstruction() instanceof LoadInstruction) {
            if (retrieve_Sign(first, cpgen).equals(sign) || ((TypedInstruction)second.getInstruction()).getType(cpgen).getSignature().equals(sign)) {
                return true;
            }
        } else if (second.getInstruction() instanceof LoadInstruction) {
            if (((TypedInstruction)first.getInstruction()).getType(cpgen).getSignature().equals(sign) || retrieve_Sign(second, cpgen).equals(sign) ) {
                return true;
            }
        } else {
            if(((TypedInstruction)first.getInstruction()).getType(cpgen).getSignature().equals(sign) || ((TypedInstruction)second.getInstruction()).getType(cpgen).getSignature().equals(sign)) {
                return true;
            }
        }

        return false;
    }



    public static int typeInsertion(Number value, String type, ConstantPoolGen cpgen)
    {


        if (type == "F")
        {
            return cpgen.addFloat(value.floatValue());
        }
        else if (type == "B")
        {
            return cpgen.addInteger(value.intValue()); //Promote byte to integer
        }
        else if (type == "I")
        {
            return cpgen.addInteger(value.intValue());
        }
        else if (type == "J")
        {
            return cpgen.addLong(value.longValue());
        }
        else if (type == "S")
        {
            return cpgen.addInteger(value.intValue());
        }
        else if (type == "D")
        {
            return cpgen.addDouble(value.doubleValue());
        }
        else
        {
            throw new RuntimeException("Error");
        }
    }

    public static int initialComp(InstructionHandle comparison, Number first, Number second)
    {
        if (comparison.getInstruction() instanceof DCMPG)
        {
            if (first.doubleValue() > second.doubleValue()) return 1;
            else return -1;
        }
        else if (comparison.getInstruction() instanceof DCMPL)
        {
            if (first.doubleValue() < second.doubleValue()) return -1;
            else return 1;
        }
        else if (comparison.getInstruction() instanceof FCMPG)
        {
            if (first.floatValue() > second.floatValue()) return 1;
            else return -1;
        }
        else if (comparison.getInstruction() instanceof FCMPL)
        {
            if (first.floatValue() < second.floatValue()) return -1;
            else return 1;
        }
        else if (comparison.getInstruction() instanceof LCMP)
        {
            if (first.longValue() == second.longValue()) return 0;
            else if (first.longValue() > second.longValue()) return 1;
            else return -1;
        }
        else
        {
            throw new RuntimeException("error");
        }
    }

    public static int finalComp(IfInstruction comparison, int val)
    {
        if (comparison instanceof IFEQ || comparison instanceof IF_ICMPEQ)
        { //if equal
            if (val == 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFGE || comparison instanceof IF_ICMPGE)
        { //if greater than or equal
            if (val >= 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFGT || comparison instanceof IF_ICMPGT)
        { //if greater than
            if (val > 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFLE || comparison instanceof IF_ICMPLE)
        { //if less than or equal
            if (val <= 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFLT || comparison instanceof IF_ICMPLT)
        { //if less than
            if (val < 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFNE || comparison instanceof IF_ICMPNE)
        { //if not equal
            if (val != 0) return 1;
            else return 0;
        }
        else
        {
            throw new RuntimeException("error");
        }
    }

    public static Number retrieve_Val(InstructionHandle instruct, ConstantPoolGen cpgen, InstructionList register, String type) throws UnableToFetchValueException {
        Instruction instruction = instruct.getInstruction();

        if(instruction instanceof LoadInstruction)
        {
            return extract_instruct_val(instruct, cpgen, register, type);
        }

        else
        {
            return extract_const_val(instruct, cpgen);
        }
    }


    public static Number extract_const_val(InstructionHandle instruct, ConstantPoolGen cpgen) {
        Number value;

        if (instruct.getInstruction() instanceof ConstantPushInstruction) {
            value = ((ConstantPushInstruction) instruct.getInstruction()).getValue();
        } else if (instruct.getInstruction() instanceof LDC) {
            value = (Number) ((LDC) instruct.getInstruction()).getValue(cpgen);
        } else if (instruct.getInstruction() instanceof LDC2_W) {
            value = ((LDC2_W) instruct.getInstruction()).getValue(cpgen);
        } else {
            throw new RuntimeException();
        }

        return value;
    }


    public static Number extract_instruct_val(InstructionHandle instruct, ConstantPoolGen cpgen, InstructionList register, String type) throws UnableToFetchValueException {
        Instruction instruction = instruct.getInstruction();
        if(!(instruction instanceof LoadInstruction)) {
            throw new RuntimeException("error");
        }

        int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

        InstructionHandle handleIterator = instruct;
        int incrementAccumulator = 0;
        while(!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {

            if(instruction instanceof IINC) {
                IINC increment = (IINC) instruction;

                if(increment.getIndex() == localVariableIndex) {

                    if(verif_active_var(instruct, register)) {
                        throw new UnableToFetchValueException("Error");
                    }
                    incrementAccumulator += increment.getIncrement();
                }
            }

            handleIterator = handleIterator.getPrev();
            instruction = handleIterator.getInstruction();
        }


        handleIterator = handleIterator.getPrev();
        instruction = handleIterator.getInstruction();

        Number storeValue;
        if(instruction instanceof ConstantPushInstruction) {
            storeValue = ((ConstantPushInstruction) instruction).getValue();
        }
        else if (instruction instanceof LDC) {
            storeValue = (Number) ((LDC) instruction).getValue(cpgen);
        }
        else if (instruction instanceof LDC2_W) {
            storeValue = ((LDC2_W) instruction).getValue(cpgen);
        }
        else
        {
            throw new UnableToFetchValueException("error");
        }

        if (type=="D")

        {
            return ops(new DADD(), storeValue, incrementAccumulator);
        }

        else
        {
            return ops(new LADD(), storeValue, incrementAccumulator);
        }

    }





    public static Number ops(ArithmeticInstruction operation, Number first, Number second)
    {
        if (operation instanceof IADD || operation instanceof LADD)
        {
            return first.longValue() + second.longValue();
        }
        else if (operation instanceof FADD || operation instanceof DADD)
        {
            return first.doubleValue() + second.doubleValue();
        }
        else if (operation instanceof ISUB || operation instanceof LSUB)
        {
            return first.longValue() - second.longValue();
        }
        else if (operation instanceof FSUB || operation instanceof DSUB)
        {
            return first.doubleValue() - second.doubleValue();
        }
        else if (operation instanceof IMUL || operation instanceof LMUL)
        {
            return first.longValue() * second.longValue();
        }
        else if (operation instanceof FMUL || operation instanceof DMUL)
        {
            return first.doubleValue() * second.doubleValue();
        }
        else if (operation instanceof IDIV || operation instanceof LDIV)
        {
            return first.longValue() / second.longValue();
        }
        else if (operation instanceof FDIV || operation instanceof DDIV)
        {
            return first.doubleValue() / second.doubleValue();
        }
        else if (operation instanceof IREM || operation instanceof LREM)
        {
            return first.longValue() % second.longValue();
        }
        else if (operation instanceof FREM || operation instanceof DREM)
        {
            return first.doubleValue() % second.doubleValue();
        }
        else if (operation instanceof IAND || operation instanceof LAND)
        {
            return first.longValue() & second.longValue();
        }
        else if (operation instanceof IOR || operation instanceof LOR)
        {
            return first.longValue() | second.longValue();
        }
        else if (operation instanceof IXOR || operation instanceof LXOR)
        {
            return first.longValue() ^ second.longValue();
        }
        else if (operation instanceof ISHL || operation instanceof LSHL)
        {
            return first.longValue() << second.longValue();
        }
        else if (operation instanceof ISHR || operation instanceof LSHR)
        {
            return first.longValue() >> second.longValue();
        }
        else
        {
            throw new RuntimeException("error");
        }
    }




    public static String extract_folded_const_sign(InstructionHandle first, InstructionHandle second, ConstantPoolGen cpgen)
    {

        ArrayList< String > places = new ArrayList < String > (Arrays.asList("D", "F", "J", "S", "I", "B"));

        for (String letter: places)
        {
            if (verif_sign(first, second, cpgen, letter))
            {
                if (letter.equals("S") || letter.equals("B"))
                    return "I";
                else
                    return letter;
            }

        }

        throw new RuntimeException("error");
    }

    public static boolean verif_active_var(InstructionHandle instruct, InstructionList register)
    {
        if (verif_if(instruct) || verif_for(instruct, register))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static boolean verif_if(InstructionHandle instruct)
    {
        Instruction checkingInstruction = instruct.getInstruction();
        Instruction currentInstruction, currentSubInstruction;
        InstructionHandle handleIterator = instruct;
        while (handleIterator != null)
        {
            try
            {
                handleIterator = handleIterator.getPrev();
                currentInstruction = handleIterator.getInstruction();
                if (currentInstruction instanceof StoreInstruction &&
                        ((StoreInstruction) currentInstruction).getIndex() == ((LoadInstruction) checkingInstruction).getIndex())
                {
                    InstructionHandle subIterator = handleIterator;
                    while (subIterator != null)
                    {
                        subIterator = subIterator.getPrev();
                        currentSubInstruction = subIterator.getInstruction();
                        if (currentSubInstruction instanceof BranchInstruction)
                        {
                            if (((BranchInstruction) currentSubInstruction).getTarget().getPosition() > handleIterator.getPosition())
                            {
                                return true;
                            }
                            else
                            {
                                return false;
                            }
                        }
                    }
                }
            }
            catch (NullPointerException e)
            {
                break;
            }
        }

        return false;
    }

    public static boolean verif_for(InstructionHandle instruct, InstructionList register)
    {
        Instruction checkingInstruction = instruct.getInstruction();
        Instruction currentInstruction, previousInstruction, currentSubInstruction;
        InstructionHandle handleIterator = register.getStart();
        while (handleIterator != null)
        {
            try
            {
                handleIterator = handleIterator.getNext();
                currentInstruction = handleIterator.getInstruction();
                previousInstruction = handleIterator.getPrev().getInstruction();
                if (currentInstruction instanceof GotoInstruction && (previousInstruction instanceof IINC || previousInstruction instanceof StoreInstruction) && (handleIterator.getPosition() > ((BranchInstruction) currentInstruction).getTarget().getPosition()))
                {
                    if (((BranchInstruction) currentInstruction).getTarget().getInstruction().equals(checkingInstruction))
                    {
                        return true;
                    }
                    InstructionHandle subIterator = handleIterator;
                    while (subIterator != null)
                    {
                        subIterator = subIterator.getPrev();
                        currentSubInstruction = subIterator.getInstruction();
                        if (currentSubInstruction instanceof StoreInstruction)
                        {
                            if (((StoreInstruction) currentSubInstruction).getIndex() == ((LoadInstruction) checkingInstruction).getIndex())
                            {
                                return true;
                            }
                        }
                        else
                        {
                            if (subIterator.equals((InstructionHandle)((BranchInstruction) handleIterator.getInstruction()).getTarget()))
                            {
                                break;
                            }
                        }
                    }
                }
            }
            catch (NullPointerException e)
            {
                break;
            }
        }

        return false;
    }


    //=================================DEFINITION OF TOOLBOX FUNCTIONS========================================================================================================================================




}

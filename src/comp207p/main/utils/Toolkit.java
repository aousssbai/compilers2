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


    public static String getInstructionSignature(InstructionHandle h, ConstantPoolGen cpgen) {
        Instruction instruction = h.getInstruction();
        if(!(instruction instanceof TypedInstruction)) {
            throw new RuntimeException();
        }

        if(instruction instanceof LoadInstruction) {
            int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

            InstructionHandle handleIterator = h;
            while (!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {
                handleIterator = handleIterator.getPrev();
                instruction = handleIterator.getInstruction();
            }


            handleIterator = handleIterator.getPrev();
            instruction = handleIterator.getInstruction();
        }

        return ((TypedInstruction)instruction).getType(cpgen).getSignature();
    }


    public static boolean checkSignature(InstructionHandle left, InstructionHandle right, ConstantPoolGen cpgen, String signature) {
        if (left.getInstruction() instanceof LoadInstruction && right.getInstruction() instanceof LoadInstruction) {
            if (getInstructionSignature(left, cpgen).equals(signature) || getInstructionSignature(right, cpgen).equals(signature)) {
                return true;
            }
        } else if (left.getInstruction() instanceof LoadInstruction) {
            if (getInstructionSignature(left, cpgen).equals(signature) || ((TypedInstruction)right.getInstruction()).getType(cpgen).getSignature().equals(signature)) {
                return true;
            }
        } else if (right.getInstruction() instanceof LoadInstruction) {
            if (((TypedInstruction)left.getInstruction()).getType(cpgen).getSignature().equals(signature) || getInstructionSignature(right, cpgen).equals(signature) ) {
                return true;
            }
        } else {
            if(((TypedInstruction)left.getInstruction()).getType(cpgen).getSignature().equals(signature) || ((TypedInstruction)right.getInstruction()).getType(cpgen).getSignature().equals(signature)) {
                return true;
            }
        }

        return false;
    }



    public static int insertion(Number value, String type, ConstantPoolGen cpgen)
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

    public static int checkFirstComparison(InstructionHandle comparison, Number leftValue, Number rightValue)
    {
        if (comparison.getInstruction() instanceof DCMPG)
        {
            if (leftValue.doubleValue() > rightValue.doubleValue()) return 1;
            else return -1;
        }
        else if (comparison.getInstruction() instanceof DCMPL)
        {
            if (leftValue.doubleValue() < rightValue.doubleValue()) return -1;
            else return 1;
        }
        else if (comparison.getInstruction() instanceof FCMPG)
        {
            if (leftValue.floatValue() > rightValue.floatValue()) return 1;
            else return -1;
        }
        else if (comparison.getInstruction() instanceof FCMPL)
        {
            if (leftValue.floatValue() < rightValue.floatValue()) return -1;
            else return 1;
        }
        else if (comparison.getInstruction() instanceof LCMP)
        {
            if (leftValue.longValue() == rightValue.longValue()) return 0;
            else if (leftValue.longValue() > rightValue.longValue()) return 1;
            else return -1;
        }
        else
        {
            throw new RuntimeException("Comparison not defined");
        }
    }

    public static int checkSecondComparison(IfInstruction comparison, int value)
    {
        if (comparison instanceof IFEQ || comparison instanceof IF_ICMPEQ)
        { //if equal
            if (value == 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFGE || comparison instanceof IF_ICMPGE)
        { //if greater than or equal
            if (value >= 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFGT || comparison instanceof IF_ICMPGT)
        { //if greater than
            if (value > 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFLE || comparison instanceof IF_ICMPLE)
        { //if less than or equal
            if (value <= 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFLT || comparison instanceof IF_ICMPLT)
        { //if less than
            if (value < 0) return 1;
            else return 0;
        }
        else if (comparison instanceof IFNE || comparison instanceof IF_ICMPNE)
        { //if not equal
            if (value != 0) return 1;
            else return 0;
        }
        else
        {
            throw new RuntimeException("Comparison not defined, got: " + comparison.getClass());
        }
    }

    public static Number getValue(InstructionHandle h, ConstantPoolGen cpgen, InstructionList list, String type) throws UnableToFetchValueException {
        Instruction instruction = h.getInstruction();
        if(instruction instanceof LoadInstruction) {
            return getLoadInstructionValue(h, cpgen, list, type);
        } else {
            return getConstantValue(h, cpgen);
        }
    }


    public static Number getConstantValue(InstructionHandle h, ConstantPoolGen cpgen) {
        Number value;

        if (h.getInstruction() instanceof ConstantPushInstruction) {
            value = ((ConstantPushInstruction) h.getInstruction()).getValue();
        } else if (h.getInstruction() instanceof LDC) {
            value = (Number) ((LDC) h.getInstruction()).getValue(cpgen);
        } else if (h.getInstruction() instanceof LDC2_W) {
            value = ((LDC2_W) h.getInstruction()).getValue(cpgen);
        } else {
            throw new RuntimeException();
        }

        return value;
    }


    public static Number getLoadInstructionValue(InstructionHandle h, ConstantPoolGen cpgen, InstructionList list, String type) throws UnableToFetchValueException {
        Instruction instruction = h.getInstruction();
        if(!(instruction instanceof LoadInstruction)) {
            throw new RuntimeException("error");
        }

        int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

        InstructionHandle handleIterator = h;
        int incrementAccumulator = 0;
        while(!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {

            if(instruction instanceof IINC) {
                IINC increment = (IINC) instruction;

                if(increment.getIndex() == localVariableIndex) {

                    if(checkDynamicVariable(h, list)) {
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
            return foldOperation(new DADD(), storeValue, incrementAccumulator);
        }

        else
        {
            return foldOperation(new LADD(), storeValue, incrementAccumulator);
        }

    }





    public static Number foldOperation(ArithmeticInstruction operation, Number left, Number right)
    {
        if (operation instanceof IADD || operation instanceof LADD)
        {
            return left.longValue() + right.longValue();
        }
        else if (operation instanceof FADD || operation instanceof DADD)
        {
            return left.doubleValue() + right.doubleValue();
        }
        else if (operation instanceof ISUB || operation instanceof LSUB)
        {
            return left.longValue() - right.longValue();
        }
        else if (operation instanceof FSUB || operation instanceof DSUB)
        {
            return left.doubleValue() - right.doubleValue();
        }
        else if (operation instanceof IMUL || operation instanceof LMUL)
        {
            return left.longValue() * right.longValue();
        }
        else if (operation instanceof FMUL || operation instanceof DMUL)
        {
            return left.doubleValue() * right.doubleValue();
        }
        else if (operation instanceof IDIV || operation instanceof LDIV)
        {
            return left.longValue() / right.longValue();
        }
        else if (operation instanceof FDIV || operation instanceof DDIV)
        {
            return left.doubleValue() / right.doubleValue();
        }
        else if (operation instanceof IREM || operation instanceof LREM)
        {
            return left.longValue() % right.longValue();
        }
        else if (operation instanceof FREM || operation instanceof DREM)
        {
            return left.doubleValue() % right.doubleValue();
        }
        else if (operation instanceof IAND || operation instanceof LAND)
        {
            return left.longValue() & right.longValue();
        }
        else if (operation instanceof IOR || operation instanceof LOR)
        {
            return left.longValue() | right.longValue();
        }
        else if (operation instanceof IXOR || operation instanceof LXOR)
        {
            return left.longValue() ^ right.longValue();
        }
        else if (operation instanceof ISHL || operation instanceof LSHL)
        {
            return left.longValue() << right.longValue();
        }
        else if (operation instanceof ISHR || operation instanceof LSHR)
        {
            return left.longValue() >> right.longValue();
        }
        else
        {
            throw new RuntimeException("Not supported operation");
        }
    }




    public static String getFoldedConstantSignature(InstructionHandle left, InstructionHandle right, ConstantPoolGen cpgen)
    {

        ArrayList< String > places = new ArrayList < String > (Arrays.asList("D", "F", "J", "S", "I", "B"));

        for (String letter: places)
        {
            if (checkSignature(left, right, cpgen, letter))
            {
                if (letter.equals("S") || letter.equals("B"))
                    return "I";
                else
                    return letter;
            }

        }

        throw new RuntimeException("Type not defined: " +
                getInstructionSignature(left, cpgen) + " " +
                getInstructionSignature(right, cpgen));
    }

    public static boolean checkDynamicVariable(InstructionHandle h, InstructionList list)
    {
        if (checkIfCondition(h, list) || checkForLoop(h, list))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static boolean checkIfCondition(InstructionHandle h, InstructionList list)
    {
        Instruction checkingInstruction = h.getInstruction();
        Instruction currentInstruction, currentSubInstruction;
        InstructionHandle handleIterator = h;
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

    public static boolean checkForLoop(InstructionHandle h, InstructionList list)
    {
        Instruction checkingInstruction = h.getInstruction();
        Instruction currentInstruction, previousInstruction, currentSubInstruction;
        InstructionHandle handleIterator = list.getStart();
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

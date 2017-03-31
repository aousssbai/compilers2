package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import comp207p.main.exceptions.UnableToFetchValueException;
import comp207p.main.utils.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;




public class ConstantFolder
{
    ClassParser parser = null;
    ClassGen gen = null;

    JavaClass original = null;
    JavaClass optimized = null;


    private static final String LOAD_INSTRUCTION_REGEXP = "(ConstantPushInstruction|LDC|LDC2_W|LoadInstruction)";

    public ConstantFolder(String classFilePath)
    {
        try
        {
            this.parser = new ClassParser(classFilePath);
            this.original = this.parser.parse();
            this.gen = new ClassGen(this.original);
        }
        catch (IOException e)
        {

        }
    }

    public void write(String optimisedFilePath)
    {
        this.optimize();

        try
        {
            FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
            this.optimized.dump(out);
        }
        catch (FileNotFoundException e)
        {


        }
        catch (IOException e)
        {


        }
    }



    /**
     * Initial method
     */
    public void optimize()
    {
        ClassGen cgen = new ClassGen(original);
        cgen.setMajor(50);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        ConstantPool cp = cpgen.getConstantPool();
        Method[] methods = cgen.getMethods();



        for (Method m: methods)
        {


            optimiseMethod(cgen, cpgen, m);
        }

        this.optimized = cgen.getJavaClass();
    }

    /**
     * Optimise method instruction list
     */
    private void optimiseMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method)
    {
        Code methodCode = method.getCode();

        InstructionList instructionList = new InstructionList(methodCode.getCode());


        MethodGen methodGen = new MethodGen(
                method.getAccessFlags(),
                method.getReturnType(),
                method.getArgumentTypes(),
                null, method.getName(),
                cgen.getClassName(),
                instructionList,
                cpgen
        );

        int optimiseCounter = 1;


        while (optimiseCounter > 0)
        {
            optimiseCounter = 0;
            optimiseCounter += optimiseNegations(instructionList, cpgen);
            optimiseCounter += optimiseArithmeticOperation(instructionList, cpgen);
            optimiseCounter += optimiseComparisons(instructionList, cpgen);
            optimiseCounter += optimiseArithmeticOperation(instructionList, cpgen);
        }


        instructionList.setPositions(true);


        methodGen.setMaxStack();
        methodGen.setMaxLocals();


        Method newMethod = methodGen.getMethod();




        cgen.replaceMethod(method, newMethod);
    }

    /**
     * Fold negations
     * @param instructionList
     * @param cpgen
     * @return
     */
    private int optimiseNegations(InstructionList instructionList, ConstantPoolGen cpgen)
    {
        int changeCounter = 0;

        String regExp = LOAD_INSTRUCTION_REGEXP + " (INEG|FNEG|LNEG|DNEG)";


        InstructionFinder finder = new InstructionFinder(instructionList);

        for (Iterator it = finder.search(regExp); it.hasNext();)
        {
            InstructionHandle[] match = (InstructionHandle[]) it.next();



            InstructionHandle loadInstruction = match[0];
            InstructionHandle negationInstruction = match[1];

            Instruction instruction = negationInstruction.getInstruction();
            //

            if (instruction instanceof LoadInstruction)
            {
                int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

                InstructionHandle handleIterator = negationInstruction;
                while (!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex)
                {
                    handleIterator = handleIterator.getPrev();
                    instruction = handleIterator.getInstruction();
                }


                handleIterator = handleIterator.getPrev();
                instruction = handleIterator.getInstruction();
            }

            String type = ((TypedInstruction) instruction).getType(cpgen).getSignature();




            Number value = Toolkit.getValue(loadInstruction, cpgen, instructionList, type);
            Number a = -1;
            Number negatedValue = value.doubleValue() * a.doubleValue();


            int newPoolIndex = Toolkit.insertion(negatedValue, type, cpgen);



            if (type.equals("F") || type.equals("I") || type.equals("S"))
            {
                LDC newInstruction = new LDC(newPoolIndex);
                loadInstruction.setInstruction(newInstruction);
            }
            else
            {
                LDC2_W newInstruction = new LDC2_W(newPoolIndex);
                loadInstruction.setInstruction(newInstruction);
            }



            try
            {
                instructionList.delete(match[1]);
            }
            catch (TargetLostException e)
            {

            }


            changeCounter++;

        }

        return changeCounter;
    }

    /**
     * Optimise arithmetic operations
     * @param instructionList Instruction list
     * @return Number of changes made to instructions
     */
    private int optimiseArithmeticOperation(InstructionList instructionList, ConstantPoolGen cpgen)
    {
        int changeCounter = 0;

        String regExp = LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)? " +
                LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)? " +
                "ArithmeticInstruction";


        InstructionFinder finder = new InstructionFinder(instructionList);

        for (Iterator it = finder.search(regExp); it.hasNext();)
        {
            InstructionHandle[] match = (InstructionHandle[]) it.next();



            Number leftValue, rightValue;
            InstructionHandle leftInstruction, rightInstruction, operationInstruction;


            leftInstruction = match[0];
            if (match[1].getInstruction() instanceof ConversionInstruction)
            {
                rightInstruction = match[2];
            }
            else
            {
                rightInstruction = match[1];
            }
            if (rightInstruction == match[2] || (rightInstruction == match[1] && match[2].getInstruction() instanceof ConversionInstruction))
            {
                operationInstruction = match[3];
            }
            else if (rightInstruction == match[2] && match[3].getInstruction() instanceof ConversionInstruction)
            {
                operationInstruction = match[4];
            }
            else
            {
                operationInstruction = match[2];
            }

            if (leftInstruction.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.checkDynamicVariable(leftInstruction, instructionList))
                {

                    continue;
                }
            }
            if (rightInstruction.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.checkDynamicVariable(rightInstruction, instructionList))
                {

                    continue;
                }
            }


            String type = Toolkit.getFoldedConstantSignature(leftInstruction, rightInstruction, cpgen);




            try
            {
                leftValue = Toolkit.getValue(leftInstruction, cpgen, instructionList, type);
                rightValue =Toolkit.getValue(rightInstruction, cpgen, instructionList, type);
            }
            catch (UnableToFetchValueException e)
            {

                continue;
            }

            ArithmeticInstruction operation = (ArithmeticInstruction) operationInstruction.getInstruction();

            Number foldedValue = Toolkit.foldOperation(operation, leftValue, rightValue);




            int newPoolIndex = Toolkit.insertion(foldedValue, type, cpgen);

            if (type.equals("F") || type.equals("I") || type.equals("S"))
            {
                LDC newInstruction = new LDC(newPoolIndex);
                leftInstruction.setInstruction(newInstruction);
            }
            else
            {
                LDC2_W newInstruction = new LDC2_W(newPoolIndex);
                leftInstruction.setInstruction(newInstruction);
            }



            try
            {
                instructionList.delete(match[1], operationInstruction);
            }
            catch (TargetLostException e)
            {

            }


            changeCounter++;

            break;
        }

        return changeCounter;
    }

    /**
     * Optimise comparison instructions
     * @param instructionList Instruction list
     * @return Number of changes made to instructions
     */
    private int optimiseComparisons(InstructionList instructionList, ConstantPoolGen cpgen)
    {
        int changeCounter = 0;
        String regExp = LOAD_INSTRUCTION_REGEXP + "InvokeInstruction?" + " (ConversionInstruction)?" +
                LOAD_INSTRUCTION_REGEXP + "?" + " (ConversionInstruction)?" +
                "(LCMP|DCMPG|DCMPL|FCMPG|FCMPL)? IfInstruction (ICONST GOTO ICONST)?";

        InstructionFinder finder = new InstructionFinder(instructionList);

        for (Iterator it = finder.search(regExp); it.hasNext();)
        { // I
            InstructionHandle[] match = (InstructionHandle[]) it.next();



            Number leftValue = 0, rightValue = 0;
            InstructionHandle leftInstruction = null, rightInstruction = null, compare = null, comparisonInstruction = null;


            leftInstruction = match[0];

            if (!(match[1].getInstruction() instanceof IfInstruction))
            {
                rightInstruction = match[1];
            }
            else if (match[1].getInstruction() instanceof ConversionInstruction && !(match[2].getInstruction() instanceof IfInstruction))
            {
                rightInstruction = match[2];
            }
            else
            {
                rightInstruction = null;
            }

            int matchCounter = 0;
            if (rightInstruction != null)
            {


                if (rightInstruction == match[2] || (rightInstruction == match[1] && match[2].getInstruction() instanceof ConversionInstruction))
                {
                    matchCounter = 1;
                }
                else
                {
                    matchCounter = 0;
                }


            }
            else

            {
                if (!(match[1].getInstruction() instanceof ConversionInstruction))
                {
                    matchCounter = -1;
                }
            }

            if (leftInstruction.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.checkDynamicVariable(leftInstruction, instructionList))
                {

                    continue;
                }
            }

            if (rightInstruction != null && rightInstruction.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.checkDynamicVariable(rightInstruction, instructionList))
                {

                    continue;
                }
            }

            if (match[2 + matchCounter].getInstruction() instanceof IfInstruction)

            {
                comparisonInstruction = match[2 + matchCounter];
            }
            else
            {
                compare = match[2 + matchCounter];
                comparisonInstruction = match[3 + matchCounter];
            }

            String type;

            if (rightInstruction != null)
            {
                type = Toolkit.getFoldedConstantSignature(leftInstruction, rightInstruction, cpgen);
            }
            else

            {
                type = Toolkit.getInstructionSignature(leftInstruction, cpgen);
            }


            try

            {
                leftValue = Toolkit.getValue(leftInstruction, cpgen, instructionList, type);

                if (rightInstruction != null)
                {
                    rightValue = Toolkit.getValue(rightInstruction, cpgen, instructionList, type);
                }

            }
            catch (UnableToFetchValueException e)
            {

                continue;
            }

            IfInstruction comparison = (IfInstruction) comparisonInstruction.getInstruction();

            int result;

            if (rightInstruction != null)

            {
                if (comparisonInstruction == match[2])
                {

                    if (comparison instanceof IF_ICMPEQ)
                    {
                        if (leftValue.intValue() == rightValue.intValue()) result = 1;
                        else result = 0;
                    }
                    else if (comparison instanceof IF_ICMPGE)
                    {
                        if (leftValue.intValue() >= rightValue.intValue()) result = 1;
                        else result = 0;
                    }
                    else if (comparison instanceof IF_ICMPGT)
                    {
                        if (leftValue.intValue() > rightValue.intValue()) result = 1;
                        else result = 0;
                    }
                    else if (comparison instanceof IF_ICMPLE)
                    {
                        if (leftValue.intValue() <= rightValue.intValue()) result = 1;
                        else result = 0;
                    }
                    else if (comparison instanceof IF_ICMPLT)
                    {
                        if (leftValue.intValue() < rightValue.intValue()) result = 1;
                        else result = 0;
                    }
                    else if (comparison instanceof IF_ICMPNE)
                    {
                        if (leftValue.intValue() != rightValue.intValue()) result = 1;
                        else result = 0;
                    }
                    else
                    {
                        throw new RuntimeException("Comparison not defined");
                    }


                }
                else

                {
                    result = Toolkit.checkFirstComparison(compare, leftValue, rightValue);

                    result = Toolkit.checkSecondComparison(comparison, result);
                }

            }
            else

            {
                result = Toolkit.checkSecondComparison(comparison, leftValue.intValue());
            }


            if (result == 0)
            {
                ICONST newInstruction = new ICONST(1);
                leftInstruction.setInstruction(newInstruction);
                result = 1;
            }
            else if (result == 1)
            {
                ICONST newInstruction = new ICONST(0);
                leftInstruction.setInstruction(newInstruction);
                result = 0;
            }
            else
            {
                ICONST newInstruction = new ICONST(-1);
                leftInstruction.setInstruction(newInstruction);
            }




            try
            {
                if (match[match.length - 1].getInstruction() instanceof IfInstruction)
                {
                    InstructionHandle tempHandle = ((BranchInstruction) comparisonInstruction.getInstruction()).getTarget().getPrev();
                    if (result == 1)
                    {
                        instructionList.delete(match[0], comparisonInstruction);
                        if (tempHandle.getInstruction() instanceof GotoInstruction)
                        {
                            InstructionHandle gotoTarget = ((BranchInstruction) tempHandle.getInstruction()).getTarget().getPrev();
                            instructionList.delete(tempHandle, gotoTarget);
                        }
                    }
                    else
                    {
                        instructionList.delete(match[0], tempHandle);
                    }
                }
                else
                {
                    instructionList.delete(match[1], match[match.length - 1]);
                }
            }
            catch (TargetLostException e)
            {

            }


            changeCounter++;
            break;
        }

        return changeCounter;
    }

}
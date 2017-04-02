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


    private void optimiseMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method)
    {
        Code methodCode = method.getCode();

        InstructionList register = new InstructionList(methodCode.getCode());


        MethodGen methodGen = new MethodGen(
                method.getAccessFlags(),
                method.getReturnType(),
                method.getArgumentTypes(),
                null, method.getName(),
                cgen.getClassName(),
                register,
                cpgen
        );

        int count = 1;


        while (count > 0)
        {
            count = 0;
            count = count + comparisons(register, cpgen);
            count = count + arithmetic(register, cpgen);
            count = count + negations(register, cpgen);
            count = count + arithmetic(register, cpgen);



        }


        register.setPositions(true);


        methodGen.setMaxStack();
        methodGen.setMaxLocals();


        Method newMethod = methodGen.getMethod();




        cgen.replaceMethod(method, newMethod);
    }

    private int comparisons(InstructionList instructionList, ConstantPoolGen cpgen)
    {
        int count = 0;
        String regex = LOAD_INSTRUCTION_REGEXP + "InvokeInstruction?" + " (ConversionInstruction)?" +
                LOAD_INSTRUCTION_REGEXP + "?" + " (ConversionInstruction)?" +
                "(LCMP|DCMPG|DCMPL|FCMPG|FCMPL)? IfInstruction (ICONST GOTO ICONST)?";

        InstructionFinder retriever = new InstructionFinder(instructionList);

        for (Iterator iterate = retriever.search(regex); iterate.hasNext();)
        { // I
            InstructionHandle[] found = (InstructionHandle[]) iterate.next();



            Number first = 0, second = 0;
            InstructionHandle initial = null, finale = null, compare = null, comparisonInstruction = null;


            initial = found[0];

            if (!(found[1].getInstruction() instanceof IfInstruction))
            {
                finale = found[1];
            }
            else if (found[1].getInstruction() instanceof ConversionInstruction && !(found[2].getInstruction() instanceof IfInstruction))
            {
                finale = found[2];
            }
            else
            {
                finale = null;
            }

            int foundCount = 0;
            if (finale != null)
            {


                if (finale == found[2] || (finale == found[1] && found[2].getInstruction() instanceof ConversionInstruction))
                {
                    foundCount = 1;
                }
                else
                {
                    foundCount = 0;
                }


            }
            else

            {
                if (!(found[1].getInstruction() instanceof ConversionInstruction))
                {
                    foundCount = -1;
                }
            }

            if (initial.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.verif_active_var(initial, instructionList))
                {

                    continue;
                }
            }

            if (finale != null && finale.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.verif_active_var(finale, instructionList))
                {

                    continue;
                }
            }

            if (found[2 + foundCount].getInstruction() instanceof IfInstruction)

            {
                comparisonInstruction = found[2 + foundCount];
            }
            else
            {
                compare = found[2 + foundCount];
                comparisonInstruction = found[3 + foundCount];
            }

            String type;

            if (finale != null)
            {
                type = Toolkit.extract_folded_const_sign(initial, finale, cpgen);
            }
            else

            {
                type = Toolkit.retrieve_Sign(initial, cpgen);
            }


            try

            {
                first = Toolkit.retrieve_Val(initial, cpgen, instructionList, type);

                if (finale != null)
                {
                    second = Toolkit.retrieve_Val(finale, cpgen, instructionList, type);
                }

            }
            catch (UnableToFetchValueException e)
            {

                continue;
            }

            IfInstruction comp = (IfInstruction) comparisonInstruction.getInstruction();

            int output;

            if (finale != null)

            {
                if (comparisonInstruction == found[2])
                {

                    if (comp instanceof IF_ICMPEQ)
                    {
                        if (first.intValue() == second.intValue()) output = 1;
                        else output = 0;
                    }
                    else if (comp instanceof IF_ICMPGE)
                    {
                        if (first.intValue() >= second.intValue()) output = 1;
                        else output = 0;
                    }
                    else if (comp instanceof IF_ICMPGT)
                    {
                        if (first.intValue() > second.intValue()) output = 1;
                        else output = 0;
                    }
                    else if (comp instanceof IF_ICMPLE)
                    {
                        if (first.intValue() <= second.intValue()) output = 1;
                        else output = 0;
                    }
                    else if (comp instanceof IF_ICMPLT)
                    {
                        if (first.intValue() < second.intValue()) output = 1;
                        else output = 0;
                    }
                    else if (comp instanceof IF_ICMPNE)
                    {
                        if (first.intValue() != second.intValue()) output = 1;
                        else output = 0;
                    }
                    else
                    {
                        throw new RuntimeException("error");
                    }


                }
                else

                {
                    output = Toolkit.initialComp(compare, first, second);

                    output = Toolkit.finalComp(comp, output);
                }

            }
            else

            {
                output = Toolkit.finalComp(comp, first.intValue());
            }


            if (output == 0)
            {
                ICONST newInstruct = new ICONST(1);
                initial.setInstruction(newInstruct);
                output = 1;
            }
            else if (output == 1)
            {
                ICONST newInstruct = new ICONST(0);
                initial.setInstruction(newInstruct);
                output = 0;
            }
            else
            {
                ICONST newInstruct = new ICONST(-1);
                initial.setInstruction(newInstruct);
            }




            try
            {
                if (found[found.length - 1].getInstruction() instanceof IfInstruction)
                {
                    InstructionHandle handle_temp = ((BranchInstruction) comparisonInstruction.getInstruction()).getTarget().getPrev();
                    if (output == 1)
                    {
                        instructionList.delete(found[0], comparisonInstruction);
                        if (handle_temp.getInstruction() instanceof GotoInstruction)
                        {
                            InstructionHandle gotoTarget = ((BranchInstruction) handle_temp.getInstruction()).getTarget().getPrev();
                            instructionList.delete(handle_temp, gotoTarget);
                        }
                    }
                    else
                    {
                        instructionList.delete(found[0], handle_temp);
                    }
                }
                else
                {
                    instructionList.delete(found[1], found[found.length - 1]);
                }
            }
            catch (TargetLostException e)
            {

            }


            count++;
            break;
        }

        return count;
    }


    private int negations(InstructionList register, ConstantPoolGen cpgen)
    {
        int count = 0;

        String regex = LOAD_INSTRUCTION_REGEXP + " (INEG|FNEG|LNEG|DNEG)";


        InstructionFinder retriever = new InstructionFinder(register);

        for (Iterator iterate = retriever.search(regex); iterate.hasNext();)
        {
            InstructionHandle[] found = (InstructionHandle[]) iterate.next();



            InstructionHandle load = found[0];
            InstructionHandle negate = found[1];

            Instruction instruct = negate.getInstruction();


            if (instruct instanceof LoadInstruction)
            {
                int localVariableIndex = ((LocalVariableInstruction) instruct).getIndex();

                InstructionHandle handleIterator = negate;
                while (!(instruct instanceof StoreInstruction) || ((StoreInstruction) instruct).getIndex() != localVariableIndex)
                {
                    handleIterator = handleIterator.getPrev();
                    instruct = handleIterator.getInstruction();
                }


                handleIterator = handleIterator.getPrev();
                instruct = handleIterator.getInstruction();
            }

            String type = ((TypedInstruction) instruct).getType(cpgen).getSignature();




            Number value = Toolkit.retrieve_Val(load, cpgen, register, type);
            Number a = -1;
            Number negatedValue = value.doubleValue() * a.doubleValue();


            int newPoolIndex = Toolkit.typeInsertion(negatedValue, type, cpgen);



            if (type.equals("F") || type.equals("I") || type.equals("S"))
            {
                LDC newInstruct = new LDC(newPoolIndex);
                load.setInstruction(newInstruct);
            }
            else
            {
                LDC2_W newInstruct = new LDC2_W(newPoolIndex);
                load.setInstruction(newInstruct);
            }



            try
            {
                register.delete(found[1]);
            }
            catch (TargetLostException e)
            {

            }


            count++;

        }

        return count;
    }




    private int arithmetic(InstructionList register, ConstantPoolGen cpgen)
    {
        int count = 0;

        String regex = LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)? " +
                LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)? " +
                "ArithmeticInstruction";


        InstructionFinder retriever = new InstructionFinder(register);

        for (Iterator iterate = retriever.search(regex); iterate.hasNext();)
        {
            InstructionHandle[] found = (InstructionHandle[]) iterate.next();



            Number first, second;
            InstructionHandle initial, finale, op;


            initial = found[0];
            if (found[1].getInstruction() instanceof ConversionInstruction)
            {
                finale = found[2];
            }
            else
            {
                finale = found[1];
            }
            if (finale == found[2] || (finale == found[1] && found[2].getInstruction() instanceof ConversionInstruction))
            {
                op = found[3];
            }
            else if (finale == found[2] && found[3].getInstruction() instanceof ConversionInstruction)
            {
                op = found[4];
            }
            else
            {
                op = found[2];
            }

            if (initial.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.verif_active_var(initial, register))
                {

                    continue;
                }
            }
            if (finale.getInstruction() instanceof LoadInstruction)
            {
                if (Toolkit.verif_active_var(finale, register))
                {

                    continue;
                }
            }


            String type = Toolkit.extract_folded_const_sign(initial, finale, cpgen);




            try
            {
                first = Toolkit.retrieve_Val(initial, cpgen, register, type);
                second =Toolkit.retrieve_Val(finale, cpgen, register, type);
            }
            catch (UnableToFetchValueException e)
            {

                continue;
            }

            ArithmeticInstruction action = (ArithmeticInstruction) op.getInstruction();

            Number foldedValue = Toolkit.ops(action, first, second);




            int newRef = Toolkit.typeInsertion(foldedValue, type, cpgen);

            if (type.equals("F") || type.equals("I") || type.equals("S"))
            {
                LDC newInstruct = new LDC(newRef);
                initial.setInstruction(newInstruct);
            }
            else
            {
                LDC2_W newInstruct = new LDC2_W(newRef);
                initial.setInstruction(newInstruct);
            }



            try
            {
                register.delete(found[1], op);
            }
            catch (TargetLostException e)
            {

            }


            count++;

            break;
        }

        return count;
    }

}

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstructionJasmin {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final List<Report> reports;
    private final MethodJasmin method;

    public InstructionJasmin(Instruction instruction, MethodJasmin method) {
        this.instruction = instruction;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.method = method;
    }

    public StringBuilder getJasminCode() {
        return jasminCode;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void generateJasminCode() {
        instruction.show();
        switch (instruction.getInstType()) {
            case ASSIGN:
                generateAssign((AssignInstruction) instruction);
                break;
            case CALL:
                generateCall((CallInstruction) instruction);
                break;
            case RETURN:
                generateReturn((ReturnInstruction) instruction);
                break;
            case PUTFIELD:
                generatePutField((PutFieldInstruction) instruction);
                break;
            case BRANCH:
                generateBranch((CondBranchInstruction) instruction);
                break;
            case GOTO:
                generateGoto((GotoInstruction) instruction);
                break;

            default:
                break;
        }
    }

    private void generateAssign(AssignInstruction instruction) {
        String value, variable;
        Instruction rhs = instruction.getRhs();

        switch (rhs.getInstType()) {
            case NOPER:
                variable = ((Operand) instruction.getDest()).getName();

                Element rhsElement = ((SingleOpInstruction) rhs).getSingleOperand();
                if (rhsElement.isLiteral()){
                    value = ((LiteralElement)rhsElement).getLiteral();
                    decideInstructionConstSize(value);
                }
                else {
                    value = method.getLocalVariableByKey(((Operand)rhsElement).getName()).toString();
                    decideType(rhsElement);
                    jasminCode.append("load ");
                    jasminCode.append(value);
                }
                decideType(rhsElement);
                jasminCode.append("store ").append(method.getLocalVariableByKey(variable)).append("\n");

                break;

            case BINARYOPER:
                variable = ((Operand) instruction.getDest()).getName();

                OperationType operation = ((BinaryOpInstruction) rhs).getUnaryOperation().getOpType();
                Element leftElement = ((BinaryOpInstruction) rhs).getLeftOperand();
                Element rightElement = ((BinaryOpInstruction) rhs).getRightOperand();

                if (leftElement.isLiteral()){
                    value = ((LiteralElement)leftElement).getLiteral();
                    decideInstructionConstSize(value);
                }
                else {
                    decideType(leftElement);
                    jasminCode.append("load ");
                    jasminCode.append(method.getLocalVariableByKey(((Operand)leftElement).getName()));
                }

                if (rightElement.isLiteral()){
                    value = ((LiteralElement)rightElement).getLiteral();
                    decideInstructionConstSize(value);
                }
                else{
                    decideType(rightElement);
                    jasminCode.append("load ");
                    jasminCode.append(method.getLocalVariableByKey(((Operand)rightElement).getName()));
                }

                if (operation.toString().equals("LTH")) {
                    jasminCode.append("\n\n\t\tif_icmplt ").append("ElseLTH").append(method.getN_branches());
                    decideType(instruction.getDest());
                    jasminCode.append("const_1");

                    value = method.getLocalVariableByKey(variable).toString();
                    decideType(instruction.getDest());
                    jasminCode.append("store ");
                    jasminCode.append(value);

                    jasminCode.append("\n\t\tgoto ").append("AfterLTH").append(method.getN_branches());

                    jasminCode.append("\n\n\tElseLTH").append(method.getN_branches()).append(":");
                    decideType(instruction.getDest());
                    jasminCode.append("const_0");

                    decideType(instruction.getDest());
                    jasminCode.append("store ");
                    jasminCode.append(value);

                    jasminCode.append("\n\n\tAfterLTH").append(method.getN_branches()).append(":");
                    method.incN_branches();
                }
                else {
                    decideType(instruction.getDest());
                    jasminCode.append(operation.toString().toLowerCase(Locale.ROOT));

                    value = method.getLocalVariableByKey(variable).toString();
                    decideType(instruction.getDest());
                    jasminCode.append("store ");
                    jasminCode.append(value);

                    jasminCode.append("\n");
                }
                break;
            case GETFIELD:
                variable = ((Operand) instruction.getDest()).getName();
                generateGetField((GetFieldInstruction) rhs);
                decideType(instruction.getDest());
                jasminCode.append("store ");

                jasminCode.append(method.getLocalVariableByKey(variable));
                break;
            case CALL:
                variable = ((Operand) instruction.getDest()).getName();
                generateCall((CallInstruction) rhs);
                Element firstArg = ((CallInstruction) rhs).getFirstArg();
                Operand opFirstArg = (Operand) firstArg;
                if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF &&
                        opFirstArg.getName().equals(method.getClassName())) {
                    jasminCode.append("\n\t\tinvokespecial ").append(method.getClassName()).append(".<init>()V");
                }
                decideType(instruction.getDest());
                jasminCode.append("store ").append(method.getLocalVariableByKey(variable)).append("\n");
                break;
        }
    }

    // invokestatic, invokevirtual, invokespecial
    private void generateCall(CallInstruction instruction) {
        if(method.getMethod().isConstructMethod()){
            jasminCode.append("\n\taload 0");
            jasminCode.append("\n\tinvokespecial java/lang/Object.<init>()V");
            jasminCode.append("\n\treturn");
        }
        else{
            Element firstArg = instruction.getFirstArg();
            Operand opFirstArg = (Operand)firstArg;

            if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF){
                // Case operand is class name
                if (opFirstArg.getName().equals(method.getClassName())) {
                    jasminCode.append("\n\t\tnew ").append(method.getClassName());
                    jasminCode.append("\n\t\tdup");
                }
                else {
                    if (instruction.getNumOperands() > 1) {
                        Element secondArg = instruction.getSecondArg();
                        if (secondArg.isLiteral()) {
                            if (((LiteralElement) secondArg).getLiteral().replace("\"", "").equals("<init>"))
                                return;
                        }
                    }
                    decideType(opFirstArg);
                    jasminCode.append("load ").append(method.getLocalVariableByKey(opFirstArg.getName()));
                    for (Element parameter : instruction.getListOfOperands()){
                        decideType(parameter);
                        jasminCode.append("load ").append(method.getLocalVariableByKey(((Operand) parameter).getName()));
                    }
                    jasminCode.append("\n\t\tinvokevirtual ");
                    jasminCode.append(method.getClassName());
                    if (instruction.getNumOperands() > 1){
                        Element secondArg = instruction.getSecondArg();
                        if(secondArg.isLiteral()){
                            jasminCode.append(".");
                            jasminCode.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));
                            jasminCode.append("(");
                            for (Element parameter : instruction.getListOfOperands()){
                                jasminCode.append(decideInvokeReturns(parameter.getType()));
                            }
                            jasminCode.append(")");
                            jasminCode.append(decideInvokeReturns(instruction.getReturnType()));
                        }
                    }
                }
            }
            // Static call to a method
            else if(opFirstArg.getType().getTypeOfElement() == ElementType.CLASS){
                for (Element parameter : instruction.getListOfOperands()){
                    decideType(parameter);
                    jasminCode.append("load ").append(method.getLocalVariableByKey(((Operand) parameter).getName()));
                }
                jasminCode.append("\n\t\tinvokestatic ");
                jasminCode.append(opFirstArg.getName());
                if(instruction.getNumOperands() > 1){
                    Element secondArg = instruction.getSecondArg();
                    if(secondArg.isLiteral()){
                        jasminCode.append(".");
                        jasminCode.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));
                        jasminCode.append("(");
                        for (Element parameter : instruction.getListOfOperands()){
                            jasminCode.append(decideInvokeReturns(parameter.getType()));
                        }
                        jasminCode.append(")");
                        jasminCode.append(decideInvokeReturns(instruction.getReturnType()));
                    }
                }
            }

            // Case method is declared in the class
            else if(opFirstArg.getName().equals("this")) {
                jasminCode.append("\n\t\taload 0");
                jasminCode.append("\n\t\tinvokevirtual ");
                int nOperands = instruction.getNumOperands();
                if (nOperands > 1) {
                    Element secondArg = instruction.getSecondArg();
                    if (secondArg.isLiteral()) {
                        LiteralElement secondArgLiteral = (LiteralElement) secondArg;
                        jasminCode.append(method.getClassName());
                        jasminCode.append(".");
                        jasminCode.append(secondArgLiteral.getLiteral().replace("\"", ""));
                        jasminCode.append("(");
                        for (Element parameter : instruction.getListOfOperands()){
                            jasminCode.append(decideInvokeReturns(parameter.getType()));
                        }
                        jasminCode.append(")");
                        jasminCode.append(decideInvokeReturns(instruction.getReturnType()));
                    }
                }
            }
        }
    }

    private void generateReturn(ReturnInstruction instruction) {
        Element e1 = instruction.getOperand();
        if (e1 != null) {
            if (!e1.isLiteral()){
                decideType(instruction.getOperand());
                jasminCode.append("load ");
                String returnedVariable = ((Operand) instruction.getOperand()).getName();
                String value = method.getLocalVariableByKey(returnedVariable).toString();
                jasminCode.append(value);
                decideType(instruction.getOperand());
                jasminCode.append("return");
            }
            else { // return 0; return true;
                String literal = ((LiteralElement) e1).getLiteral();
                decideInstructionConstSize(literal);
                jasminCode.append("\n\t\tireturn");
            }

        } else {
            jasminCode.append("\n\t\treturn");
        }
    }

    private void generatePutField(PutFieldInstruction instruction) {

        Element e1 = instruction.getFirstOperand();
        Element e2 = instruction.getSecondOperand();
        Element e3 = instruction.getThirdOperand();
        Operand o1 = (Operand) e1;
        Operand o2 = (Operand) e2;

        if(e3.isLiteral()) { // if the e1 is not a literal, then it is a variable
            decideInstructionConstSize(((LiteralElement) e3).getLiteral());
        } else {
            Operand o3 = (Operand) e3;
            decideType(e3);
            jasminCode.append("load ").append(method.getLocalVariableByKey(o3.getName()));
        }

        decideType(e2);
        jasminCode.append("store ").append(method.getLocalVariableByKey(o2.getName()));

        String name = o1.getName();

        decideType(e1);
        jasminCode.append("load ").append(method.getLocalVariableByKey(name));
        if(name.equals("this")) name = method.getClassName();

        decideType(e2);
        jasminCode.append("load ").append(method.getLocalVariableByKey(o2.getName()));

        jasminCode.append("\n\t\tputfield ").append(name).append("/").append(o2.getName()).append(" ").append(decideInvokeReturns(e2.getType()));

    }

    private void generateGetField(GetFieldInstruction instruction) {
        String firstName = "";
        Element e1 = instruction.getFirstOperand();
        if (!e1.isLiteral()) { // if the e1 is not a literal, then it is a variable
            Operand o1 = (Operand) e1;
            firstName = o1.getName();
            jasminCode.append("\n\t\taload ").append(method.getLocalVariableByKey(o1.getName()));
        }

        if (firstName.equals("this")) firstName = method.getClassName();
        jasminCode.append("\n\t\tgetfield ").append(firstName).append("/");
        e1 = instruction.getSecondOperand();

        if (!e1.isLiteral()) { // if the e1 is not a literal, then it is a variable
            Operand o1 = (Operand) e1;
            jasminCode.append(o1.getName()).append(" ").append(decideInvokeReturns(o1.getType()));
        }

    }

    private void generateBranch(CondBranchInstruction instruction) {
        // TODO
        if (instruction.getLeftOperand().isLiteral()) {
            String leftLiteral = ((LiteralElement) instruction.getLeftOperand()).getLiteral();
            decideInstructionConstSize(leftLiteral);
        }
        else {
            Operand leftOperand = (Operand) instruction.getLeftOperand();
            jasminCode.append("\n\t\tiload ").append(method.getLocalVariableByKey(leftOperand.getName()));
        }

        if (instruction.getRightOperand().isLiteral()) {
            String rightLiteral = ((LiteralElement) instruction.getRightOperand()).getLiteral();
            decideInstructionConstSize(rightLiteral);
        }
        else {
            Operand rightOperand = (Operand) instruction.getRightOperand();
            jasminCode.append("\n\t\tiload ").append(method.getLocalVariableByKey(rightOperand.getName()));
        }

        OperationType conditionType = instruction.getCondOperation().getOpType();
        switch (conditionType) {
            case EQ:
                jasminCode.append("\n\t\tif_icmpeq ");
                break;
            case LTH:
                jasminCode.append("\n\t\tif_icmplt ");
                break;
            case ANDB:
                jasminCode.append("\n\t\tiandb");
                jasminCode.append("\n\t\ticonst_1");
                jasminCode.append("\n\t\tif_icmpeq ");
                break;
            default:
                break;
        }
        jasminCode.append(instruction.getLabel());
    }

    private void generateGoto(GotoInstruction instruction) {
        jasminCode.append("\n\t\tgoto ").append(instruction.getLabel()).append("\n");
    }

    private void decideInstructionConstSize(String value){
        int val = Integer.parseInt(value);
        String res;
        if (val >= 0 && val <= 5) res = "iconst_";
        else if (val > 5 && val <= 128) res = "bipush ";
        else if (val > 128 && val <= 32768) res = "sipush ";
        else res = "ldc ";
        jasminCode.append("\n\t\t").append(res).append(val);
    }

    private void decideType(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
                jasminCode.append("\n\t\ti");
                break;
            case BOOLEAN:
                jasminCode.append("\n\t\ti");
                break;
            case ARRAYREF:
                jasminCode.append("\n\t\ta");
                break;
            case THIS:
                jasminCode.append("\n\t\ta");
                break;
            case OBJECTREF:
                jasminCode.append("\n\t\ta");
                break;
            default:
                jasminCode.append("\n\t\t");
                break;
        }
    }

    public String decideInvokeReturns(Type type) {
        String returnType = null;
        switch (type.getTypeOfElement()) {
            case INT32:
                returnType = "I";
                break;
            case BOOLEAN:
                returnType = "Z";
                break;
            case ARRAYREF:
                returnType = "[I";
                break;
            case OBJECTREF:
                returnType = method.getClass().getName();
                break;
            case VOID:
                returnType = "V";
                break;
            default:
                break;
        }
        return returnType;
    }
}

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OLLIRVisitorTemp extends AJmmVisitor<String, OllirObject> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private final OllirObject code;
    private int loop_counter = 1;
    private int if_counter = 1;
    private int var_temp = 0;
    private SymbolMethod currentMethod;

    public OLLIRVisitorTemp(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = new OllirObject(OLLIRUtils.init(symbolTable.getClassName(), symbolTable.getFields()));
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("Statement", this::visitStatement);
        addVisit("Expression", this::visitExpression);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("Return", this::visitReturn);
        addVisit("Assign", this::visitAssign);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("FinalTerms", this::visitFinalTerms);
        addVisit("Call", this::visitCall);
        addVisit("Add", this::visitOperation);
        addVisit("Sub", this::visitOperation);
        addVisit("Mult", this::visitOperation);
        addVisit("Div", this::visitOperation);
        addVisit("Not", this::visitOperation);
        addVisit("And", this::visitOperation);
        addVisit("Less", this::visitOperation);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("IfElse", this::visitIfElse);
        setDefaultVisit(this::defaultVisit);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String getCode() {
        return code.getCode();
    }

    private OllirObject defaultVisit(JmmNode node, String dummy) {
        List<JmmNode> nodes = node.getChildren();
        OllirObject result = new OllirObject("");

        for (JmmNode child : nodes)
            result.appendToCode(visit(child));


        return result;
    }

    private OllirObject visitMethod(JmmNode node, String dummy) {
        var_temp = 0;

        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();

        StringBuilder methodInfo = new StringBuilder();
        boolean alreadyInBody = false;
        boolean hasReturn = false;

        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            if (child.getKind().equals("LParenthesis")) { // parameters
                if (!alreadyInBody) {
                    List<JmmNode> parameters = new ArrayList<>();
                    while (true) {
                        i++;
                        JmmNode aux = children.get(i);
                        if (aux.getKind().equals("RParenthesis")) break;
                        parameters.add(aux);
                    }
                    methodInfo.append(SemanticAnalysisUtils.getTypeParameters(parameters));
                }
            } else if (child.getKind().equals("Main")) {
                methodInfo.append("main(");
            } else if (child.getKind().contains("Identifier")) {
                String methodName = child.getKind().replaceAll("'", "").replace("Identifier ", "");
                methodInfo.append(methodName).append("(");
            } else if (child.getKind().equals("MethodBody")) {
                methodInfo.append(")");
                this.currentMethod = symbolTable.getMethodByInfo(methodInfo.toString());
                result.appendCode(OLLIRUtils.methodDeclaration(this.currentMethod));
                result.appendCode("{\n");
                result.appendToCode(visit(child));
                result.appendCode("\n"); //ver isto

                alreadyInBody = true;
            } else if (child.getKind().equals("Return")) {
                hasReturn = true;
                result.appendToCode(visit(child));
                result.appendCode("\n}\n\n");
            }
        }

        if (!hasReturn) result.appendCode(OLLIRUtils.returnVoid() + "}\n\n");

        this.currentMethod = null;
        this.code.appendToCode(result);

        return result;
    }


    private OllirObject visitReturn(JmmNode node, String dummy) {
        JmmNode returnExpression = node.getChildren().get(0).getChildren().get(0);
        OllirObject result = visit(returnExpression);
        checkReturnTemporary(result);
        return result;
    }

    private void checkReturnTemporary(OllirObject object) {

        if (object.returnNeedsTemp()) {
            var_temp++;
            object.getReturn(var_temp);
        } else object.getReturn();
    }

    private OllirObject visitMethodBody(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();
        for (JmmNode child : children) result.appendToCode(visit(child));
        return result;
    }

    private OllirObject visitStatement(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            OllirObject aux = visit(child);
            List<JmmNode> childrenC = child.getChildren();

            if (!aux.getCode().isEmpty() && !aux.getCode().endsWith(";") && !aux.getCode().endsWith(";\n") &&
                    !child.getKind().equals("WhileStatement") && !child.getKind().equals("IfElse") &&
                    !childrenC.get(childrenC.size() - 1).getKind().equals("WhileStatement") && !childrenC.get(childrenC.size() - 1).getKind().equals("IfElse")) {
                aux.appendCode(";\n");
            }

            result.appendToCode(aux);
        }
        return result;
    }

    private OllirObject visitExpression(JmmNode node, String dummy) {
        return visit(node.getChildren().get(0));
    }

    private OllirObject visitClassDeclaration(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        for (JmmNode child : node.getChildren()) result.appendToCode(visit(child));
        return result;
    }

    private OllirObject visitAssign(JmmNode node, String dummy) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftchild = children.get(0);
        JmmNode rightchild = children.get(1);

        OllirObject left = visit(leftchild);
        OllirObject right = visit(rightchild);

        String type = OLLIRUtils.getReturnTypeExpression(left.getCode());
        OllirObject result = new OllirObject(OLLIRUtils.assign(left.getCode(), type, right.getCode()));
        result.appendTemps(left);
        result.appendTemps(right);

        if (right.getCode().contains("new("))
            result.addBelowTemp(OLLIRUtils.invokeSpecial(left.getCode()));

        return result;
    }

    //TODO
    private OllirObject visitWhile(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();
        JmmNode condition = children.get(0);
        JmmNode body = children.get(1);

        result.appendCode("\nLoop" + loop_counter + ":\n");
        OllirObject cond = visit(condition);
        result.appendCode(cond.getAboveTemp());

        result.appendCode("if (" + cond.getCode() + ") goto Body" + loop_counter + ";\n");
        result.appendCode("goto EndLoop" + loop_counter + ";");
        result.appendCode("\nBody" + loop_counter + ":\n");

        OllirObject cond1 = visit(body);
        System.out.println("\nESTOUAQUI\n\n -> " + cond1.getCode() + "-------------\n\n");
        result.append(cond1);
        result.appendCode("\nEndLoop" + loop_counter + ":\n");

        loop_counter++;
        return result;
    }

    //TODO
    private OllirObject visitIfElse(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();
        OllirObject aux = visit(children.get(0));
        for (String above : aux.getAboveList()) result.addAboveTemp(above);

        result.appendCode("\nif (" + aux.getCode() + ") goto else" + if_counter + ";\n");
        result.appendCode(aux.getBelowTemp()); //TODO: not sure about this

        aux = visit(children.get(1));
        result.append(aux);

        result.appendCode("\ngoto endif" + if_counter + ";\n");
        result.appendCode("else" + if_counter + ":\n");

        aux = visit(children.get(1));
        result.append(aux);
        result.appendCode("\nendif" + if_counter + ":\n");

        if_counter++;
        return result;
    }

    //TODO : newintarrayexpression and its temporary variables
    private OllirObject visitFinalTerms(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();
        JmmNode child = children.get(0);
        String value;

        if (child.getKind().contains("Number")) {
            value = child.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            result.appendCode(value + ".i32");

        } else if (child.getKind().equals("NewIntArrayExpression")) { //TODO: check if var needs temporary variable
            OllirObject var = visit(child.getChildren().get(0));
            result.appendCode(var.getAboveTemp());
            result.appendCode("new(array," + var.getCode() + ").array.i32");
            result.appendCode(var.getBelowTemp()); //TODO: check this

        } else if (child.getKind().contains("NewIdentifier")) {
            value = child.getKind().replaceAll("'", "").replace("NewIdentifier ", "");
            result.appendCode("new(" + value + ")." + value); // new(myClass).myClass

        } else if (child.getKind().contains("Identifier")) {
            value = OLLIRUtils.getIdentifier(child, symbolTable, currentMethod);
            result.appendCode(value);
            if (!value.contains("putfield") && !value.contains("getfield"))
                result.appendCode(OLLIRUtils.getIdentifierType(child, symbolTable, currentMethod));

        } else if (child.getKind().equals("True") || child.getKind().equals("False")) {
            int bool = child.getKind().equals("True") ? 1 : 0;
            result.appendCode(bool + ".bool");

        } else if (child.getKind().equals("Expression"))
            result.append(visit(child));
        else if (child.getKind().equals("This"))
            result.appendCode("this");

        return result;
    }

    private OllirObject visitOperation(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                String resultLeft = checkExpressionTemporary(node.getChildren().get(0), result);
                String resultRight = checkExpressionTemporary(node.getChildren().get(1), result);

                result.appendCode(resultLeft + " " + OLLIRUtils.getOperationType(node) + " " + resultRight);
                return result;
            case "Not":
                String resultR = checkExpressionTemporary(node.getChildren().get(0), result);

                result.appendCode(OLLIRUtils.getOperationType(node) + " " + resultR);

                return result;
            default: // FinalTerms
                return new OllirObject("");
        }
    }

    private OllirObject visitArrayAccess(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        JmmNode identifier = node.getChildren().get(0);
        JmmNode access = node.getChildren().get(1);
        OllirObject i = visit(identifier);
        OllirObject a = visit(access);

        result.appendTemps(i);
        result.appendTemps(a);

        String aCode = a.getCode();
        String type = OLLIRUtils.getReturnTypeExpression(aCode);
        if (type.length() - type.replaceAll("\\.", "").length() > 1) type = type.substring(type.lastIndexOf("."));

        int ident;

        try {
            int index = a.getCode().indexOf(".");
            ident = Integer.parseInt(aCode.substring(0, index));
            var_temp++;
            aCode = "aux" + var_temp + ".i32";
            result.addAboveTemp(aCode + " :=.i32 " + ident + ".i32;");
        } catch (NumberFormatException ignored) {
        }

        result.appendCode(OLLIRUtils.getIdentifierExpression(i.getCode()) + "[" + aCode + "]" + type);

        return result;
    }

    private OllirObject visitCall(JmmNode node, String dummy) {

        List<JmmNode> children = node.getChildren();
        JmmNode child1 = children.get(0);
        JmmNode child2 = children.get(1);

        System.out.println("visit call: " + node + ", " + child1 + ", " + child2);
        if (child2.getKind().equals("MethodCall"))
            return visitMethodCall(node, child1, child2);
        else if (child2.getKind().equals("Length")) return visitLength(child1);

        return new OllirObject("");
    }

    private OllirObject visitMethodCall(JmmNode call, JmmNode firstChild, JmmNode method) {

        OllirObject result = new OllirObject("");
        OllirObject identifier = visit(firstChild);
        System.out.println("method call -> " + identifier);
        String invokeType = OLLIRUtils.getInvokeType(identifier.getCode(), method.getChildren().get(0), symbolTable);

        switch (invokeType) {
            case "virtual":
                List<String> temporary = new ArrayList<>();
                List<String> args = getMethodArgs(method, result);

                for (String s : args) {
                    String[] splitted = s.split("\\n");
                    temporary.addAll(Arrays.asList(splitted).subList(1, splitted.length));
                }

                String methodInfo = OLLIRUtils.getMethodInfo(method, args);
                SymbolMethod met = symbolTable.getMethodByInfo(methodInfo);
                String type = met.getReturnType().toOLLIR();

                for (String temp : temporary) result.addAboveTemp(temp + ";");
                String aux = identifier.getCode();

                if (identifier.getCode().contains("new(")) {
                    String identifierType = OLLIRUtils.getReturnTypeExpression(identifier.getCode());

                    var_temp++;
                    aux = "aux" + var_temp + identifierType;

                    result.addAboveTemp(aux + " :=" + identifierType + " " + identifier.getCode() + ";\n");
                    result.addAboveTemp(OLLIRUtils.invokeSpecial(aux));
                } else if (identifier.getCode().contains("invokevirtual")) {
                    String identifierType = OLLIRUtils.getReturnTypeExpression(identifier.getCode());

                    var_temp++;
                    aux = "aux" + var_temp + identifierType;
                    result.addAboveTemp(aux + " :=" + identifierType + " " + identifier.getCode() + ";\n");
                } else result.appendTemps(identifier);

                String methodCode = OLLIRUtils.invokeStaticVirtual(false, aux, OLLIRUtils.getMethodName(method.getChildren().get(0)), args, type);
                result.appendCode(methodCode);
                return result;
            case "static":
                List<String> arg = getMethodArgs(method, result);
                String returnType = getStaticReturnType(call);
                result.appendCode(OLLIRUtils.invokeStaticVirtual(true, identifier.getCode(), OLLIRUtils.getMethodName(method.getChildren().get(0)), arg, returnType));
                return result;
            default:
                return result;
        }
    }

    public List<String> getMethodArgs(JmmNode method, OllirObject res) {
        List<JmmNode> children = method.getChildren();
        List<String> args = new ArrayList<>();

        for (int i = 1; i < children.size(); i++) {
            String result = checkExpressionTemporary(children.get(i).getChildren().get(0), res);
            args.add(result);
        }

        return args;
    }

    public String getStaticReturnType(JmmNode method) {
        if (method.getParent().getKind().equals("Assign")) {
            JmmNode brother = method.getParent().getChildren().get(0);
            return OLLIRUtils.getReturnTypeExpression(visit(brother).getCode());
        } else return ".V";
    }

    private OllirObject visitLength(JmmNode identifier) {
        OllirObject result = new OllirObject("");
        String ident = checkExpressionTemporary(identifier, result);

        result.appendCode("arraylength(" + ident + ").i32");

        return result;
    }

    private String checkExpressionTemporary(JmmNode expression, OllirObject res) {
        OllirObject aux = visit(expression);
        res.appendTemps(aux);

        if (OLLIRUtils.hasOperation(expression) || OLLIRUtils.hasCall(expression) || OLLIRUtils.hasField(expression, symbolTable, currentMethod)) {
            String type;
            if (expression.getKind().equals("Call") || expression.getKind().contains("FinalTerms"))
                type = OLLIRUtils.getReturnTypeExpression(aux.getCode());
            else
                type = OLLIRUtils.getReturnTypeExpression(visit(expression.getChildren().get(0)).getCode());

            var_temp++;
            String temp_name = "aux" + var_temp + type;

            res.addAboveTemp(temp_name + " :=" + type + " " + aux.getCode());
            return temp_name;
        }

        return aux.getCode();
    }

}

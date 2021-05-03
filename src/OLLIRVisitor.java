import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OLLIRVisitor extends AJmmVisitor<StringBuilder, String> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private String code = "";
    private int var_temp = 0;
    private SymbolMethod currentMethod;

    public OLLIRVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = OLLIRTemplates.init(symbolTable.getClassName(), symbolTable.getFields());
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
        setDefaultVisit(this::defaultVisit);
    }

    public List<Report> getReports() { return reports; }

    public String getCode() { return code; }

    private String defaultVisit(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> nodes = node.getChildren();
        StringBuilder res = new StringBuilder();
        for(JmmNode child: nodes){
            String re = visit(child);
            res.append(re);
        }
        this.code += res.toString();
        return res.toString();
    }

    private String visitMethod(JmmNode node, StringBuilder stringBuilder) {
        var_temp = 0;
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        boolean ret = false;
        boolean alreadyInBody = false;

        StringBuilder methodInfo = new StringBuilder();

        for(int i = 0; i < children.size(); i++){
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
            }
            else if(child.getKind().equals("MethodBody")){
                methodInfo.append(")");
                this.currentMethod = symbolTable.getMethodByInfo(methodInfo.toString());
                res.append(OLLIRTemplates.methodDeclaration(this.currentMethod));
                res.append("{\n").append(visit(child)).append("\n");
                alreadyInBody = true;
            }
            else if(child.getKind().equals("Return")){
                ret = true;
                res.append(visit(child));
                res.append("\n}\n\n");
            }
        }
        if(!ret){
            res.append(OLLIRTemplates.returnVoid());
            res.append("}\n\n");
        }
        this.currentMethod = null;
        return res.toString();
    }

    //TODO
    private String visitReturn(JmmNode node, StringBuilder stringBuilder) {
        String result = checkReturnTemporary(node.getChildren().get(0).getChildren().get(0));
        if(result.equals(""))
            return OLLIRTemplates.returnTemplate(visit(node.getChildren().get(0)), OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))));
        else
            return OLLIRTemplates.returnTemplate("aux" + var_temp + OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))) + "\n" + result, OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))));

    }

    private String visitAssign(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftchild = children.get(0);
        JmmNode rightchild = children.get(1);

        String left = visit(leftchild);
        String right = visit(rightchild);
        if(left.contains("putfield")){
            String[] args = left.split(" ");
            String var = args[2];

            StringBuilder aux = new StringBuilder();
            String result = checkReturnTemporary(rightchild.getChildren().get(0));
            if(result.equals(""))
                aux.append(visit(rightchild));
            else
                aux.append("aux").append(var_temp).append(OLLIRTemplates.getReturnTypeExpression(visit(rightchild))).append("\n").append(result);

            System.out.println(aux);
            String[] temporary = aux.toString().split("\\n");

            StringBuilder res = new StringBuilder();
            for (int i = temporary.length - 1; i >= 1; i--) {
                res.append(temporary[i]).append(";\n");
            }
            return res + OLLIRTemplates.putField(var, temporary[0]);
        }
        else{
            String type = OLLIRTemplates.getReturnTypeExpression(left);
            return OLLIRTemplates.assign(left, type, right);
        }
    }


    private String visitMethodBody(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) res.append(visit(child));//only the statements matter

        return res.toString();
    }

    private String visitStatement(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for(JmmNode child: children){
            if(!(child.getKind().equals("WhileStatement") || child.getKind().equals("IfExpression"))) res.append(visit(child)); //ifs and whiles are not for this checkpoint
        }
        return res + ";\n";
    }

    private String visitExpression(JmmNode node, StringBuilder stringBuilder) {
        return visit(node.getChildren().get(0));
    }
    private String visitClassDeclaration(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        for(JmmNode child : node.getChildren()){
            res.append(visit(child));
        }
        return res.toString();
    }


    private String visitFinalTerms(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder result = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        JmmNode child = children.get(0);
        String ret, res = "", value;
        if(child.getKind().contains("Number")){
            ret = ".i32";
            value = child.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            res = value + ret;
        } else if (child.getKind().equals("NewIntArrayExpression")) { //not for this checkpoint
            ret = "";
            res = "";
        } else if (child.getKind().contains("NewIdentifier")) {
            value = child.getKind().replaceAll("'", "").replace("NewIdentifier ", "").trim();
            res = "new(" + value + ")." + value; // new(myClass).myClass
        } else if (child.getKind().contains("Identifier")) {
            value = OLLIRTemplates.getIdentifier(child, symbolTable, currentMethod);
            res = value;
            if(!value.contains("putfield") && !value.contains("getfield")) res += OLLIRTemplates.getIdentifierType(child, symbolTable, currentMethod);
        } else if (child.getKind().equals("True") || child.getKind().equals("False")) {
            ret = ".bool";
            int bool = child.getKind().equals("True") ? 1 : 0;
            res = bool + ret;
        } else if(child.getKind().equals("Expression")){ // new int[EXPRESSION] -> nao é necessário nesta entrega
            res = visit(child);
        }else if(child.getKind().equals("This")){
            res = "this";

        }

        result.append(res);
        return result.toString();
    }

    private String visitCall(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> children = node.getChildren();
        JmmNode left = children.get(0);
        JmmNode right = children.get(1);

        if(left.getKind().equals("FinalTerms") && right.getKind().equals("MethodCall")) return visitMethodCall(node, left, right);
        else if(left.getKind().equals("FinalTerms") && right.getKind().equals("Length")) return visitLength(left);
        return "";
    }


    private String visitMethodCall(JmmNode call, JmmNode finalterm, JmmNode method ) {
        String identifier = visit(finalterm);
        String invokeType = OLLIRTemplates.getInvokeType(identifier, method.getChildren().get(0), symbolTable);

        switch(invokeType){
            case "virtual":
                List<String> temporary = new ArrayList<>();
                List<String> args = getMethodArgs(method);
                for (int i = 0; i < args.size(); i++) {
                    String[] splitted = args.get(i).split("\\n");
                    args.set(i, splitted[0]);
                    temporary.addAll(Arrays.asList(splitted).subList(1, splitted.length));
                }
                String methodInfo = OLLIRTemplates.getMethodInfo(method, args);
                SymbolMethod met = symbolTable.getMethodByInfo(methodInfo);
                StringBuilder res = new StringBuilder();
                for (String temp: temporary) {
                    res.append(temp).append(";\n");
                }
                return res + OLLIRTemplates.invokeStaticVirtual(false, identifier, OLLIRTemplates.getMethodName(method.getChildren().get(0)), args, met.getReturnType().toOLLIR());
            case "static":
                List<String> arg = getMethodArgs(method);
                String returnType = getStaticReturnType(call);
                return OLLIRTemplates.invokeStaticVirtual(true, identifier, OLLIRTemplates.getMethodName(method.getChildren().get(0)), arg, returnType);
            default:
                return "";
        }
    }

    public List<String> getMethodArgs(JmmNode method){
        List<JmmNode> children = method.getChildren();
        List<String> args = new ArrayList<>();
        for(int i = 1; i < children.size(); i++) {
            String result = checkReturnTemporary(children.get(i).getChildren().get(0));
            if(result.equals(""))
                args.add(visit(children.get(i)));
            else
                args.add("aux" + var_temp + OLLIRTemplates.getReturnTypeExpression(visit(children.get(i).getChildren().get(0))) + "\n" + result);
        }
        return args;
    }

    public String getStaticReturnType(JmmNode method){
        if(method.getParent().getKind().equals("Assign")) {
            JmmNode brother = method.getParent().getChildren().get(0);
            String v = visit(brother);
            return OLLIRTemplates.getReturnTypeExpression(v);
        }
        else return ".V";
    }

    private String visitLength(JmmNode identifier) {
        String array = visit(identifier);
        return "arraylength(" + array + ").i32";
    }

    private String visitOperation(JmmNode node, StringBuilder stringBuilder) {
        String resultLeft, resultRight;
        switch (node.getKind()){
            // Binary Instructions
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                resultLeft = checkReturnTemporary(node.getChildren().get(0));
                String leftAux = "aux" + var_temp;
                resultRight = checkReturnTemporary(node.getChildren().get(1));
                String rightAux = "aux" + var_temp;

                if (resultLeft.equals("") && resultRight.equals(""))
                    return visit(node.getChildren().get(0)) + " " + OLLIRTemplates.getOperationType(node) + " " + visit(node.getChildren().get(1));
                else if (resultLeft.equals(""))
                    return visit(node.getChildren().get(0)) + " " + OLLIRTemplates.getOperationType(node) + " " + rightAux + OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))) + "\n" + resultRight;
                else if (resultRight.equals(""))
                    return leftAux + OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))) + " " + OLLIRTemplates.getOperationType(node) + " " + visit(node.getChildren().get(1)) + "\n" + resultLeft;
                else
                    return leftAux + OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))) + " " + OLLIRTemplates.getOperationType(node) + " " + rightAux + OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))) + "\n" + resultRight + "\n" + resultLeft;
                // Unary Instruction
            case "Not":
                resultRight = checkReturnTemporary(node.getChildren().get(0));
                if (resultRight.equals(""))
                    return OLLIRTemplates.getOperationType(node) + " " + visit(node.getChildren().get(0));
                else
                    return OLLIRTemplates.getOperationType(node) + " " + resultRight;
            default: // FinalTerms
                return "";
        }
    }

    private String visitArrayAccess(JmmNode node, StringBuilder stringBuilder) { //not for this checkpoint
        StringBuilder res = new StringBuilder();
        return res.toString();
    }

    public String checkReturnTemporary(JmmNode expression) {
        StringBuilder result = new StringBuilder();
        if(OLLIRTemplates.hasOperation(expression) || OLLIRTemplates.hasCall(expression) || OLLIRTemplates.hasField(expression, symbolTable, currentMethod))
        {
            String aux = visit(expression);
            String type;
            if(expression.getKind().equals("Call") || expression.getKind().contains("FinalTerms"))
                type = OLLIRTemplates.getReturnTypeExpression(visit(expression));
            else
                type = OLLIRTemplates.getReturnTypeExpression(visit(expression.getChildren().get(0)));
            var_temp++;
            result.append("aux").append(var_temp).append(type).append(" :=").append(type).append(" ").append(aux);
        }
        return result.toString();
    }

}
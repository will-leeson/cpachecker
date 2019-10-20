package org.sosy_lab.cpachecker.intelligence.ast.neural;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.intelligence.ast.IEdgeListener;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer.PEGraph;

public class PointerListener2 implements IEdgeListener {

    private PEGraph graph;
    private ShutdownNotifier notifier;

    public PointerListener2(PEGraph graph, ShutdownNotifier notifier) {
        this.graph = graph;
        this.notifier = notifier;
    }

    private boolean isPointer(CType type){
        return type instanceof CPointerType;
    }

    private boolean isPointer(CPointerExpression expression){
        return isPointer(expression.getOperand());
    }

    private boolean isPointer(CIdExpression expression){
        return isPointer(expression.getDeclaration().getType());
    }

    private boolean isPointer(CExpression expression){

        if(expression instanceof CPointerExpression ){
            return isPointer((CPointerExpression)expression);
        }

        if(expression instanceof CIdExpression){
            return isPointer((CIdExpression)expression);
        }

        return false;
    }

    private String getId(String s){
        return s.replace("*", "P").replace("&", "A").replace("::", "_");
    }

    private String handleExpr(CExpression expression){

        if(expression instanceof CUnaryExpression){
            return handleUnary((CUnaryExpression)expression);
        }

        if(expression instanceof CIdExpression){
            return handleId((CIdExpression)expression);
        }

        if(expression instanceof CPointerExpression){
            return handlePointer((CPointerExpression)expression);
        }

        throw new UnsupportedOperationException(expression.toASTString());
    }

    private String handleUnary(CUnaryExpression expression){

        if(expression.getOperator() == CUnaryExpression.UnaryOperator.AMPER){
            String inner = handleExpr(expression.getOperand());
            if(inner.charAt(0) == '*'){
                return inner.substring(1);
            }else{
                return "&" + inner;
            }
        }

        throw new UnsupportedOperationException(expression.toASTString());
    }

    private String handleId(CIdExpression idExpression){
        return idExpression.getDeclaration().getQualifiedName();
    }

    private String handlePointer(CPointerExpression pointerExpression){

        String inner = handleExpr(pointerExpression.getOperand());
        if(inner.charAt(0) == '&'){
            return inner.substring(1);
        }else{
            return "*" + inner;
        }

    }


    private void handleAssign(String name, CExpression rightHand){

        String target = handleExpr(rightHand);
        addNode(target);
        graph.addAssignEdge(getId(target), name);
    }

    private void addNode(String name){

        graph.addNode(getId(name), name);
        String pre = name;
        while(pre.charAt(0) == '*'){
            String n = pre.substring(1);
            graph.addNode(getId(n), n);
            graph.addDerefEdge(getId(n), getId(pre));
            pre = n;
        }

        while(pre.charAt(0) == '&'){
            String n = pre.substring(1);
            graph.addNode(getId(n), n);
            graph.addDerefEdge(getId(pre), getId(n));
            pre = n;
        }



    }


    @Override
    public void listen(CFAEdge edge) {

        if(edge instanceof CDeclarationEdge){

            CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();

            if(!(declaration instanceof CVariableDeclaration))return;

            CVariableDeclaration varDecl = (CVariableDeclaration)declaration;

            if(!isPointer(varDecl.getType())){
                return;
            }

            String def = varDecl.getQualifiedName();
            String amp = "&"+def;

            addNode(def);
            addNode(amp);
            addNode("*"+def);

            CInitializer init = varDecl.getInitializer();
            if(init != null && init instanceof CInitializerExpression){
                handleAssign(getId(def), ((CInitializerExpression) init).getExpression());
            }

        }else if(edge instanceof CStatementEdge){

            CStatement statement = ((CStatementEdge) edge).getStatement();

            if(statement instanceof CExpressionAssignmentStatement){
                CExpressionAssignmentStatement assign = (CExpressionAssignmentStatement)statement;

                CExpression left = assign.getLeftHandSide();
                if(!isPointer(left))return;

                String name = handleExpr(left);
                addNode(name);
                handleAssign(getId(name), assign.getRightHandSide());
            }

        }


    }
}

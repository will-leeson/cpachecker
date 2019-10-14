package org.sosy_lab.cpachecker.intelligence.ast.neural;

import org.eclipse.cdt.internal.core.dom.parser.c.CVariable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.intelligence.ast.IEdgeListener;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer.PEGraph;

import java.util.ArrayList;
import java.util.List;

public class PointerListener implements IEdgeListener {

    private PEGraph graph;
    private ShutdownNotifier notifier;

    public PointerListener(PEGraph graph, ShutdownNotifier notifier) {
        this.graph = graph;
        this.notifier = notifier;
    }

    private List<PointerUse> handleAssign(CType leftType, String leftName, CExpression right){
        List<PointerUse> out = new ArrayList<>();

        boolean assign = false;
        CExpression expression = right;

        String pointerOp = "";
        while(expression instanceof CUnaryExpression && ((CUnaryExpression) expression).getOperator() == CUnaryExpression.UnaryOperator.AMPER){
            pointerOp += "&";
            expression = ((CUnaryExpression) expression).getOperand();
        }

        while(expression instanceof CPointerExpression){
            pointerOp += "*";
            expression = ((CPointerExpression) expression).getOperand();
        }

        if(expression instanceof CIdExpression){
            out.add(new PointerUse(pointerOp, ((CIdExpression) expression).getDeclaration().getQualifiedName()));
            assign = true;
        }else if(expression instanceof CUnaryExpression || expression instanceof CBinaryExpression){
            System.out.println("Unsupported statement for Alias Analysis. Ignore: "+right);
        }

        if(assign){
            pointerOp = "";
            CType type = leftType;
            while (type instanceof CPointerType) {
                pointerOp += "*";
                type = ((CPointerType) type).getType();
            }
            out.add(new PointerUse(pointerOp, leftName, true));
        }
        return out;
    }

    private List<PointerUse> getPointerUse(CFAEdge edge){

        List<PointerUse> out = new ArrayList<>();

        if(edge instanceof CDeclarationEdge){
            CDeclaration decl = ((CDeclarationEdge)edge).getDeclaration();
            if(decl instanceof CVariableDeclaration){

                CVariableDeclaration varDecl = (CVariableDeclaration)decl;
                CInitializer init = varDecl.getInitializer();

                if(init != null && init instanceof CInitializerExpression){
                    out = handleAssign(varDecl.getType(), varDecl.getQualifiedName(), ((CInitializerExpression) init).getExpression());
                }


            }

        }else if(edge instanceof CStatementEdge) {
            CStatementEdge cfaEdge = (CStatementEdge)edge;
            CStatement statement = cfaEdge.getStatement();

            if(statement instanceof CExpressionAssignmentStatement){

                CExpressionAssignmentStatement assignmentStatement = (CExpressionAssignmentStatement)statement;
                CExpression left = assignmentStatement.getLeftHandSide();
                CType leftType = left.getExpressionType();

                while (left instanceof CPointerExpression) {
                    leftType = new CPointerType(false, false, leftType);
                    left = ((CPointerExpression) left).getOperand();
                }

                if(leftType instanceof CPointerType && left instanceof CIdExpression){
                    CIdExpression leftId = (CIdExpression)left;
                    out = handleAssign(leftType, leftId.getDeclaration().getQualifiedName(), assignmentStatement.getRightHandSide());
                }

            }

        }

        return out;
    }

    private String getId(String s){
        return s.replace("*", "P").replace("&", "A").replace("::", "_");
    }

    @Override
    public void listen(CFAEdge edge) {
        List<PointerUse> pointer = getPointerUse(edge);
        if(pointer.isEmpty())return;

        PointerUse def = null;
        for(PointerUse use : pointer){

            String maxOp = use.pointerOp;
            String op = "";

            for(int i = 0; i < maxOp.length(); i++){
                char c = maxOp.charAt(i);

                String source = op + use.varName;
                String target = c + op + use.varName;

                if(c == '&'){
                    String tmp = source;
                    source = target;
                    target = tmp;
                }

                graph.addNode(getId(source), source);
                graph.addNode(getId(target), target);
                graph.addDerefEdge(getId(source), getId(target));
                op = op + c;
            }

            if(use.def)
                def = use;

        }

        String target = def.pointerOp + def.varName;
        graph.addNode(getId(target), target);

        String inTarget = "&" + def.varName;
        graph.addNode(getId(inTarget), inTarget);
        graph.addNode(getId(def.varName), def.varName);
        graph.addDerefEdge(getId(inTarget), getId(def.varName));


        for(PointerUse use: pointer){
            if(use.def)continue;
            String source = use.pointerOp + use.varName;
            graph.addNode(getId(source), source);
            graph.addAssignEdge(getId(source), getId(target));
        }
    }

    private class PointerUse {

        private String pointerOp;
        private String varName;
        private boolean def = false;

        public PointerUse(String pointerOp, String varName) {
            this.pointerOp = pointerOp;
            this.varName = varName;
        }

        public PointerUse(String pointerOp, String varName, boolean isDef) {
            this.pointerOp = pointerOp;
            this.varName = varName;
            this.def = isDef;
        }



    }
}

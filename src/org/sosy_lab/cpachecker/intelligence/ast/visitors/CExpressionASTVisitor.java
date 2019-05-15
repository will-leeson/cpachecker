/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.intelligence.ast.visitors;

import com.google.common.base.Optional;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.ASTCollectorUtils;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;


public class CExpressionASTVisitor implements CExpressionVisitor<String, CPATransferException> {

  private SVGraph graph;
  private int depth;

  public CExpressionASTVisitor(SVGraph pGraph, int pDepth) {
    this.graph = pGraph;
    this.depth = pDepth;
  }


  @Override
  public String visit(CBinaryExpression pIastBinaryExpression)
      throws CPATransferException {

    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = "";
    switch(pIastBinaryExpression.getOperator()) {
      case MULTIPLY:
        label = ASTNodeLabel.MULTIPLY.name();
        break;
      case DIVIDE:
        label = ASTNodeLabel.DIVIDE.name();
        break;
      case PLUS:
        label = ASTNodeLabel.PLUS.name();
        break;
      case MINUS:
        label = ASTNodeLabel.MINUS.name();
        break;
      case EQUALS:
        label = ASTNodeLabel.EQUALS.name();
        break;
      case NOT_EQUALS:
        label = ASTNodeLabel.NOT_EQUALS.name();
        break;
      case LESS_THAN:
        label = ASTNodeLabel.LESS_THAN.name();
        break;
      case GREATER_THAN:
        label = ASTNodeLabel.GREATER_THAN.name();
        break;
      case LESS_EQUAL:
        label = ASTNodeLabel.LESS_EQUAL.name();
        break;
      case GREATER_EQUAL:
        label = ASTNodeLabel.GREATER_EQUAL.name();
        break;
      case BINARY_AND:
        label = ASTNodeLabel.BINARY_AND.name();
        break;
      case BINARY_XOR:
        label = ASTNodeLabel.BINARY_XOR.name();
        break;
      case BINARY_OR:
        label = ASTNodeLabel.BINARY_OR.name();
        break;
      case SHIFT_LEFT:
        label = ASTNodeLabel.SHIFT_LEFT.name();
        break;
      case SHIFT_RIGHT:
        label = ASTNodeLabel.SHIFT_RIGHT.name();
        break;
      case MODULO:
        label = ASTNodeLabel.MODULO.name();
        break;
      default:
        label = "UNKNOWN";
    }

    graph.addNode(id, label);

    if(depth >= 1) {
      String leftExpTree = pIastBinaryExpression.getOperand1().accept(
          new CExpressionASTVisitor(this.graph, depth - 1)
      );
      String rightExpTree = pIastBinaryExpression.getOperand2().accept(
          new CExpressionASTVisitor( this.graph, depth - 1)
      );
      graph.addSEdge(leftExpTree, id);
      graph.addSEdge(rightExpTree, id);
    }

    return id;
  }

  @Override
  public String visit(CCastExpression pIastCastExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.CAST_EXPRESSION.name();

    graph.addNode(id, label);

    String tId = graph.genId("A");
    String tIdO = graph.genId("A");

    if(depth >= 1){
      graph.addNode(tId, ASTNodeLabel.CAST_TYPE.name());
      graph.addSEdge(tId, id);
      graph.addNode(tIdO, ASTNodeLabel.OPERAND.name());
      graph.addSEdge(tIdO, id);
    }

    if(depth >= 2){
      String castTypeTree = pIastCastExpression.getCastType().accept(new CTypeASTVisitor(graph, depth - 2));
      graph.addSEdge(castTypeTree, tId);
      String operandTree = pIastCastExpression.getOperand().accept(new CExpressionASTVisitor(
          this.graph, this.depth - 2
      ));
      graph.addSEdge(operandTree, tIdO);
    }

    return id;
  }

  @Override
  public String visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.CHAR_LITERAL.name();

    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.FLOAT_LITERAL.name();

    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = "";
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    if(value.compareTo(BigInteger.ZERO) == 0) {
      label = ASTNodeLabel.ZERO.name();
    }else if(value.compareTo(BigInteger.ONE) == 0) {
      label = ASTNodeLabel.ONE.name();
    }else if(value.compareTo(new BigInteger("256")) == -1)
      label = ASTNodeLabel.INT_LITERAL_SMALL.name();
    else if(value.compareTo(new BigInteger("1024")) == -1)
      label = ASTNodeLabel.INT_LITERAL_MEDIUM.name();
    else
      label = ASTNodeLabel.INT_LITERAL_LARGE.name();

    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.STRING_LITERAL.name();

    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CTypeIdExpression pIastTypeIdExpression)
      throws CPATransferException {

    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.TYPE_ID.name();

    graph.addNode(id, label);

    if(depth >= 1){
      String typeIdTree = pIastTypeIdExpression.getType()
          .accept(new CTypeASTVisitor(graph, depth - 1));
      graph.addSEdge(typeIdTree, id);
    }

    return id;
  }

  @Override
  public String visit(CUnaryExpression pIastUnaryExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = "";
    switch(pIastUnaryExpression.getOperator()) {
      case MINUS:
        label = ASTNodeLabel.MINUS.name();
        break;
      case AMPER:
        label = ASTNodeLabel.AMPER.name();
        break;
      case TILDE:
        label = ASTNodeLabel.TILDE.name();
        break;
      case SIZEOF:
        label = ASTNodeLabel.SIZEOF.name();
        break;
      case ALIGNOF:
        label = ASTNodeLabel.ALIGNOF.name();
        break;
      default:
        label = "Unknown";
    }
    graph.addNode(id, label);

    if(depth >= 1) {
      String operandTree = pIastUnaryExpression.getOperand().accept(new CExpressionASTVisitor(
           this.graph, this.depth -1));
      graph.addSEdge(operandTree, id);
    }
    return id;
  }

  @Override
  public String visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.COMPLEX_LITERAL.name();

    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.LABEL_ADDRESS.name();

    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws CPATransferException {

    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.ARRAY_SUBSCRIPT_EXPRESSION.name();

    graph.addNode(id, label);

    String tIdA = graph.genId("A");
    String tIdS = graph.genId("A");


    if(depth >= 1){
      graph.addNode(tIdA, ASTNodeLabel.ARRAY_EXPRESSION.name());
      graph.addSEdge(tIdA, id);
      graph.addNode(tIdS, ASTNodeLabel.SUBSCRIPT_EXPRESSION.name());
      graph.addSEdge(tIdS, id);
    }

    if(depth >= 2) {
      String arrayExp = pIastArraySubscriptExpression.getArrayExpression().accept(new CExpressionASTVisitor(
          this.graph, this.depth - 1
      ));
      graph.addSEdge(arrayExp, tIdA);
      String subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression().accept(new CExpressionASTVisitor(
          this.graph, this.depth - 1
      ));
      graph.addSEdge(subscriptExp, tIdS);
    }
    return id;
  }

  @Override
  public String visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = "";
    if(pIastFieldReference.isPointerDereference())
      label = ASTNodeLabel.FIELD_POINTER_DEREF.name();
    else
      label = ASTNodeLabel.FIELD_REF.name();

    graph.addNode(id, label);

    if(depth >= 1) {
      String ownerTree = pIastFieldReference.getFieldOwner().accept(new CExpressionASTVisitor(
          this.graph, this.depth - 1
      ));
      graph.addSEdge(ownerTree, id);
    }

    return id;
  }

  @Override
  public String visit(CIdExpression pIastIdExpression)
      throws CPATransferException {

    if(depth <= 0)return "";

    if(graph.getGlobalOption(OptionKeys.REPLACE_ID) != null && graph.getGlobalOption(OptionKeys.REPLACE_ID)){
      CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
      return declaration.accept(new CSimpleDeclTypeASTVisitor(graph, depth));
    }

    String id = graph.genId("A");
    String label = "";

    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(pIastIdExpression.getName());
    if(specialLabel.isPresent())
      label = specialLabel.get().name();
    else
      label = ASTNodeLabel.ID.name();

    graph.addNode(id, label);

    return id;
  }

  @Override
  public String visit(CPointerExpression pointerExpression)
      throws CPATransferException {

    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.POINTER_EXPRESSION.name();
    graph.addNode(id, label);

    if(depth >= 1) {
      String ownerTree = pointerExpression.getOperand().accept(new CExpressionASTVisitor(
          this.graph, this.depth - 1
      ));
      graph.addSEdge(ownerTree, id);
    }

    return id;
  }

  @Override
  public String visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    return "";
  }
}

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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.intelligence.ast.ASTCollectorUtils;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;


public class CStatementASTVisitor implements CStatementVisitor<String, CPATransferException> {


  private final StructureGraph graph;
  private final int depth;

  public CStatementASTVisitor(StructureGraph pGraph, int pDepth) {
    graph = pGraph;
    depth = pDepth;
  }

  @Override
  public String visit(CExpressionStatement pIastExpressionStatement)
      throws CPATransferException {
    return pIastExpressionStatement.getExpression().accept(
        new CExpressionASTVisitor(this.graph, depth));
  }

  @Override
  public String visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.ASSIGNMENT.name();
    graph.addNode(id, label);

    if(depth >= 1) {
      String leftTree = pIastExpressionAssignmentStatement.getLeftHandSide().accept(
          new CExpressionASTVisitor(graph, depth - 1));
      graph.addSEdge(leftTree, id);
      String rightTree = pIastExpressionAssignmentStatement.getRightHandSide().accept(
          new CExpressionASTVisitor(graph, depth - 1));
      graph.addSEdge(rightTree, id);
    }
    return id;
  }

  @Override
  public String visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws CPATransferException {
    if(depth <= 0)return "";

    String calledFunctionName = pIastFunctionCallAssignmentStatement.getFunctionCallExpression()
        .getFunctionNameExpression().toString();

    String id = graph.genId("A");
    String label = ASTNodeLabel.FUNC_CALL.name();

    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(calledFunctionName);
    if(specialLabel.isPresent())
      label = label + "_" + specialLabel.get().name();

    graph.addNode(id, label);

    if(depth >= 1) {
      String leftTree = pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(
          new CExpressionASTVisitor(graph, depth - 1));
      graph.addSEdge(leftTree, id);
      if (pIastFunctionCallAssignmentStatement.getRightHandSide().getParameterExpressions().size()
          > 0) {
        String tId = graph.genId("A");
        graph.addNode(tId, ASTNodeLabel.PARAMS.name());
        graph.addSEdge(tId, id);

        if(depth >= 2) {
          for (CExpression paramExp : pIastFunctionCallAssignmentStatement.getRightHandSide()
              .getParameterExpressions()) {
            String paramExpTree = paramExp.accept(new CExpressionASTVisitor(graph, depth - 2));
            graph.addSEdge(paramExpTree, tId);
          }
        }
      }
    }
    return id;
  }

  @Override
  public String visit(CFunctionCallStatement pIastFunctionCallStatement)
      throws CPATransferException {
    if(depth <= 0)return "";

    String calledFunctionName = pIastFunctionCallStatement.getFunctionCallExpression()
        .getFunctionNameExpression().toString();

    String id = graph.genId("A");
    String label = ASTNodeLabel.FUNC_CALL.name();

    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(calledFunctionName);
    if(specialLabel.isPresent())
      label = label + "_" + specialLabel.get().name();

    graph.addNode(id, label);

    // add labels for arguments as well
    if(pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions().size() > 0) {
      String tId = graph.genId("A");

      if(depth >= 1) {
        graph.addNode(tId, ASTNodeLabel.PARAMS.name());
        graph.addSEdge(tId, id);
      }

      if(depth >= 2) {
        for (CExpression paramExp : pIastFunctionCallStatement.getFunctionCallExpression()
            .getParameterExpressions()) {
          String paramExpTree = paramExp.accept(new CExpressionASTVisitor(graph, depth - 2));
          graph.addSEdge(paramExpTree, tId);
        }
      }
    }
    return id;
  }
}
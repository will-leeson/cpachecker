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

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class CStatementVariablesCollectingVisitor implements CStatementVisitor<Set<String>, CPATransferException> {

  private CFANode predecessor;

  public CStatementVariablesCollectingVisitor(CFANode pre) {
    this.predecessor = pre;
  }

  @Override
  public Set<String> visit(CExpressionStatement pIastExpressionStatement)
      throws CPATransferException {
    return pIastExpressionStatement.getExpression().accept(
        new CVariablesCollectingVisitor(this.predecessor));
  }

  @Override
  public Set<String> visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws CPATransferException {
    return pIastExpressionAssignmentStatement.getRightHandSide().accept(
        new CVariablesCollectingVisitor(this.predecessor));
  }

  @Override
  public Set<String> visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws CPATransferException {
    Set<String> vars = new HashSet<>();
    for(CExpression exp : pIastFunctionCallAssignmentStatement.getFunctionCallExpression().getParameterExpressions()) {
      vars.addAll(exp.accept(new CVariablesCollectingVisitor(this.predecessor)));
    }
    return vars;
  }

  @Override
  public Set<String> visit(CFunctionCallStatement pIastFunctionCallStatement)
      throws CPATransferException {
    Set<String> vars = new HashSet<>();
    for(CExpression exp : pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
      vars.addAll(exp.accept(new CVariablesCollectingVisitor(this.predecessor)));
    }
    return vars;
  }
}

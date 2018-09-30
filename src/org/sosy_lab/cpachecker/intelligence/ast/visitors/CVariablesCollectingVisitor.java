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

import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;

import com.google.common.collect.Sets;

public class CVariablesCollectingVisitor implements
                                         CExpressionVisitor<Set<String>, RuntimeException> {

  private CFANode predecessor;

  private static final String SCOPE_SEPARATOR = "::";

  public CVariablesCollectingVisitor(CFANode pre) {
    this.predecessor = pre;
  }

  private static String scopeVar(@Nullable final String function, final String var) {
    return (function == null) ? (var) : (function + SCOPE_SEPARATOR + var);
  }

  private static boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  @Override
  public Set<String> visit(CArraySubscriptExpression exp) {
    return Sets.newHashSet();
  }

  @Override
  public Set<String> visit(CBinaryExpression exp) {

    Set<String> operand1 = exp.getOperand1().accept(this);
    Set<String> operand2 = exp.getOperand2().accept(this);

    // handle vars from operands
    if (operand1 == null) {
      return operand2;
    } else if (operand2 == null) {
      return operand1;
    } else {
      operand1.addAll(operand2);
      return operand1;
    }
  }

  @Override
  public Set<String> visit(CCastExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CComplexCastExpression exp) {
    // TODO complex numbers are not supported for evaluation right now, this
    // way of handling the variables my be wrong
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CFieldReference exp) {
    String varName = exp.toASTString(); // TODO "(*p).x" vs "p->x"
    String function = isGlobal(exp) ? "" : predecessor.getFunctionName();
    Set<String> ret = Sets.newHashSetWithExpectedSize(1);
    ret.add(scopeVar(function, varName));
    return ret;
  }

  @Override
  public Set<String> visit(CIdExpression exp) {
    Set<String> ret = Sets.newHashSetWithExpectedSize(1);
    ret.add(exp.getDeclaration().getQualifiedName());
    return ret;
  }

  @Override
  public Set<String> visit(CCharLiteralExpression exp) {
    return Sets.newHashSet();
  }

  @Override
  public Set<String> visit(CFloatLiteralExpression exp) {
    return Sets.newHashSet();
  }

  @Override
  public Set<String> visit(CImaginaryLiteralExpression exp) {
    return exp.getValue().accept(this);
  }

  @Override
  public Set<String> visit(CIntegerLiteralExpression exp) {
    return Sets.newHashSet();
  }

  @Override
  public Set<String> visit(CStringLiteralExpression exp) {
    return Sets.newHashSet();
  }

  @Override
  public Set<String> visit(CTypeIdExpression exp) {
    return Sets.newHashSet();
  }

  @Override
  public Set<String> visit(CUnaryExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CPointerExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CAddressOfLabelExpression exp) {
    return Sets.newHashSet();
  }
}

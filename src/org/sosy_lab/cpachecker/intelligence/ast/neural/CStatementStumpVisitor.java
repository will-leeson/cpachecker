/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.intelligence.ast.neural;

import com.google.common.base.Optional;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.ASTCollectorUtils;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;


public class CStatementStumpVisitor implements CStatementVisitor<String, CPATransferException> {

  @Override
  public String visit(CExpressionStatement pIastExpressionStatement)
      throws CPATransferException {
    return pIastExpressionStatement.getExpression().accept(
        new CExpressionVisitor<String, CPATransferException>() {
          @Override
          public String visit(CBinaryExpression pIastBinaryExpression) throws CPATransferException {
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
            return label;
          }

          @Override
          public String visit(CCastExpression pIastCastExpression) throws CPATransferException {
            return ASTNodeLabel.CAST_EXPRESSION.name();
          }

          @Override
          public String visit(CCharLiteralExpression pIastCharLiteralExpression) throws CPATransferException {
            return ASTNodeLabel.CHAR_LITERAL.name();
          }

          @Override
          public String visit(CFloatLiteralExpression pIastFloatLiteralExpression)
              throws CPATransferException {
            return ASTNodeLabel.FLOAT_LITERAL.name();
          }

          @Override
          public String visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
              throws CPATransferException{
            String label = "";
            BigInteger value = pIastIntegerLiteralExpression.getValue();
            if(value.compareTo(new BigInteger("256")) == -1)
              label = ASTNodeLabel.INT_LITERAL_SMALL.name();
            else if(value.compareTo(new BigInteger("1024")) == -1)
              label = ASTNodeLabel.INT_LITERAL_MEDIUM.name();
            else
              label = ASTNodeLabel.INT_LITERAL_LARGE.name();

            return label;
          }

          @Override
          public String visit(CStringLiteralExpression pIastStringLiteralExpression)
              throws CPATransferException {
            return ASTNodeLabel.STRING_LITERAL.name();
          }

          @Override
          public String visit(CTypeIdExpression pIastTypeIdExpression) throws CPATransferException {
            return ASTNodeLabel.TYPE_ID.name();
          }

          @Override
          public String visit(CUnaryExpression pIastUnaryExpression) throws CPATransferException {
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
            return label;
          }

          @Override
          public String visit(CImaginaryLiteralExpression PIastLiteralExpression) throws CPATransferException {
            return ASTNodeLabel.COMPLEX_LITERAL.name();
          }

          @Override
          public String visit(CAddressOfLabelExpression pAddressOfLabelExpression)
              throws CPATransferException {
            return ASTNodeLabel.LABEL_ADDRESS.name();
          }

          @Override
          public String visit(CArraySubscriptExpression pIastArraySubscriptExpression)
              throws CPATransferException {
            return ASTNodeLabel.ARRAY_SUBSCRIPT_EXPRESSION.name();
          }

          @Override
          public String visit(CFieldReference pIastFieldReference) throws CPATransferException {
            String label = "";
            if(pIastFieldReference.isPointerDereference())
              label = ASTNodeLabel.FIELD_POINTER_DEREF.name();
            else
              label = ASTNodeLabel.FIELD_REF.name();
            return label;
          }

          @Override
          public String visit(CIdExpression pIastIdExpression) throws CPATransferException {
            String label = "";

            Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(pIastIdExpression.getName());
            if(specialLabel.isPresent())
              label = specialLabel.get().name();
            else
              label = ASTNodeLabel.ID.name();

            return label;
          }

          @Override
          public String visit(CPointerExpression pointerExpression) throws CPATransferException {
            return ASTNodeLabel.POINTER_EXPRESSION.name();
          }

          @Override
          public String visit(CComplexCastExpression complexCastExpression) throws CPATransferException {
            return "";
          }
        });
  }

  @Override
  public String visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws CPATransferException {
    return ASTNodeLabel.ASSIGNMENT.name();
  }

  @Override
  public String visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws CPATransferException {

    String calledFunctionName = pIastFunctionCallAssignmentStatement.getFunctionCallExpression()
        .getFunctionNameExpression().toString();

    String label = ASTNodeLabel.FUNC_CALL.name();

    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(calledFunctionName);
    if(specialLabel.isPresent())
      label = label + "_" + specialLabel.get().name();

    return label;
  }

  @Override
  public String visit(CFunctionCallStatement pIastFunctionCallStatement)
      throws CPATransferException {

    String calledFunctionName = pIastFunctionCallStatement.getFunctionCallExpression()
        .getFunctionNameExpression().toString();

    String label = ASTNodeLabel.FUNC_CALL.name();

    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(calledFunctionName);
    if(specialLabel.isPresent())
      label = label + "_" + specialLabel.get().name();

    return label;
  }
}
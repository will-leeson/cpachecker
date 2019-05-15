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

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;

public class CTypeStumpVisitor implements CTypeVisitor<String, CPATransferException> {
  @Override
  public String visit(CArrayType pArrayType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CCompositeType pCompositeType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CElaboratedType pElaboratedType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CEnumType pEnumType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CFunctionType pFunctionType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CPointerType pPointerType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CProblemType pProblemType) throws CPATransferException {
    return ASTNodeLabel.PROBLEM_TYPE.name();
  }

  @Override
  public String visit(CSimpleType pSimpleType) throws CPATransferException {
    List<String> labels = new ArrayList<>();

    if (pSimpleType.isConst())
      labels.add(ASTNodeLabel.CONST.name());
    if (pSimpleType.isVolatile())
      labels.add(ASTNodeLabel.VOLATILE.name());
    if (pSimpleType.isLong())
      labels.add(ASTNodeLabel.LONG.name());
    if (pSimpleType.isLongLong())
      labels.add(ASTNodeLabel.LONGLONG.name());
    if (pSimpleType.isUnsigned())
      labels.add(ASTNodeLabel.UNSIGNED.name());

    switch (pSimpleType.getType()) {
      case BOOL:
        labels.add(ASTNodeLabel.BOOL.name());
        break;
      case CHAR:
        labels.add(ASTNodeLabel.CHAR.name());
        break;
      case INT:
        labels.add(ASTNodeLabel.INT.name());
        break;
      case FLOAT:
        labels.add(ASTNodeLabel.FLOAT.name());
        break;
      case DOUBLE:
        labels.add(ASTNodeLabel.DOUBLE.name());
        break;
      default:
        if (pSimpleType.isShort()) {
          labels.add(ASTNodeLabel.SHORT.name());
          break;
        }
    }

    return String.join("_", labels);
  }

  @Override
  public String visit(CTypedefType pTypedefType) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CVoidType pVoidType) throws CPATransferException {
    String label = ASTNodeLabel.VOID_TYPE.name();

    if (pVoidType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pVoidType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;

    return label;
  }

  @Override
  public String visit(CBitFieldType pCBitFieldType) throws CPATransferException {
    return "";
  }
}

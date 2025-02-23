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

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CDeclarationDefCollectorVisitor implements
                                             CSimpleDeclarationVisitor<String, CPATransferException> {
  @Override
  public String visit(CFunctionDeclaration pDecl) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CComplexTypeDeclaration pDecl) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CTypeDefDeclaration pDecl) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CVariableDeclaration pDecl) throws CPATransferException {
    return pDecl.getQualifiedName();
  }

  @Override
  public String visit(CParameterDeclaration pDecl) throws CPATransferException {
    return null;
  }

  @Override
  public String visit(CEnumerator pDecl) throws CPATransferException {
    return null;
  }
}

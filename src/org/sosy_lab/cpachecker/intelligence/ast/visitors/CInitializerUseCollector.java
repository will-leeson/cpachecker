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
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CInitializerUseCollector implements
                                      CInitializerVisitor<Set<String>, CPATransferException> {
  private CFANode predecessor;

  public CInitializerUseCollector(CFANode pPredecessor) {
    predecessor = pPredecessor;
  }


  @Override
  public Set<String> visit(CInitializerExpression pInitializerExpression)
      throws CPATransferException {
    return pInitializerExpression.getExpression().accept(new CVariablesCollectingVisitor(
        predecessor
    ));
  }

  @Override
  public Set<String> visit(CInitializerList pInitializerList) throws CPATransferException {
    Set<String> vars = new HashSet<>();

    for (CInitializer initializer : pInitializerList.getInitializers()) {
        vars.addAll(initializer.accept(this));
    }

    return vars;
  }

  @Override
  public Set<String> visit(CDesignatedInitializer pCStructInitializerPart)
      throws CPATransferException {
    return new HashSet<>();
  }
}

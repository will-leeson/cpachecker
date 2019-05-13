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


import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CDeclarationDefCollectorVisitor;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CDeclarationUseCollectorVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class DeclarationListener extends AEdgeListener {

  public DeclarationListener(
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(-1, pGraph, pShutdownNotifier);
  }

  @Override
  public void listen(CFAEdge edge) {
    if (edge instanceof CDeclarationEdge) {
      CDeclarationEdge declEdge = (CDeclarationEdge) edge;
      CDeclaration decl = declEdge.getDeclaration();

      String label = ASTNodeLabel.DECL.name();
      if (decl.isGlobal()) {
        label = label + "_" + ASTNodeLabel.GLOBAL.name();
      }

      String id = "N" + declEdge.getPredecessor().getNodeNumber();
      String idS = "N" + declEdge.getSuccessor().getNodeNumber();
      graph.addNode(id, label);
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);

      if (notifier != null)
        try {
          notifier.shutdownIfNecessary();
        } catch (InterruptedException pE) {
          return;
        }

      try {
        GNode sourceNode = graph.getNode(id);

        sourceNode.setOption(OptionKeys.CDECL, decl);

        Set<String> vars = decl.accept(new CDeclarationUseCollectorVisitor(edge.getPredecessor()));
        if (vars != null && !vars.isEmpty())
          sourceNode.setOption(OptionKeys.VARS, vars);

        if (notifier != null)
          try {
            notifier.shutdownIfNecessary();
          } catch (InterruptedException pE) {
            return;
          }

        String declVar = decl.accept(new CDeclarationDefCollectorVisitor());
        if (declVar != null) {
          Set<String> declVars = new HashSet<>();
          declVars.add(declVar);
          sourceNode.setOption(OptionKeys.DECL_VARS, declVars);
        }

      } catch (CPATransferException pE) {
      }


    }
  }


}

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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CAssignVariablesCollector;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CStatementVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class ReturnStatementListener extends AEdgeListener {

  public ReturnStatementListener(
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(-1, pGraph, pShutdownNotifier);
  }

  public static String extractLabel(CFAEdge pCFAEdge){
    if(!(pCFAEdge instanceof CReturnStatementEdge)) return null;

    return ASTNodeLabel.RETURN.name();
  }

  @Override
  public void listen(CFAEdge edge) {
    if(edge instanceof CReturnStatementEdge){
      String id = "N"+edge.getPredecessor().getNodeNumber();
      String idS = "N"+edge.getSuccessor().getNodeNumber();
      graph.addNode(id, ASTNodeLabel.RETURN.name());
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);

      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)edge;

      if(returnStatementEdge.asAssignment().isPresent()){

        GNode node = graph.getNode(id);
        CAssignment statement = returnStatementEdge.asAssignment().get();

        try {
          node.setOption(OptionKeys.VARS,
              statement.accept(new CStatementVariablesCollectingVisitor(edge.getPredecessor())));

          node.setOption(OptionKeys.DECL_VARS,
              statement.accept(new CAssignVariablesCollector(edge.getPredecessor())));
        } catch (CPATransferException pE) {
          return;
        }


      }

    }
  }
}

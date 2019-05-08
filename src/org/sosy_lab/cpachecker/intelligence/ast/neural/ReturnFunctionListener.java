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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CAssignVariablesCollector;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CStatementVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.SVGraph;

public class ReturnFunctionListener extends AEdgeListener {

  public ReturnFunctionListener(
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(-1, pGraph, pShutdownNotifier);
  }

  @Override
  public void listen(CFAEdge edge) {
    if(edge instanceof CFunctionReturnEdge) {
      String label = ASTNodeLabel.FUNCTION_EXIT.name();
      String id = "N"+edge.getPredecessor().getNodeNumber();
      String idS = "N"+edge.getSuccessor().getNodeNumber();
      graph.addNode(id, label);
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);

      GNode prev = graph.getNode(id);
      if(!prev.containsOption(OptionKeys.VARS)){
        prev.setOption(OptionKeys.VARS, new HashSet<>());
      }

      CFANode pre = edge.getPredecessor();
      for(int i = 0; i < pre.getNumEnteringEdges(); i++){
        String pId = "N"+pre.getEnteringEdge(i).getPredecessor().getNodeNumber();
        GNode pNode = graph.getNode(pId);

        if(pNode == null)continue;

        if(pNode.containsOption(OptionKeys.DECL_VARS)) {
          prev.getOption(OptionKeys.VARS).addAll(
              pNode.getOption(OptionKeys.DECL_VARS)
          );
        }
      }

    }
  }
}

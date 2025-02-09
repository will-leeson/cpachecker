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
package org.sosy_lab.cpachecker.intelligence.ast.base;

import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CExpressionASTVisitor;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class AssumptionListener extends AEdgeListener {


  public AssumptionListener(
      int pDepth,
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(pDepth, pGraph, pShutdownNotifier);
  }

  public static ASTNodeLabel extractControlLabel(CFAEdge pCFAEdge) {
    if(pCFAEdge.getPredecessor().isLoopStart()) {
      return ASTNodeLabel.LOOP_CONDITION;
    }
    return ASTNodeLabel.BRANCH_CONDITION;
  }

  @Override
  public void listen(CFAEdge edge) {
    if(edge instanceof CAssumeEdge){
      CAssumeEdge assume = (CAssumeEdge)edge;

      String label = extractControlLabel(assume).name();
      String id = "N"+assume.getPredecessor().getNodeNumber();
      String idS = "N"+assume.getSuccessor().getNodeNumber();
      graph.addNode(id, label);
      GNode sourceNode = graph.getNode(id);
      sourceNode.setOption(OptionKeys.TRUTH, assume.getTruthAssumption());
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);

      if(notifier != null)
        try{
          notifier.shutdownIfNecessary();
        }catch (InterruptedException pE){
          return;
        }

      Set<String> vars = assume.getExpression().accept(new CVariablesCollectingVisitor(assume.getPredecessor()));
      sourceNode.setOption(OptionKeys.VARS, vars);

      if(assume.getTruthAssumption()) {
        if (depth >= 1) {

          if(notifier != null)
            try{
              notifier.shutdownIfNecessary();
            }catch (InterruptedException pE){
              return;
            }

          try {
            String assumeExpTree = assume.getExpression().accept(
                new CExpressionASTVisitor(graph, depth - 1)
            );
            graph.addSEdge(assumeExpTree, id);
          } catch (CPATransferException pE) {
            return;
          }
        }
      }



    }
  }
}

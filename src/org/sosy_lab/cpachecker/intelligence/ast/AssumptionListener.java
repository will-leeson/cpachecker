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
package org.sosy_lab.cpachecker.intelligence.ast;

import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CExpressionASTVisitor;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

public class AssumptionListener extends AEdgeListener {
  public AssumptionListener(
      int pDepth,
      StructureGraph pGraph) {
    super(pDepth, pGraph);
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
      //if(assume.getTruthAssumption()){

      String label = extractControlLabel(assume).name();
      String id = "N"+assume.getPredecessor().getNodeNumber();
      String idS = "N"+assume.getSuccessor().getNodeNumber();
      graph.addNode(id, label);
      Map<String, Object> options = graph.getNode(id).getOptions();
      options.put("truth", assume.getTruthAssumption());
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);

      Set<String> vars = assume.getExpression().accept(new CVariablesCollectingVisitor(assume.getPredecessor()));
      options.put("variables", vars);

      if(depth >= 1){
        try {
          String assumeExpTree = assume.getExpression().accept(
              new CExpressionASTVisitor(graph, depth - 1)
          );
          graph.addSEdge(assumeExpTree, id);
        } catch (CPATransferException pE) {
          pE.printStackTrace();
        }
      }


      //}
    }
  }
}

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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.SVGraph;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

public class ReturnStatementListener extends AEdgeListener {

  public ReturnStatementListener(
      int pDepth,
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(pDepth, pGraph, pShutdownNotifier);
  }

  @Override
  public void listen(CFAEdge edge) {
    if(edge instanceof CReturnStatementEdge){
      String id = "N"+edge.getPredecessor().getNodeNumber();
      String idS = "N"+edge.getSuccessor().getNodeNumber();
      graph.addNode(id, ASTNodeLabel.RETURN.name());
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);
    }
  }
}

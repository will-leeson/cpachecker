// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning;

import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.CFAPathIterator;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.IAnalysisListener;
import org.sosy_lab.cpachecker.intelligence.ast.IEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.AssumptionListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.BlankEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.DeclarationListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.FunctionCallListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.ReturnFunctionListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.ReturnStatementListener;
import org.sosy_lab.cpachecker.intelligence.ast.neural.StatementListener;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class SVPathProcessor {

  private List<IEdgeListener> createListeners(SVGraph pGraph, ShutdownNotifier pShutdownNotifier){
    return List.of(
        new AssumptionListener(pGraph, pShutdownNotifier),
        new BlankEdgeListener(pGraph, pShutdownNotifier),
        new DeclarationListener(pGraph, pShutdownNotifier),
        new FunctionCallListener(pGraph, pShutdownNotifier),
        new ReturnFunctionListener(pGraph, pShutdownNotifier),
        new ReturnStatementListener(pGraph, pShutdownNotifier),
        new StatementListener(pGraph, pShutdownNotifier)
    );
  }


  public PathGraph process(List<CFAEdge> pCFAPath){

    PathGraph pathGraph = new PathGraph();

    if(pCFAPath.isEmpty())return pathGraph;

    List<IEdgeListener> listeners = createListeners(pathGraph, null);

    int edgeNumber = 0;
    String lastID = "N" +pCFAPath.get(0).getPredecessor().getNodeNumber();

    for(CFAEdge currentEdge : pCFAPath){

      for(IEdgeListener listener: listeners){
        listener.listen(currentEdge);
      }

      String nextID = "N"+currentEdge.getSuccessor().getNodeNumber();
      pathGraph.addPathEdge(lastID, nextID, edgeNumber);

      lastID = nextID;
      edgeNumber++;
    }

    return pathGraph;
  }

}

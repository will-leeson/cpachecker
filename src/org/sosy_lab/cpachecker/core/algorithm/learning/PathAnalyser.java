// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.IAnalysisListener;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class PathAnalyser {

  private List<IAnalysisListener> listeners = new ArrayList<>();

  public void attachListener(IAnalysisListener pIAnalysisListener){
    listeners.add(pIAnalysisListener);
  }

  private void clearState(){
    for(IAnalysisListener listener : listeners){
      listener.reset();
    }
  }

  private void handleEdge(GEdge pGEdge){
    GNode source = pGEdge.getSource();

    if(source.containsOption(OptionKeys.PROCESSED))return;

    for(IAnalysisListener listener : listeners){
      listener.listen(pGEdge);
    }

    source.setOption(OptionKeys.PROCESSED, true);
  }

  public void analyse(PathGraph pPathGraph, String targetId){

    if(pPathGraph.isEmpty())return;

    GNode currentNode = pPathGraph.getNode(targetId);
    int edgeID = pPathGraph.edgeStream().filter(e -> e instanceof PathEdge)
                  .mapToInt(e -> ((PathEdge) e).getEdgeID())
                  .max().orElseThrow(NoSuchElementException::new);

    while (currentNode != null){
      Optional<PathEdge> ingoing = pPathGraph.getIngoingPathEdge(currentNode.getId(), edgeID);

      if(!ingoing.isPresent())break;

      handleEdge(ingoing.get());

      currentNode = ingoing.get().getSource();
      edgeID--;
    }

    clearState();
  }


}

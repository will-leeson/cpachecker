// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class PathGraph extends SVGraph {

  public void addPathEdge(String sourceId, String sinkId, int edgeID){
    super.addEdge(new PathEdge(edgeID, super.getNode(sourceId), super.getNode(sinkId)));
  }

  public Optional<PathEdge> getIngoingPathEdge(String sinkId, int edgeID){
    Set<GEdge> edges = super.getIngoingTyped(sinkId, "path_"+edgeID);
    if(edges.size() == 0)
      return Optional.empty();

    for(GEdge edge: edges){
      if(!(edge instanceof PathEdge))continue;
      PathEdge pathEdge = (PathEdge)edge;

      if(pathEdge.getEdgeID() == edgeID)
        return Optional.of(pathEdge);

    }

    return Optional.empty();

  }

  @Override
  public String toDot(){
    return super.toDot(
        n -> n.getId().startsWith("N"),
        e -> {
          if(e instanceof PathEdge)
            return "black";

          return "opaque";
        },
        n -> {

          if(n.getOption(OptionKeys.PARENT_FUNC) != null){
            return n.getOption(OptionKeys.PARENT_FUNC);
          }

          return "main";
        }
    );
  }

}

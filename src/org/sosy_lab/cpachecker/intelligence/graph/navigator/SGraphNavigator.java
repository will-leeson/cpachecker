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
package org.sosy_lab.cpachecker.intelligence.graph.navigator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.intelligence.graph.CFGEdge;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;
import org.sosy_lab.cpachecker.intelligence.graph.navigator.IGraphNavigator;

public class SGraphNavigator implements IGraphNavigator {


  private StructureGraph graph;

  public SGraphNavigator(StructureGraph pGraph) {
    graph = pGraph;
  }

  @Override
  public Set<String> successor(String node) {
    if(graph.getNode(node) == null)return new HashSet<>();
    return graph.getOutgoingStream(node).filter(edge -> edge instanceof CFGEdge)
              .map(edge -> edge.getSink().getId()).collect(Collectors.toSet());
  }

  @Override
  public Set<String> predecessor(String node) {
    if(graph.getNode(node) == null)return new HashSet<>();
    return graph.getIngoingStream(node).filter(edge -> edge instanceof CFGEdge)
        .map(edge -> edge.getSource().getId()).collect(Collectors.toSet());
  }

  @Override
  public Set<String> nodes() {
    Set<String> out = new HashSet<>();

    for(String node: graph.nodes())
      if(node.startsWith("N"))
        out.add(node);

    return out;
  }
}

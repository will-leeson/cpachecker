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
package org.sosy_lab.cpachecker.intelligence.graph.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;

public class SCCUtil {

  private StructureGraph graph;

  private ArrayDeque<SCCNode> stack;
  private Map<String, SCCNode> nodeMap = new HashMap<>();
  private int pubIndex = 0;

  private List<SCC> sccs;

  public SCCUtil(StructureGraph pGraph) {
    graph = pGraph;
  }

  private SCCNode getNode(String id){
    if(!nodeMap.containsKey(id)){
      nodeMap.put(id, new SCCNode(id));
    }
    return nodeMap.get(id);
  }

  public List<SCC> getStronglyConnectedComponents(){
    if(sccs != null)return sccs;

    sccs = new ArrayList<>();
    stack = new ArrayDeque<>();


    for(String vId: graph.nodes()) {
      SCCNode node = getNode(vId);
      if(node.index == -1) {
        strongconnected(node);
      }
    }

    return sccs;
  }

  private void strongconnected(SCCNode pSCCNode){

    ArrayDeque<SCCNode> strong = new ArrayDeque<>();
    strong.push(pSCCNode);

    while (!strong.isEmpty()) {
      SCCNode sccNode = strong.peekFirst();

      if(sccNode.index == -1) {
        sccNode.index = pubIndex;
        sccNode.lowlink = pubIndex;
        pubIndex++;
        stack.addFirst(sccNode);
        sccNode.onStack = true;
        sccNode.open = graph.getOutgoing(sccNode.nodeId).iterator();
      }

      while (sccNode.open.hasNext()){
          SCCNode w = getNode(sccNode.open.next().getSink().getId());
          if (w.index == -1){
            sccNode.waitOn = w.nodeId;
            strong.addFirst(w);
            break;
          }else if(w.onStack){
            sccNode.lowlink = Math.min(sccNode.lowlink, w.index);
          }
      }

      if(!strong.peekFirst().equals(sccNode))
        continue;

      //Finish node
      strong.pop();

      if(!strong.isEmpty() && sccNode.nodeId.equals(strong.peek().waitOn)){
        strong.peekFirst().lowlink = Math.min(strong.peek().lowlink, sccNode.lowlink);
        strong.peekFirst().waitOn = null;
      }

      if(sccNode.lowlink == sccNode.index){
        sccs.add(new SCC());
        while (!stack.isEmpty()){
          SCCNode w = stack.removeFirst();
          w.onStack = false;
          sccs.get(sccs.size() - 1).nodes.add(w.nodeId);
          if(w.equals(sccNode))break;
        }
      }


    }


  }

  private static class SCCNode{

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      SCCNode sccNode = (SCCNode) pO;
      return Objects.equals(nodeId, sccNode.nodeId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodeId);
    }

    String nodeId;
    int lowlink;
    int index = -1;
    boolean onStack = false;
    String waitOn;
    Iterator<GEdge> open;

    public SCCNode(String pNodeId) {
      nodeId = pNodeId;
    }


  }

  public static class SCC{

    public Set<String> getNodes() {
      return nodes;
    }

    @Override
    public String toString() {
      return "SCC{" +
          "nodes=" + nodes +
          '}';
    }

    private Set<String> nodes = new HashSet<>();

  }

}

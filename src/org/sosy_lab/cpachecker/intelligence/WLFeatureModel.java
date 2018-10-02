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
package org.sosy_lab.cpachecker.intelligence;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.ast.CFAProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

public class WLFeatureModel {

    private CFA cfa;
    private int astDepth;

    private StructureGraph graph;
    private Map<String, String> relabel = new HashMap<>();
    private int iteration = -1;

    public WLFeatureModel(CFA pCFA, int pAstDepth){
      cfa = pCFA;
      astDepth = pAstDepth;
    }

    private Map<String, Integer> iteration0(){
      graph = getGraph();

      Map<String, Integer> count = new HashMap<>();

      for(String n: graph.nodes()){
        String label = graph.getNode(n).getLabel();
        if(!count.containsKey(label))
          count.put(label, 0);
        count.put(label, count.get(label) + 1);
      }

      return count;
    }

    public StructureGraph getGraph() {
      if(graph == null){
        graph = new CFAProcessor().process(cfa, astDepth);
      }
      return graph;
    }


    private String relabelNode(String nodeId, Map<String, String> relabelIndex){
      String label = graph.getNode(nodeId).getLabel();
      if(relabelIndex.containsKey(nodeId))
        label = relabelIndex.get(nodeId);

      HashFunction function = Hashing.murmur3_32(42);

      List<String> neighbours = new ArrayList<>();

      for(GEdge e: graph.getIngoing(nodeId)){
        GNode node = graph.getNode(e.getSource().getId());
        String neigh_label = node.getLabel();
        if(relabelIndex.containsKey(e.getSource().getId()))
          neigh_label = relabelIndex.get(e.getSource().getId());
        String edge_type = e.getId();
        String truth = Boolean.FALSE.toString();

        if(node.getOptions().containsKey("truth")){
          truth = Boolean.valueOf((boolean)node.getOptions().get("truth")).toString();
        }

        String nLabel = function.hashString(
            String.join("_", neigh_label, edge_type, truth), Charset.defaultCharset()
        ).asInt() + "";

        neighbours.add(nLabel);
      }

      Collections.sort(neighbours);
      String rLabel = String.join("_", neighbours);
      rLabel = function.hashString(
          String.join("_", label, rLabel), Charset.defaultCharset()
      ).asInt() + "";

      return rLabel;
    }


    public Map<String, Integer> iterate(){
      iteration++;
      if(iteration == 0){
        return iteration0();
      }else if(iteration == 1){
        GraphAnalyser.applyDD(graph);
        GraphAnalyser.applyCD(graph);
      }

      Map<String, String> oldRelabel = relabel;
      relabel = new HashMap<>();

      Map<String, Integer> count = new HashMap<>();

      for(String n: graph.nodes()){
        String label = relabelNode(n, oldRelabel);
        relabel.put(n, label);
        if(!count.containsKey(label))
          count.put(label, 0);
        count.put(label, count.get(label) + 1);
      }

      return count;
    }

}

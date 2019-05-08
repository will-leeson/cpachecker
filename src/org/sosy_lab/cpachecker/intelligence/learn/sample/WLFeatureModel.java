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
package org.sosy_lab.cpachecker.intelligence.learn.sample;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.ast.base.CFAProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.NativeGraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.SVGraph;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

public class WLFeatureModel {

    private static Map<String, String> relabelLabel = null;

    private static Map<String, String> relabelLabel(){
      if(relabelLabel == null) {
        Map<String, String> map = new HashMap<>();
        map.put("UNSIGNED_INT", "INT");
        map.put("LONG_UNSIGNED_INT", "LONG");
        map.put("LONG_INT", "LONG");
        map.put("LONGLONG_UNSIGNED_INT", "LONG");
        map.put("LONGLONG_INT", "LONG");
        map.put("LONG_UNSIGNED_LONG", "LONG");
        map.put("LONG_LONG", "LONG");
        map.put("UNSIGNED_CHAR", "CHAR");
        map.put("VOLATILE_LONG_LONG", "VOLATILE_LONG");
        map.put("VOLATILE_LONG_UNSIGNED_INT", "VOLATILE_LONG");
        map.put("VOLATILE_LONG_INT", "VOLATILE_LONG");
        map.put("VOLATILE_LONG_UNSIGNED_LONG", "VOLATILE_LONG");
        map.put("VOLATILE_UNSIGNED_INT", "VOLATILE_INT");
        map.put("CONST_UNSIGNED_INT", "CONST_INT");
        map.put("CONST_LONG_LONG", "CONST_LONG");
        map.put("CONST_LONG_UNSIGNED_LONG", "CONST_LONG");
        map.put("CONST_LONGLONG_UNSIGNED_LONGLONG", "CONST_LONG");
        map.put("CONST_LONGLONG_LONGLONG", "CONST_LONG");
        map.put("CONST_UNSIGNED_CHAR", "CONST_CHAR");
        map.put("INT_LITERAL_SMALL", "INT_LITERAL");
        map.put("INT_LITERAL_MEDIUM", "INT_LITERAL");
        map.put("INT_LITERAL_LARGE", "INT_LITERAL");

        relabelLabel = ImmutableMap.copyOf(map);
      }
      return relabelLabel;
    }

    private CFA cfa;
    private int astDepth;

    private SVGraph graph;
    private GraphAnalyser analyser;
    private Map<String, String> relabel = new HashMap<>();
    private int iteration = -1;

    public WLFeatureModel(CFA pCFA, int pAstDepth){
      cfa = pCFA;
      astDepth = pAstDepth;
    }

    public int getAstDepth(){
      return astDepth;
    }

    private Map<String, Integer> iteration0(ShutdownNotifier pShutdownNotifier)
        throws InterruptedException {
      graph = getGraph(pShutdownNotifier);

      Map<String, Integer> count = new HashMap<>();

      for(String n: graph.nodes()){
        String label = graph.getNode(n).getLabel();

        if(pShutdownNotifier != null)
          pShutdownNotifier.shutdownIfNecessary();

        //if(relabelLabel().containsKey(label))
          //label = relabelLabel().get(label);

        if(!count.containsKey(label))
          count.put(label, 0);
        count.put(label, count.get(label) + 1);
      }


      return count;
    }

    public SVGraph getGraph(ShutdownNotifier pShutdownNotifier) throws InterruptedException {
      if(graph == null){
        graph = new CFAProcessor().process(cfa, astDepth, pShutdownNotifier);
      }
      return graph;
    }

    private GraphAnalyser getGraphAnalyser(ShutdownNotifier pShutdownNotifier)
        throws InterruptedException {
      if(analyser == null) {
        SVGraph g = getGraph(pShutdownNotifier);
        analyser = new NativeGraphAnalyser(cfa, g, pShutdownNotifier, null);
      }
      return analyser;
    }

    private String hash(String str){
      HashFunction function = Hashing.murmur3_32();
      byte[] bytes = function.hashString(str, StandardCharsets.UTF_8).asBytes();
      bytes = Bytes.toArray(Lists.reverse(Bytes.asList(bytes)));
      return new BigInteger(bytes).toString();
    }


    private String relabelNode(String nodeId, Map<String, String> relabelIndex){
      String label = graph.getNode(nodeId).getLabel();
      if(relabelIndex.containsKey(nodeId))
        label = relabelIndex.get(nodeId);
      if(relabelLabel().containsKey(label))
        label = relabelLabel().get(label);

      List<String> neighbours = new ArrayList<>();

      for(GEdge e: graph.getIngoing(nodeId)){
        GNode node = graph.getNode(e.getSource().getId());
        String neigh_label = node.getLabel();
        if(relabelIndex.containsKey(e.getSource().getId()))
          neigh_label = relabelIndex.get(e.getSource().getId());
        if(relabelLabel().containsKey(neigh_label))
          neigh_label = relabelLabel().get(neigh_label);
        String edge_type = e.getId();

        //String truth = Boolean.FALSE.toString();

        //if(node.getOptions().containsKey("truth")){
          //truth = Boolean.valueOf((boolean)node.getOptions().get("truth")).toString();
        //}

        String nLabel = String.join("_", neigh_label, edge_type);

        nLabel = hash(nLabel);

        neighbours.add(nLabel);
      }

      Collections.sort(neighbours);
      String rLabel = String.join("_", neighbours);
      if(!rLabel.isEmpty())
        rLabel = String.join("_", label, rLabel);
      else
        rLabel = label;
      rLabel = hash(rLabel);

      return rLabel;
    }

    public Map<String, Integer> iterate(){
      try {
        return iterate(null);
      } catch (InterruptedException pE) {
        return  new HashMap<>();
      }
    }


    public Map<String, Integer> iterate(ShutdownNotifier pShutdownNotifier)
        throws InterruptedException {
      iteration++;
      GraphAnalyser localAnalyser = getGraphAnalyser(pShutdownNotifier);
      if(iteration == 0){
        localAnalyser.pruneBlank();
        return iteration0(pShutdownNotifier);
      }else if(iteration == 1){
        localAnalyser.applyDummyEdges();
        localAnalyser.pruneGraph();
        localAnalyser.applyDD();
        localAnalyser.applyCD();
      }

      if(pShutdownNotifier != null){
        pShutdownNotifier.shutdownIfNecessary();
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

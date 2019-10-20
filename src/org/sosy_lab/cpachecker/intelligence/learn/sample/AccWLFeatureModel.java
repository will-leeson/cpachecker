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
package org.sosy_lab.cpachecker.intelligence.learn.sample;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.base.CFAProcessor;
import org.sosy_lab.cpachecker.intelligence.ast.neural.SVPEProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.NativeGraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.BlockedGraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer.AliasAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;
import ove.crypto.digest.Blake2b;

public class AccWLFeatureModel implements IWLFeatureModel {

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
    private Map<String, Map<String, String>> relabel = new HashMap<>();
    private int iteration = -1;

    public AccWLFeatureModel(CFA pCFA, int pAstDepth){
      cfa = pCFA;
      astDepth = pAstDepth;
    }

    @Override
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

    @Override
    public SVGraph getGraph(ShutdownNotifier pShutdownNotifier) throws InterruptedException {
      if(graph == null){
        graph = new SVPEProcessor().process(cfa, pShutdownNotifier);
      }
      return graph;
    }

    private GraphAnalyser getGraphAnalyser(ShutdownNotifier pShutdownNotifier)
        throws InterruptedException {
      if(analyser == null) {
        SVGraph g = getGraph(pShutdownNotifier);
        analyser = new BlockedGraphAnalyser(g, pShutdownNotifier, null);
      }
      return analyser;
    }

    private String hash(String str){
      Blake2b blake2b = Blake2b.Digest.newInstance();
      byte[] bytes = blake2b.digest(str.getBytes(StandardCharsets.UTF_8));
      bytes = Bytes.toArray(Lists.reverse(Bytes.asList(bytes)));
      return new BigInteger(bytes).toString();
    }


    private String relabel(String source, List<String> neighbours){

      Collections.sort(neighbours);
      neighbours.add(0, source);
      String text = String.join("_", neighbours);

      return hash(text);
    }


    private Map<String, String> relabelNode(String nodeId, Map<String, Map<String, String>> relabelIndex){
      return new HashMap<>();
    }

    @Override
    public Map<String, Integer> iterate(){
      try {
        return iterate(null);
      } catch (InterruptedException pE) {
        return  new HashMap<>();
      }
    }


    @Override
    public Map<String, Integer> iterate(ShutdownNotifier pShutdownNotifier)
        throws InterruptedException {
      iteration++;
      GraphAnalyser localAnalyser = getGraphAnalyser(pShutdownNotifier);
      if(iteration == 0){
        localAnalyser.simplify();
        localAnalyser.recursionDetection();
        return iteration0(pShutdownNotifier);
      }else if(iteration == 1){

        Map<String, Set<String>> aliases = null;
        if(graph.getGlobalOption(OptionKeys.POINTER_GRAPH) != null){
          AliasAnalyser aliasAnalyser = new AliasAnalyser(graph.getGlobalOption(OptionKeys.POINTER_GRAPH), pShutdownNotifier);
          aliases = aliasAnalyser.getAliases();

          graph.setGlobalOption(OptionKeys.POINTER_GRAPH, null);
          aliasAnalyser = null;
        }

        localAnalyser.applyDD(aliases);

        localAnalyser.disconnectFunctionsViaDependencies();
        localAnalyser.applyCD();
      }

      if(pShutdownNotifier != null){
        pShutdownNotifier.shutdownIfNecessary();
      }

      Map<String, Map<String, String>> oldRelabel = relabel;
      relabel = new HashMap<>();

      Map<String, Integer> count = new HashMap<>();

      for(String n: graph.nodes()){
        Map<String, String> labels = relabelNode(n, oldRelabel);
        relabel.put(n, labels);

        for(String label : labels.values()) {
          if (!count.containsKey(label))
            count.put(label, 0);
          count.put(label, count.get(label) + 1);
        }
      }

      return count;
    }

}

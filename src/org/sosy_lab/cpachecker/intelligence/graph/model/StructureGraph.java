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
package org.sosy_lab.cpachecker.intelligence.graph.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.Options.Key;

public class StructureGraph {

    private Map<String, GNode> nodes;

    private Options globalOptions = new Options();

    private int lastGen = 0;

    public StructureGraph(){
      nodes = new HashMap<>();
    }

    public boolean addNode(String id){
      return addNode(id, "");
    }

    public boolean addNode(String id, String label){
      return addNode(new GNode(id, label));
    }

    protected boolean addNode(GNode node){
        if(nodes.containsKey(node.getId())){
            if(node.getLabel().isEmpty() || !nodes.get(node.getId()).getLabel().isEmpty()){
                return false;
            }else{
                nodes.get(node.getId()).setLabel(node.getLabel());
                return true;
            }
        }
        nodes.put(node.getId(), node);
        return true;
    }

    private void add(Map<String, Map<String, GEdge>> map, String id, GEdge pGEdge){
      if(!map.containsKey(pGEdge.getId())){
        map.put(pGEdge.getId(), new HashMap<>());
      }
      map.get(pGEdge.getId()).put(id, pGEdge);
    }

    protected boolean addEdge(GEdge e){
      if(e.getSource() == null || e.getSink() == null){
        return false;
      }

      if(!nodes.containsKey(e.getSource().getId()))return false;
      if(!nodes.containsKey(e.getSink().getId()))return false;

      if(e.getSource().out.containsKey(e.getId()) && e.getSource().out.get(e.getId()).containsKey(e.getSink().getId())) return false;
      add(e.getSource().out, e.getSink().getId(), e);
      add(e.getSink().in, e.getSource().getId(), e);


      return true;
    }

    public boolean addEdge(String source, String target, String id){
      return addEdge(new GEdge(id, nodes.get(source), nodes.get(target)));
    }

    public <T> void setGlobalOption(Key<T> key, T option){
      globalOptions.put(key, option);
    }

    public <T> T getGlobalOption(Key<T> key){
      return globalOptions.get(key);
    }

    public Stream<GEdge> getIngoingStream(String target){
      if(!nodes.containsKey(target))
        return Stream.empty();
      return nodes.get(target).in.values().stream()
            .map(m -> m.values()).flatMap(Collection::stream);
    }

    public Set<GEdge> getIngoing(String target) {
      return getIngoingStream(target).collect(Collectors.toSet());
    }

    public Stream<GEdge> getOutgoingStream(String source){
      if(!nodes.containsKey(source))
        return Stream.empty();
      return nodes.get(source).out.values()
          .stream().map(m -> m.values()).flatMap(Collection::stream);
    }

    public Set<GEdge> getOutgoing(String source){
      return getOutgoingStream(source).collect(Collectors.toSet());
    }


    public Stream<GEdge> getIngoingTypedStream(String target, String type){
      if(!nodes.containsKey(target))
        return Stream.empty();
      return nodes.get(target).in.getOrDefault(type, new HashMap<>()).values()
            .stream();
    }

    public Set<GEdge> getIngoingTyped(String target, String type) {
      return getIngoingTypedStream(target, type).collect(Collectors.toSet());
    }

    public Stream<GEdge> getOutgoingTypedStream(String source, String type){
      if(!nodes.containsKey(source))
        return Stream.empty();
      return nodes.get(source).out.getOrDefault(type, new HashMap<>()).values()
          .stream();
    }

    public Set<GEdge> getOutgoingTyped(String source, String type){
      return getOutgoingTypedStream(source, type).collect(Collectors.toSet());
    }

    public GEdge getEdge(String source, String target, String id){
        if(nodes.containsKey(source)){
          GNode node = getNode(source);

          if(node.out.containsKey(id)){
            Map<String, GEdge> out = node.out.get(id);
            if(out.containsKey(target)){
              return out.get(target);
            }
          }


        }
        return null;
    }


    public GNode getNode(String id){
      if(nodes.containsKey(id)){
        return nodes.get(id);
      }
      return null;
    }

    public boolean removeEdge(GEdge pGEdge){
      if(pGEdge.getSource().out.containsKey(pGEdge.getId())){
        pGEdge.getSource().out.get(pGEdge.getId()).remove(pGEdge.getSink().getId());
      }

      if(pGEdge.getSink().in.containsKey(pGEdge.getId())){
        pGEdge.getSink().in.get(pGEdge.getId()).remove(pGEdge.getSource().getId());
      }

      return true;
    }

    public boolean removeNode(String id){
      if(!nodes.containsKey(id))return false;

      for(GEdge e: getIngoing(id)){
        removeEdge(e);
      }

      for(GEdge e: getOutgoing(id)){
        removeEdge(e);
      }

      nodes.remove(id);

      return true;
    }



    public Set<String> nodes(){
      return nodes.keySet();
    }

    public Stream<GEdge> edgeStream(){
      return nodes.values().stream()
                .map(n -> n.out.values()).flatMap(Collection::stream)
                .map(m -> m.values()).flatMap(Collection::stream);
    }


    @Override
    public String toString(){
      String s =  "Graph (Nodes "+nodes().size()+"):\n";
      for(GEdge edge: edgeStream().collect(Collectors.toSet())){
        s += edge.getSource().getId()+"[ "+edge.getSource().getLabel()+" ] -- "+edge.getId()+" --> "+edge.getSink().getId()+"[ "+edge.getSink().getLabel()+" ]\n";
      }
      return s;
    }

    public String genId(String prefix){
      while(nodes.containsKey(prefix + lastGen))
        lastGen++;
      return prefix+(lastGen++);
    }

    public String toDot(Predicate<GNode> filter, Function<GEdge, String> colorFunction, Function<GNode, String> cluster){
      String s = "digraph G {\n";

      Map<String, Set<String>> clusterMap = new HashMap<>();

      for(String n: this.nodes()){
        GNode node = this.nodes.get(n);
        if(filter.test(node)){
          s += n+"[ label=\""+node.getLabel()+"\" ];\n";

          String clusterId = cluster.apply(node);

          if(!clusterMap.containsKey(clusterId))
            clusterMap.put(clusterId, new HashSet<>());
          clusterMap.get(clusterId).add(n);

        }
      }

      for(Entry<String, Set<String>> clusterEntry : clusterMap.entrySet()) {
        String clusterId = clusterEntry.getKey();

        if (!clusterId.equals("main")) {
          s += "subgraph cluster_" + clusterId + " {\n label=\"" + clusterId
              + "()\";\n color=black;\n";

          for (String n : clusterEntry.getValue())
            s += n+"; ";
          s += "\n";

          s += "}\n";

        }


      }


      for(Entry<String, Set<String>> clusterEntry : clusterMap.entrySet()){

        for(String n: clusterEntry.getValue()) {

          for (GEdge e : this.getIngoing(n)) {

            String color = colorFunction.apply(e);

            if (color.equals("opaque"))
              continue;

            s +=
                e.getSource().getId() + " -> " + n + (!color.equals("black") ? "[color=" + color + "]"
                                                                           : "") + ";\n";
          }
        }
      }

      return s + "};";
    }

    public String toDot(){
      return toDot(
          x -> true,
          x -> "black",
          x -> "main");
    }
}

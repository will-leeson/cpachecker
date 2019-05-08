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
package org.sosy_lab.cpachecker.intelligence.graph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.protobuf.Option;
import java.util.ArrayDeque;
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
import org.sosy_lab.cpachecker.intelligence.graph.Options.Key;

public class StructureGraph {

    private Map<String, GNode> nodes;
    private Table<String, String, Map<String, GEdge>> edges;
    private Table<String, String, Map<String, GEdge>> reverseEdges;

    private Options globalOptions = new Options();

    private int lastGen = 0;

    public StructureGraph(){
      nodes = new HashMap<>();
      edges = HashBasedTable.create();
      reverseEdges = HashBasedTable.create();
    }

    public boolean addNode(String id){
      return addNode(id, "");
    }

    public boolean addNode(String id, String label){
      if(nodes.containsKey(id)){
        if(label.isEmpty() || !nodes.get(id).getLabel().isEmpty()){
          return false;
        }else{
          nodes.get(id).setLabel(label);
          return true;
        }
      }
      nodes.put(id, new GNode(id, label));
      return true;
    }

    protected boolean addEdge(GEdge e){
      if(e.getSource() == null || e.getSink() == null){
        return false;
      }

      if(!nodes.containsKey(e.getSource().getId()))return false;
      if(!nodes.containsKey(e.getSink().getId()))return false;

      String sink = e.getSink().getId();
      String source = e.getSource().getId();

      if(!edges.contains(source, sink)){
        Map<String, GEdge> map = new HashMap<>();
        edges.put(source, sink, map);
        reverseEdges.put(sink, source, map);
      }
      Map<String, GEdge> edge = edges.get(source, sink);

      if(edge.containsKey(e.getId()))return  false;
      edge.put(e.getId(), e);

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
      return reverseEdges.row(target).values().stream()
          .map(m -> m.values()).flatMap(Collection::stream);
    }

    public Set<GEdge> getIngoing(String target) {
      return getIngoingStream(target).collect(Collectors.toSet());
    }

    public Stream<GEdge> getOutgoingStream(String source){
      return edges.row(source).values().stream()
          .map(m -> m.values()).flatMap(Collection::stream);
    }

    public Set<GEdge> getOutgoing(String source){
      return getOutgoingStream(source).collect(Collectors.toSet());
    }

    public GEdge getEdge(String source, String target, String id){
        if(edges.contains(source, target)){
          Map<String, GEdge> eMap = edges.get(source, target);
          if(eMap.containsKey(id))
            return eMap.get(id);
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
      if(edges.contains(pGEdge.getSource().getId(), pGEdge.getSink().getId())){
        Map<String, GEdge> m = edges.get(pGEdge.getSource().getId(), pGEdge.getSink().getId());
        if(m.containsKey(pGEdge.getId())){
          m.remove(pGEdge.getId());
          if(m.isEmpty()){
            edges.remove(pGEdge.getSource().getId(), pGEdge.getSink().getId());
            reverseEdges.remove(pGEdge.getSink().getId(), pGEdge.getSource().getId());
          }
          return true;
        }
      }
      return false;
    }

    public boolean removeNode(String id){
      if(!nodes.containsKey(id))return false;
      nodes.remove(id);

      for(GEdge e: getIngoing(id)){
        removeEdge(e);
      }

      for(GEdge e: getOutgoing(id)){
        removeEdge(e);
      }

      return true;
    }



    public Set<String> nodes(){
      return nodes.keySet();
    }

    public Stream<GEdge> edgeStream(){
      return edges.values().stream()
              .map(M -> M.values())
              .flatMap(Collection::stream);
    }

    @Override
    public String toString(){
      String s =  "Graph (Nodes "+nodes().size()+"):\n";
      for(Cell<String, String, Map<String, GEdge>> cell: edges.cellSet()){
        for(String k: cell.getValue().keySet()){
          s += cell.getRowKey()+"[ "+nodes.get(cell.getRowKey()).getLabel()+" ] -- "+k+" --> "+cell.getColumnKey()+"[ "+nodes.get(cell.getColumnKey()).getLabel()+" ]\n";
        }
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

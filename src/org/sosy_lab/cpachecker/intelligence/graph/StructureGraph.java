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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class StructureGraph {

    public enum EdgeType{
      CFG, CD, DD, S
    }

    private Map<String, GNode> nodes;
    private Table<String, String, Map<String, GEdge>> edges;

    private int lastGen = 0;

    public StructureGraph(){
      nodes = new HashMap<>();
      edges = HashBasedTable.create();
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

    private boolean addEdge(GEdge e){
      String sink = e.getSink().getId();
      String source = e.getSource().getId();

      if(!edges.contains(source, sink)){
        edges.put(source, sink, new HashMap<>());
      }
      Map<String, GEdge> edge = edges.get(source, sink);

      if(edge.containsKey(e.getId()))return  false;
      edge.put(e.getId(), e);
      return true;
    }

    public boolean addEdge(String source, String target, String id){
      if(!nodes.containsKey(source))return false;
      if(!nodes.containsKey(target))return false;
      return addEdge(new GEdge(id, nodes.get(source), nodes.get(target)));
    }

    public boolean addCFGEdge(String source, String target){
      if(!nodes.containsKey(source))return false;
      if(!nodes.containsKey(target))return false;
      return addEdge(new CFGEdge(nodes.get(source), nodes.get(target)));
    }

    public boolean addCDEdge(String source, String target){
      if(!nodes.containsKey(source))return false;
      if(!nodes.containsKey(target))return false;
      return addEdge(new CDEdge(nodes.get(source), nodes.get(target)));
    }

    public boolean addDDEdge(String source, String target){
      if(!nodes.containsKey(source))return false;
      if(!nodes.containsKey(target))return false;
      return addEdge(new DDEdge(nodes.get(source), nodes.get(target)));
    }


    public boolean addSEdge(String source, String target){
      if(!nodes.containsKey(source))return false;
      if(!nodes.containsKey(target))return false;
      return addEdge(new SEdge(nodes.get(source), nodes.get(target)));
    }

    public Set<GEdge> getIngoing(String target){
        Map<String, Map<String, GEdge>> E = edges.column(target);
        Set<GEdge> set = new HashSet<>();

        for(Map<String, GEdge> m: E.values()){
          set.addAll(m.values());
        }

        return set;
    }

    public Set<GEdge> getOutgoing(String source){
      Map<String, Map<String, GEdge>> E = edges.row(source);
      Set<GEdge> set = new HashSet<>();

      for(Map<String, GEdge> m: E.values()){
        set.addAll(m.values());
      }

      return set;
    }

    public GEdge getEdge(String source, String target, String id){
        if(edges.contains(source, target)){
          Map<String, GEdge> eMap = edges.get(source, target);
          if(eMap.containsKey(id))
            return eMap.get(id);
        }
        return null;
    }

    public GEdge getEdge(String source, String target, EdgeType pEdgeType){
        String id = "";
        switch (pEdgeType){
          case CFG:
            id = "cfg";
            break;
          case CD:
            id = "cd";
            break;
          case DD:
            id = "dd";
            break;
          case S:
            id = "s";
            break;
        }

        if(id.isEmpty())
          return null;

        return getEdge(source, target, id);
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

    public String toDot(){
      String s = "digraph G {\n";

      for(String n: this.nodes()){
        if(n.startsWith("N")){
          s += n+"[ label="+this.nodes.get(n).getLabel()+" ];\n";
        }
      }

      for(String n: this.nodes()){
        if(n.startsWith("N")){

          for(GEdge e: this.getOutgoing(n)){
            if(e instanceof CFGEdge){
              s += n+" -> "+e.getSink().getId()+";\n";
            }
            if(e instanceof DDEdge){
              s += n+" -> "+e.getSink().getId()+"[color=red];\n";
            }
            if(e instanceof CDEdge){
              s += n+" -> "+e.getSink().getId()+"[color=green];\n";
            }
            if(e instanceof SEdge){
              s += n+" -> "+e.getSink().getId()+"[color=blue];\n";
            }
          }

        }
      }


      return s + "}";
    }

    public String toDFSRepresentation(){
      Map<String, Integer> index = new HashMap<>();

      String startId = null;

      for(String nodeId: this.nodes()){
        GNode node = this.getNode(nodeId);
        if(node.getLabel().equalsIgnoreCase("START")){
          startId = nodeId;
          break;
        }
      }

      if(startId == null){
        for(String nodeId: this.nodes()){
          if(this.getIngoing(nodeId).isEmpty()){
            startId = nodeId;
            break;
          }
        }
      }

      int counter = 0;
      Stack<String> stack = new Stack<>();
      stack.push(startId);

      while (!stack.isEmpty()){

        String id = stack.pop();

        if(index.containsKey(id))continue;

        index.put(id, counter++);

        for(GEdge e: this.getOutgoing(id)){
          stack.push(e.getSink().getId());
        }

        for(GEdge e: this.getIngoing(id)){
          stack.push(e.getSource().getId());
        }

      }

      String s = "[";
      stack.push(startId);
      Set<String> seen = new HashSet<>();
      Set<DFSReprNode> seenRepr = new HashSet<>();

      while (!stack.isEmpty()){

        String id = stack.pop();

        if(seen.contains(id))continue;
        seen.add(id);

        for(GEdge e: this.getOutgoing(id)){
          DFSReprNode node = new DFSReprNode(index.get(id), index.get(e.getSink().getId()), false,
              e);
          if(!seenRepr.contains(node)) {
            seenRepr.add(node);
            s = s + node.getRepresentation()+", ";
          }
          stack.push(e.getSink().getId());
        }

        for(GEdge e: this.getIngoing(id)){
          DFSReprNode node = new DFSReprNode(index.get(id), index.get(e.getSource().getId()), true,
              e);
          if(!seenRepr.contains(node)) {
            seenRepr.add(node);
            s = s + node.getRepresentation()+", ";
          }
          stack.push(e.getSource().getId());
        }

      }

      return s.substring(0, s.length()-2) +"]";

    }

    private class DFSReprNode{

      public DFSReprNode(
          int pSourceIndex,
          int pSinkIndex,
          boolean pIncoming,
          GEdge pEdge) {
        sourceIndex = pSourceIndex;
        sinkIndex = pSinkIndex;
        incoming = pIncoming;
        edge = pEdge;
      }

      private int sourceIndex;
      private int sinkIndex;
      private boolean incoming;
      private GEdge edge;

      private String edgeRepr(){
        if(sourceIndex <= sinkIndex){
          if(incoming){
            return "<|"+edge.getId();
          }else{
            return edge.getId()+"|>";
          }
        }else {
          if(incoming){
            return edge.getId()+"|>";
          }else {
            return "<|"+edge.getId();
          }
        }
      }

      private String getSourceLabel(){

        if(sourceIndex <= sinkIndex){
          if(incoming){
            return edge.getSink().getLabel();
          }else{
            return edge.getSource().getLabel();
          }
        }else{
          if(incoming){
            return edge.getSource().getLabel();
          }else{
            return edge.getSink().getLabel();
          }
        }
      }

      private String getSinkLabel(){

        if(sourceIndex <= sinkIndex){
          if(incoming){
            return edge.getSource().getLabel();
          }else{
            return edge.getSink().getLabel();
          }
        }else{
          if(incoming){
            return edge.getSink().getLabel();
          }else{
            return edge.getSource().getLabel();
          }
        }
      }

      public String getRepresentation(){
        int source = (sourceIndex <= sinkIndex)?sourceIndex:sinkIndex;
        int sink = (sourceIndex <= sinkIndex)?sinkIndex:sourceIndex;
        String sourceLabel = getSourceLabel();
        String sinkLabel  = getSinkLabel();

        return "["+source+", "+sink+", \""+sourceLabel+"\", \""+edgeRepr()+"\", \""+sinkLabel+"\"]";

      }

      @Override
      public int hashCode(){
        int source = (sourceIndex <= sinkIndex)?sourceIndex:sinkIndex;
        int sink = (sourceIndex <= sinkIndex)?sinkIndex:sourceIndex;
        return source*13*13 + sink*13 + edgeRepr().hashCode();
      }

      @Override
      public boolean equals(Object pDFSReprNode){
        if(pDFSReprNode instanceof DFSReprNode){

          DFSReprNode node = (DFSReprNode)pDFSReprNode;

          int source = (sourceIndex <= sinkIndex)?sourceIndex:sinkIndex;
          int sink = (sourceIndex <= sinkIndex)?sinkIndex:sourceIndex;
          int oSource = (node.sourceIndex <= node.sinkIndex)?node.sourceIndex: node.sinkIndex;
          int oSink = (node.sourceIndex <= node.sinkIndex)?node.sinkIndex: node.sourceIndex;

          return source==oSource && sink == oSink && edgeRepr().equals(node.edgeRepr());
        }
        return false;
      }

    }

}

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
package org.sosy_lab.cpachecker.intelligence.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.SCCUtil.SCC;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.CachedGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.IDominator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.IGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.InverseGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.SGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.TarjanDominator;

public class GraphAnalyser {

  private LogManager logger;
  private StructureGraph graph;
  private ShutdownNotifier shutdownNotifier;

  private String startNode;
  private String endNode;
  private IGraphNavigator navigator;


  public GraphAnalyser(StructureGraph pGraph, ShutdownNotifier pShutdownNotifier, LogManager pLogger)
      throws InterruptedException {
    logger = pLogger;
    graph = pGraph;
    shutdownNotifier = pShutdownNotifier;

    initNavigator();

    for(String n: navigator.nodes()){

      if(pShutdownNotifier != null){
        pShutdownNotifier.shutdownIfNecessary();
      }

      if(pGraph.getNode(n).getLabel().equals(ASTNodeLabel.START.name())){
        startNode = n;
        break;
      }
    }

    endNode = findEndOrFix();
    initNavigator();

  }

  public GraphAnalyser( StructureGraph pGraph) throws InterruptedException {
    this(pGraph, null, null);
  }


  private void initNavigator(){
    navigator = new CachedGraphNavigator(new SGraphNavigator(graph));
  }


  private void pruneFloating(){
    //Floating nodes

    Set<String> floatingNodes = new HashSet<>();

    for(String nodeId: navigator.nodes()){
      GNode node = graph.getNode(nodeId);

      if(!node.getLabel().equalsIgnoreCase("START") && navigator.predecessor(nodeId).isEmpty()){
        floatingNodes.add(nodeId);
      }

    }

    for(String nodeId: floatingNodes)
      graph.removeNode(nodeId);

    initNavigator();
  }

  public void pruneGraph(){
    pruneFloating();
  }

  public void applyDummyEdges()
      throws InterruptedException {

    if(shutdownNotifier != null)
      shutdownNotifier.shutdownIfNecessary();

    Set<GEdge> toDelete = new HashSet<>();

    for(String id: graph.nodes()){
      GNode node = graph.getNode(id);

      if(shutdownNotifier != null)
        shutdownNotifier.shutdownIfNecessary();

      if(id.equalsIgnoreCase(endNode))continue;

      if(graph.getOutgoing(id).isEmpty() || (node.getLabel().contains("FUNC_CALL") && node.getLabel().contains("VERIFIER_ERROR"))){

        for(GEdge e: graph.getOutgoing(id)){
          toDelete.add(e);
        }

        graph.addCFGEdge(id, endNode);

      }

    }

    for(GEdge delete: toDelete)
      graph.removeEdge(delete);

    if(shutdownNotifier != null)
      shutdownNotifier.shutdownIfNecessary();

    SCCUtil sccUtil = new SCCUtil(graph);

    for(SCC scc: sccUtil.getStronglyConnectedComponents()){

      if(scc.getNodes().size() < 2){
        continue;
      }

      String sourceId = null;
      boolean terminate = false;

      for(String nId: scc.getNodes()){
        if(shutdownNotifier != null)
          shutdownNotifier.shutdownIfNecessary();

        for(GEdge e: graph.getOutgoing(nId)){
          if(!scc.getNodes().contains(e.getSink().getId())){
            terminate = true;
            break;
          }
        }

        if(terminate)
          break;

        Set<GEdge> out = graph.getOutgoing(nId);

        if(out.size() < 2){
          sourceId = nId;
        }


      }

      if(!terminate){
        graph.addCFGEdge(sourceId, endNode);
      }

    }

    initNavigator();


  }


  private Set<String> diffComponent(Set<String> pNodes,
                                 IGraphNavigator pGraphNavigator,
                                 String start){

    Set<String> nodes = new HashSet<>(pNodes);
    Stack<String> search = new Stack<>();
    search.push(start);

    while(!search.isEmpty()){
      String c = search.pop();

      if(!nodes.remove(c))
        continue;

      for(String s : pGraphNavigator.successor(c))
        search.add(s);

    }

    return nodes;
  }

  private String connectComponent(
      IGraphNavigator pGraphNavigator,
      String start, boolean forward){

    Set<String> diff = diffComponent(
        pGraphNavigator.nodes(), pGraphNavigator, start
    );

    if(!diff.isEmpty()){
      String id = graph.genId("N");
      graph.addNode(id, graph.getNode(start).getLabel());

      if(forward)
        graph.addDummyEdge(id, start);
      else
        graph.addDummyEdge(start, id);


      start = id;
    }


    List<String> discComp = new ArrayList<>();

    while (!diff.isEmpty()){

      int size = diff.size();

      String minNode = null;
      int min = Integer.MAX_VALUE;

      for(String d : diff){
        int pred = pGraphNavigator.predecessor(d).size();
        if(pred < min){
          minNode = d;
          min = pred;
        }
      }

      diff = diffComponent(diff, pGraphNavigator, minNode);

      int difference = size - diff.size();

      if(difference == 1){
        graph.removeNode(minNode);
        continue;
      }

      discComp.add(graph.getNode(minNode).getLabel()+"-"+(difference));

      if(forward)
        graph.addDummyEdge(start, minNode);
      else
        graph.addDummyEdge(minNode, start);

    }

    if(logger != null && !discComp.isEmpty()){

      String s = "Found disconnected components: ";
      for(String size : discComp)
        s = s + "[-"+size+"-] ";

      logger.log(Level.INFO, s);

    }

    return start;

  }


  public void connectComponents(){

    startNode = connectComponent(
        navigator, startNode, true
    );

    endNode = connectComponent(
        new InverseGraphNavigator(navigator), endNode, false
    );

    initNavigator();
  }

  public void pruneBlank(){

    for(String n : navigator.nodes()){

      if(graph.getNode(n).getLabel().equals(ASTNodeLabel.BLANK.name())){

        Set<String> pred = navigator.predecessor(n);
        Set<String> succ = navigator.successor(n);

        graph.removeNode(n);
        for(String p : pred){
          for(String s : succ){
            graph.addCFGEdge(p, s);
          }
        }

      }

    }
    initNavigator();

  }

  private Map<String, Integer> rpo(){

    Set<String> seen = new HashSet<>();
    Stack<String> stack = new Stack<>();
    Map<String, Integer> rpo = new HashMap<>();
    int count = navigator.nodes().size();

    stack.push(startNode);

    while(!stack.isEmpty()){

      while(!stack.isEmpty() && seen.contains(stack.peek())){
        String s = stack.pop();
        if(!rpo.containsKey(s))
          rpo.put(s, count--);
      }

      if(stack.isEmpty())break;

      String current = stack.peek();
      seen.add(current);

      for(String succesor : navigator.successor(current)){
        if(!seen.contains(succesor))
          stack.push(succesor);
      }
    }

    return rpo;

  }


  public void applyDD() throws InterruptedException {

    Map<String, Integer> rpo = rpo();

    Map<String, Set<String>> output = new HashMap<>();
    Map<String, Set<String>> variables = new HashMap<>();

    PriorityQueue<String> queue = new PriorityQueue<>(
        new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            return Integer.compare(rpo.get(o1), rpo.get(o2));
          }
        }
    );

    for(String n : navigator.nodes()){

      if(shutdownNotifier != null)
        shutdownNotifier.shutdownIfNecessary();

      Map<String, Object> options = graph.getNode(n).getOptions();

      Object out = options.get("output");
      if(out != null && out instanceof Set){
        output.put(n, (Set<String>)out);
      }

      Object var = options.get("variables");
      if(var != null && var instanceof Set){
        variables.put(n, (Set<String>)var);
      }

      queue.add(n);
    }


    Map<String, Map<String, Set<String>>> reachingDef = new HashMap<>();


    while (!queue.isEmpty()){

        if(shutdownNotifier != null)
          shutdownNotifier.shutdownIfNecessary();

        String current = queue.poll();

        Map<String, Set<String>> last =
            new HashMap<>(reachingDef.getOrDefault(current, new HashMap<>()));

        Set<String> out = output.getOrDefault(current, new HashSet<>());

        //Al-Phi
        for (String variable : out) {
          //Kill and generate...
          Set<String> pointer = new HashSet<>();
          pointer.add(current);
          last.put(variable, pointer);
        }

        for(String successor : navigator.successor(current)) {

          boolean change = false;

          if(!reachingDef.containsKey(successor)){
            reachingDef.put(successor, last);
            change = true;
          }else{

            Map<String, Set<String>> next = reachingDef.get(successor);

            for(Entry<String, Set<String>> k : last.entrySet()){

              if(!next.containsKey(k.getKey())){
                next.put(k.getKey(), k.getValue());
                change = true;
              }else{

                Set<String> pointer = next.get(k.getKey());

                for(String i : k.getValue()){
                  change |= pointer.add(i);
                }
              }
            }
          }

          if (change) {
            queue.add(successor);
          }

        }
      }

      for(Entry<String, Set<String>> use : variables.entrySet()){

        if(shutdownNotifier != null)
          shutdownNotifier.shutdownIfNecessary();

        Map<String, Set<String>> reaching = reachingDef.get(use.getKey());

        if(reaching == null)continue;

        for(String v : use.getValue()){

          for(String pointer : reaching.getOrDefault(v, new HashSet<>())){
            graph.addDDEdge(pointer, use.getKey());
          }

        }


      }

  }

  private Set<String> findOnlyCyle(String start){

    Map<String, String> pred = new HashMap<>();

    Stack<String> stack = new Stack<>();
    Set<String> onlyCycle = new HashSet<>();
    stack.add(start);

    while(!stack.isEmpty()){
      String s = stack.pop();

      Set<String> path = new HashSet<>();

      String c = s;
      while(pred.containsKey(c)){
        c = pred.get(c);
        path.add(c);
      }
      path.add(s);


      Set<String> cylces = new HashSet<>();

      for(String next: navigator.successor(s)){
        if(path.contains(next)){
          cylces.add(next);
        }
      }

      boolean oCycle = cylces.size() > 0;

      for(String next: navigator.successor(s)){
          if(!pred.containsKey(next)){
            stack.add(next);
            pred.put(next, s);
          }

          if(!cylces.contains(next))
            oCycle = false;
      }

      if(oCycle)
        onlyCycle.add(s);

    }

    return onlyCycle;

  }

  private String findEndOrFix(){
    String start = startNode;
    String end = "";

    for(String n: navigator.nodes()){
      if(graph.getNode(n).getLabel().equals(ASTNodeLabel.END.name())){
        end = n;
        break;
      }
    }

    if(!end.isEmpty()){
      return end;
    }

    Set<String> possibleEnds = findOnlyCyle(start);

    String id = graph.genId("N");
    graph.addNode(id, ASTNodeLabel.END.name());

    for(String n: possibleEnds){
      graph.addCFGEdge(n, id);
    }

    initNavigator();

    return id;
  }


  public void applyCD() throws InterruptedException {

    IGraphNavigator navi = new InverseGraphNavigator(navigator);
    IDominator dominator = new TarjanDominator(navi, endNode);

    for(String n : navi.nodes()){
      Set<String> pred = navi.predecessor(n);

      if(shutdownNotifier != null)
        shutdownNotifier.shutdownIfNecessary();

      if(pred.size() <= 1)continue;
      if(graph.getNode(n).getLabel().equals(ASTNodeLabel.START.name()))continue;

      String idom = dominator.getIDom(n);

      for(String p : pred){

        String runner = p;

        while (!runner.equals(idom)){
          graph.addCDEdge(n , runner);
          runner = dominator.getIDom(runner);
        }


      }


    }


  }

  public void defaultAnalysis() throws InterruptedException {
    pruneBlank();
    connectComponents();
    applyDummyEdges();
    applyDD();
    applyCD();
  }




}

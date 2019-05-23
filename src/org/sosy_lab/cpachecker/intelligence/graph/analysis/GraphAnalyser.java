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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.protobuf.Struct;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.FunctionGraph.CallEdge;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.SCCUtil.SCC;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.dominator.IDominator;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.navigator.IGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.model.navigator.InverseGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.dominator.IterativeDominator;
import org.sosy_lab.cpachecker.intelligence.graph.model.navigator.SGraphNavigator;
import org.sosy_lab.cpachecker.util.Pair;

public class GraphAnalyser {

  private LogManager logger;
  protected SVGraph graph;
  protected ShutdownNotifier shutdownNotifier;

  protected String startNode;
  protected String endNode;
  protected IGraphNavigator navigator;

  Map<String, Integer> rpoIndex;


  public GraphAnalyser(SVGraph pGraph, ShutdownNotifier pShutdownNotifier, LogManager pLogger)
      throws InterruptedException {
    logger = pLogger;
    graph = pGraph;
    shutdownNotifier = pShutdownNotifier;

    initNavigator();

    assignEntryAndExit();

    initNavigator();

  }

  protected void assignEntryAndExit() throws InterruptedException {
    for(String n: navigator.nodes()){

      if(shutdownNotifier != null){
        shutdownNotifier.shutdownIfNecessary();
      }

      if(graph.getNode(n).getLabel().equals(ASTNodeLabel.START.name())){
        startNode = n;
        break;
      }
    }

    endNode = findEndOrFix();
  }

  public GraphAnalyser(SVGraph pGraph) throws InterruptedException {
    this(pGraph, null, null);
  }


  protected void initNavigator(){
    navigator = new SGraphNavigator(graph);
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

    initNavigator();


  }


  private Set<String> diffComponent(Set<String> pNodes,
                                 IGraphNavigator pGraphNavigator,
                                 String start){

    Set<String> nodes = new HashSet<>(pNodes);
    ArrayDeque<String> search = new ArrayDeque<>();
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

    if(rpoIndex == null) {

      Set<String> seen = new HashSet<>();
      ArrayDeque<String> stack = new ArrayDeque<>();
      Map<String, Integer> rpo = new HashMap<>();
      int count = navigator.nodes().size();

      stack.push(startNode);

      while (!stack.isEmpty()) {

        while (!stack.isEmpty() && seen.contains(stack.peek())) {
          String s = stack.pop();
          if(graph.getNode(s) == null)
            System.out.println(s);
          if (!rpo.containsKey(s))
            rpo.put(s, count--);
        }

        if (stack.isEmpty()) break;

        String current = stack.peek();
        seen.add(current);

        for (String succesor : navigator.successor(current)) {
          if (!seen.contains(succesor))
            stack.push(succesor);
        }
      }
      rpoIndex = rpo;
    }

    return rpoIndex;

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

    for(String n : rpo.keySet()){

      if(shutdownNotifier != null)
        shutdownNotifier.shutdownIfNecessary();

      GNode node = graph.getNode(n);

      if(node.containsOption(OptionKeys.DECL_VARS)){
        output.put(n, node.getOption(OptionKeys.DECL_VARS));
      }

      if(node.containsOption(OptionKeys.VARS)){
        variables.put(n, node.getOption(OptionKeys.VARS));
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

    ArrayDeque<String> stack = new ArrayDeque<>();
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

  private void applyGeneralCD(String pStartNode, String pEndNode) throws InterruptedException {
    initNavigator();
    IGraphNavigator navi = new InverseGraphNavigator(navigator);
    IDominator dominator = new IterativeDominator(navi, pEndNode);

    ArrayDeque<String> queue = new ArrayDeque<>();
    queue.add(pStartNode);
    Set<String> seen = new HashSet<>();

    while (!queue.isEmpty()){
      String n = queue.pop();
      Set<String> pred = navi.predecessor(n);

      if(shutdownNotifier != null)
        shutdownNotifier.shutdownIfNecessary();

      if(pred.size() > 1 && !n.equals(pStartNode)) {

        String idom = dominator.getIDom(n);

        for (String p : pred) {

          Set<String> runnerSet = new HashSet<>();

          String runner = p;

          while (!Objects.equals(runner, idom)) {
            runnerSet.add(runner);

            String pRunner = runner;
            runner = dominator.getIDom(runner);

            if (runner == null || runner.equals(pRunner)) {
              runnerSet.clear();
              break;
            }

          }


          for (String r : runnerSet)
            graph.addCDEdge(n, r);

        }
      }

      for(GEdge next : graph.getOutgoing(n)){
        String id = next.getSink().getId();
        if(!seen.contains(id)){
          seen.add(id);
          queue.add(id);
        }
      }
    }
  }

  private void applyContextCD(String context, String pStartNode, String pEndNode)
      throws InterruptedException {

    initNavigator();
    IGraphNavigator navi = new InverseGraphNavigator(navigator);
    IDominator dominator = new IterativeDominator(navi, pEndNode);

    for(String n : graph.nodes()){
      GNode node = graph.getNode(n);

      if(node.getOption(OptionKeys.PARENT_FUNC) == null) {
        if(!context.equals("main"))
          continue;

      }else if(!node.getOption(OptionKeys.PARENT_FUNC).equals(context))
        continue;

      Set<String> pred = navi.predecessor(n);

      if(shutdownNotifier != null)
        shutdownNotifier.shutdownIfNecessary();

      if(pred.size() > 1 && !n.equals(pStartNode)) {

        String idom = dominator.getIDom(n);

        for (String p : pred) {

          Set<String> runnerSet = new HashSet<>();

          String runner = p;

          while (!Objects.equals(runner, idom)) {
            runnerSet.add(runner);

            String pRunner = runner;
            runner = dominator.getIDom(runner);

            if (runner == null || runner.equals(pRunner)) {
              runnerSet.clear();
              break;
            }

          }


          for (String r : runnerSet)
            graph.addCDEdge(n, r);

        }
      }

    }
  }

  private void applyCD(String pStartNode, String pEndNode) throws InterruptedException {
    GNode start = graph.getNode(pStartNode);

    if(start.containsOption(OptionKeys.PARENT_FUNC)){
      applyContextCD(start.getOption(OptionKeys.PARENT_FUNC), pStartNode, pEndNode);
    }else{
      applyGeneralCD(pStartNode, pEndNode);
    }
  }


  public void applyCD() throws InterruptedException {

    if(graph.getGlobalOption(OptionKeys.FUNC_BOUNDRY) != null){

      for(Entry<String, Pair<String, String>> e : graph.getGlobalOption(OptionKeys.FUNC_BOUNDRY).entrySet()){
        applyCD(e.getValue().getFirst(), e.getValue().getSecond());
      }

    }
    applyCD(startNode, endNode);
  }

  private void pruneNodes(Set<String> prune){

    Map<String, Set<String>> succ = new HashMap<>();

    for(String id : prune){

      for(GEdge incoming : graph.getIngoing(id)){

        if(prune.contains(incoming.getSource().getId()))
          continue;

        if(!succ.containsKey(incoming.getSource().getId())){
          succ.put(incoming.getSource().getId(), new HashSet<>());
        }
        Set<String> next = succ.get(incoming.getSource().getId());

        Queue<String> possible = new ArrayDeque<>(
            graph.getOutgoingStream(id).map(e -> e.getSink().getId()).collect(Collectors.toSet())
        );
        Set<String> seen = new HashSet<>();

        while (!possible.isEmpty()){

          String current = possible.poll();
          seen.add(current);

          if(prune.contains(current)){
            for(String succs : graph.getOutgoingStream(current).map(e -> e.getSink().getId()).collect(
                Collectors.toSet())){

              if(!next.contains(succs) && !seen.contains(succs))
                possible.add(succs);

            }
          }else {
            next.add(current);
          }

        }

      }

    }

    for(String r : prune)
      graph.removeNode(r);

    for(Entry<String, Set<String>> entry : succ.entrySet()){
      for(String n : entry.getValue())
        graph.addCFGEdge(entry.getKey(), n);
    }


  }

  public void simplify() throws InterruptedException {
    applyDummyEdges();

    Set<String> prune = new HashSet<>();

    for(String id : graph.nodes()){
      if(id.startsWith("N")){
        GNode node = graph.getNode(id);
        if(node.getLabel().equals("BLANK") || node.getLabel().equals("LABEL") || node.getLabel().equals("SKIP")){
          prune.add(id);
        }
      }
    }

    pruneNodes(prune);
  }

  public void recursionDetection(){

      FunctionGraph functionGraph = new FunctionGraph();

      for(String n : graph.nodes()){
        GNode node = graph.getNode(n);

        if(node.containsOption(OptionKeys.FUNC_CALL)){
          String parent = node.getOption(OptionKeys.PARENT_FUNC);

          if(parent == null){
            logger.log(Level.WARNING, "ParentListener needs to be included for recursion detection.");
            return;
          }

          String call = node.getOption(OptionKeys.FUNC_CALL);
          functionGraph.addNode(parent);
          functionGraph.addNode(call);
          functionGraph.addEdge(parent, call, n);
        }
      }

      SCCUtil sccUtil = new SCCUtil(functionGraph);

      for(SCC scc: sccUtil.getStronglyConnectedComponents()){

        Set<String> nodes = scc.getNodes();

        for(String n : nodes){

          for(GEdge edge : functionGraph.getOutgoingStream(n).filter(e -> nodes.contains(e.getSink().getId())).collect(
              Collectors.toSet())){

            CallEdge call = (CallEdge)edge;
            GNode funcCall = graph.getNode(call.getFunctionCall());

            funcCall.setLabel(
                funcCall.getLabel() + "_"+ASTNodeLabel.RECURSIVE.name()
            );


          }

        }

      }


  }

  public void disconnectFunctionsViaDependencies(){

    Map<String, String> start = new HashMap<>();
    Map<String, String> stop = new HashMap<>();

    for(String n : graph.nodes()){

      GNode node = graph.getNode(n);

      if(node.getLabel().equals("FUNCTION_START")){
        for(GEdge pe : graph.getIngoing(n)){

          if(!pe.getId().equals("cfg"))continue;

          GNode p = pe.getSource();
          if(p.getLabel().contains("FUNC_CALL")) {
            graph.removeEdge(pe);
            graph.addDDEdge(p.getId(), n);
            graph.addCDEdge(p.getId(), n);
          }

        }

        if(node.containsOption(OptionKeys.PARENT_FUNC)){
          start.put(node.getOption(OptionKeys.PARENT_FUNC), n);
        }

      }else if(node.getLabel().equals("FUNCTION_EXIT")){
        for(GEdge pe : graph.getOutgoing(n)){

          if(!pe.getId().equals("cfg"))continue;

          GNode p = pe.getSink();
          graph.removeEdge(pe);
          graph.addDDEdge(n, p.getId());

        }

        if(node.containsOption(OptionKeys.PARENT_FUNC)){
          stop.put(node.getOption(OptionKeys.PARENT_FUNC), n);
        }
      }

    }

    Map<String, Pair<String, String>> boundry = new HashMap<>();

    for(Entry<String, String> e : start.entrySet()){
      if(stop.containsKey(e.getKey())){
        boundry.put(e.getKey(), Pair.of(e.getValue(), stop.get(e.getKey())));
      }
    }

    graph.setGlobalOption(OptionKeys.FUNC_BOUNDRY, boundry);

  }


}

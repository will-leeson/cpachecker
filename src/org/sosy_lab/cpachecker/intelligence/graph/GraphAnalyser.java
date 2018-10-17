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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.IDominator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.IGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.InverseGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.SGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.SimpleDominator;
import org.sosy_lab.cpachecker.intelligence.graph.dominator.TarjanDominator;

public class GraphAnalyser {


  public static void applyDD(StructureGraph pGraph){
    applyDD(pGraph, pGraph.nodes().size() * 10);
  }


  public static void applyDD(StructureGraph pGraph, int cooldown){

    Table<String, String, String> lastDef = HashBasedTable.create();
    Set<String> seen = new HashSet<>();
    Stack<String> stack = new Stack<>();

    for(String n: pGraph.nodes()){
      if(pGraph.getNode(n).getLabel().equals(ASTNodeLabel.START.name())){
        stack.add(n);
        break;
      }
    }

    int cooling = cooldown;

    while(cooling > 0 && !stack.isEmpty()){

      cooling--;

      String position = stack.pop();
      Map<String, Object> options = pGraph.getNode(position).getOptions();

      if(options.containsKey("variables")){
        for(String v: (Set<String>) options.get("variables")){
          String last = lastDef.get(position, v);
          if(last != null && pGraph.addDDEdge(position, last)){
            cooling = cooldown;
          }
        }
      }

      Map<String, String> newDef = new HashMap<>(lastDef.row(position));
      if(options.containsKey("output")){
        for(String v: (Set<String>) options.get("output")){
          newDef.put(v, position);
        }
      }

      for(GEdge e: pGraph.getOutgoing(position)){
        if(e instanceof CFGEdge){
          String next = e.getSink().getId();
          boolean change = !seen.contains(next);
          for(Entry<String, String> def: newDef.entrySet()){
            String v = def.getKey();
            change |= !lastDef.contains(next, v) || !lastDef.get(next, v).equals(def.getValue());
            lastDef.put(next, v, def.getValue());
          }

          if(change){
            seen.add(next);
            stack.add(next);
          }

        }
      }


    }
  }


  private static Set<String> findOnlyCyle(StructureGraph pGraph, String start){

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

      for(GEdge e: pGraph.getOutgoing(s)){
        if(e instanceof CFGEdge){
          String next = e.getSink().getId();

          if(path.contains(next)){
            cylces.add(next);
          }

        }
      }

      boolean oCycle = cylces.size() > 0;

      for(GEdge e: pGraph.getOutgoing(s)){
        if(e instanceof CFGEdge){
          String next = e.getSink().getId();

          if(!pred.containsKey(next)){
            stack.add(next);
            pred.put(next, s);
          }

          if(!cylces.contains(next))
            oCycle = false;

        }
      }

      if(oCycle)
        onlyCycle.add(s);

    }

    return onlyCycle;

  }

  private static String findEndOrFix(StructureGraph pGraph){
    String start = "";
    String end = "";

    for(String n: pGraph.nodes()){
      if(pGraph.getNode(n).getLabel().equals(ASTNodeLabel.END.name())){
        end = n;
        break;
      }
      if(pGraph.getNode(n).getLabel().equals(ASTNodeLabel.START.name())){
        start = n;
      }
    }

    if(!end.isEmpty()){
      return end;
    }

    Set<String> possibleEnds = findOnlyCyle(pGraph, start);


    String id = pGraph.genId("N");
    pGraph.addNode(id, ASTNodeLabel.END.name());

    for(String n: possibleEnds){
      pGraph.addCFGEdge(n, id);
    }

    return id;
  }



  public static void applyCD(StructureGraph pGraph){

    String end = findEndOrFix(pGraph);

    IGraphNavigator navigator = new InverseGraphNavigator(
        new SGraphNavigator(pGraph)
    );

    IDominator dominator = new TarjanDominator(navigator, end);

    Stack<CDTravelor> stack = new Stack<>();
    Set<String> branches = new HashSet<>();
    Set<String> seen = new HashSet<>();

    for(String n: navigator.nodes()){
      if(pGraph.getNode(n).getLabel().equals(ASTNodeLabel.START.name())){
        stack.add(new CDTravelor(n, new HashSet<>()));
      }
      if(navigator.predecessor(n).size() > 1){
        branches.add(n);
      }
    }

    while(!stack.isEmpty()){
      CDTravelor travelor = stack.pop();

      Set<String> nPost = dominator.getDom(travelor.pos);
      nPost.retainAll(travelor.lastPostDomination);
      nPost.retainAll(branches);

      Set<String> rest = new HashSet<>(travelor.lastPostDomination);
      rest.removeAll(nPost);

      for(String r: rest){
        if(!r.equals(travelor.pos)){
          pGraph.addCDEdge(travelor.pos, r);
        }
      }

      if(seen.contains(travelor.pos))
        continue;
      seen.add(travelor.pos);

      for(String next: navigator.predecessor(travelor.pos)){
        stack.add(new CDTravelor(next, new HashSet<>(nPost)));
      }

    }
  }

  private static class CDTravelor{
    String pos;
    Set<String> lastPostDomination;

    public CDTravelor(String pPos, Set<String> pLastPostDomination) {
      pos = pPos;
      lastPostDomination = pLastPostDomination;
      lastPostDomination.add(pos);
    }

  }

}


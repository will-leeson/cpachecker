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
package org.sosy_lab.cpachecker.intelligence.graph.dominator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.graph.navigator.IGraphNavigator;

public class IterativeDominator implements IDominator {

  private BiMap<String, Integer> rpost;
  private int counter = 0;
  private Map<Integer, Integer> doms = new HashMap<>();
  private Multimap<String, String> dominator;

  private IGraphNavigator navigator;
  private String start;

  public IterativeDominator(IGraphNavigator pNavigator, String pStart) {
    this(pNavigator, pStart, HashBiMap.create());
  }

  public IterativeDominator(IGraphNavigator pNavigator, String pStart, BiMap<String, Integer> reverse) {
    navigator = pNavigator;
    start = pStart;
    rpost = reverse;

    while(rpost.containsValue(counter++)){}

  }

  private int index(String s){
    if(!rpost.containsKey(s)){
      rpost.put(s, counter++);
    }
    return rpost.get(s);
  }

  private int flow(int from, int to){
    return from;
  }

  private int merge(int u1, int u2){
    int f1 = u1;
    int f2 = u2;

    while (f1 != f2){
      while(f1 > f2){
        f1 = doms.get(f1);
      }

      while (f2 > f1){
        f2 = doms.get(f2);
      }

    }

    return f1;
  }


  private void rpostIndex(){

    if(!rpost.isEmpty())return;

    Set<String> seen = new HashSet<>();
    ArrayDeque<String> stack = new ArrayDeque<>();
    int count = navigator.nodes().size();
    counter = count+1;

    stack.push(start);

    while(!stack.isEmpty()){

      while(!stack.isEmpty() && seen.contains(stack.peek())){
        String s = stack.pop();
        if(!rpost.containsKey(s))
          rpost.put(s, count--);
      }

      if(stack.isEmpty())break;

      String current = stack.peek();
      seen.add(current);

      for(String succesor : navigator.successor(current)){
        if(!seen.contains(succesor))
          stack.push(succesor);
      }
    }

  }


  private void calculateDoms(){
    if(!doms.isEmpty())return;

    rpostIndex();

    int startN = index(start);
    doms.put(startN, startN);

    Queue<int[]> queue = new ArrayDeque<>();

    for(String succ : navigator.successor(start)){
      int succI = index(succ);
      queue.add(new int[]{startN, succI});
    }

    while (!queue.isEmpty()){

      int[] fl = queue.poll();

      int idom = flow(fl[0], fl[1]);

      int cdom = -1;

      if(doms.containsKey(fl[1])){
        cdom = doms.get(fl[1]);
        idom = merge(cdom, idom);
      }

      if(idom != cdom){
        doms.put(fl[1], idom);

        for(String succ : navigator.successor(rpost.inverse().get(fl[1]))){
          int succI = index(succ);
          queue.add(new int[]{fl[1], succI});
        }

      }

    }



  }


  @Override
  public Set<String> getDom(String v) {
    if(v == null){
      return new HashSet<>();
    }
    if(dominator == null){
      dominator = HashMultimap.create();
    }
    if(!dominator.containsKey(v)){
      String p = v;
      while(!(p = getIDom(p)).equals(start)){
        dominator.put(v, p);
      }
      dominator.put(v, start);
      dominator.put(v, v);
    }

    return new HashSet<>(dominator.get(v));
  }

  @Override
  public String getIDom(String v) {
    if(v == null){
      return start;
    }

    calculateDoms();

    int vi = index(v);

    if(doms.containsKey(vi)){
      return rpost.inverse().get(doms.get(vi));
    }

    return start;
  }
}

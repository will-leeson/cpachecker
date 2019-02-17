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
package org.sosy_lab.cpachecker.intelligence.graph.dominator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;

public class TarjanDominator implements IDominator {

  private IGraphNavigator navigator;
  private String start;

  private BiMap<String, Integer> index;
  private Map<String, String> parent = new HashMap<>();
  private Map<String, Integer> semi = new HashMap<>();
  private Map<String, String> dom = new HashMap<>();
  private Multimap<String, String> bucket = HashMultimap.create();
  private Multimap<String, String> dominator;

  private ShutdownNotifier notifier;


  public TarjanDominator(IGraphNavigator pGraphNavigator, String pStart) {
    this(pGraphNavigator, pStart, null);
  }

  public TarjanDominator(IGraphNavigator pGraphNavigator, String pStart, ShutdownNotifier pShutdownNotifier) {
    navigator = pGraphNavigator;
    start = pStart;
    notifier = pShutdownNotifier;
  }


  private int semi(String s){
    if(semi.containsKey(s)){
      return semi.get(s);
    }
    if(index.containsKey(s)){
      return index.get(s);
    }
    return 0;
  }

  private int index(String s){
    if(index.containsKey(s)){
      return index.get(s);
    }
    return 0;
  }

  private String vertex(int i){
    return index.inverse().get(i);
  }



  private void dfs() throws InterruptedException {
    this.index = HashBiMap.create();
    ArrayDeque<String> stack = new ArrayDeque<>();
    stack.add(start);

    int i = 0;

    while(!stack.isEmpty()){

      if(notifier != null){
        notifier.shutdownIfNecessary();
      }

      String s = stack.pop();

      if(index.containsKey(s))
        continue;
      index.put(s, i++);

      for(String succ: navigator.successor(s)){
        if(index.containsKey(succ))
          continue;
        parent.put(succ, s);
        stack.add(succ);
      }
    }
  }

  private void step234() throws InterruptedException {
    Forest forest = new Forest();

    for(int i = index.size()-1; i >=1; i--){

      if(notifier != null){
        notifier.shutdownIfNecessary();
      }

      String w = vertex(i);

      for(String p: navigator.predecessor(w)){
        String u = forest.eval(p);

        if(semi(u) < semi(w)){
          semi.put(w, semi(u));
        }
      }

      bucket.put(vertex(semi(w)), w);
      forest.link(parent.get(w), w);

      Set<String> buck = new HashSet<>(bucket.get(parent.get(w)));
      for(String b: buck){
        bucket.remove(parent.get(w), b);
        String u = forest.eval(b);
        if(semi(u) < semi(b)){
          dom.put(b, u);
        }else{
          dom.put(b, parent.get(w));
        }
      }
    }

    for(int i = 1; i < index.size(); i++){
      String w = vertex(i);
      if(dom.get(w) != vertex(semi(w))){
        dom.put(w, dom.get(dom.get(w)));
      }
    }
    dom.put(vertex(0), vertex(0));
  }

  @Override
  public Set<String> getDom(String v){
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
    if(index == null){
      try {
        dfs();
        step234();
      }catch (InterruptedException e){
        return start;
      }
    }
    if(dom.containsKey(v)){
      return dom.get(v);
    }

    return start;
  }


  private class Forest{
    Map<String, String> ancestor = new HashMap<>();
    Map<String, String> label = new HashMap<>();
    Map<String, Integer> size = new HashMap<>();
    Map<String, String> child = new HashMap<>();

    private String ancestor(String s){
      if(ancestor.containsKey(s)){
        return ancestor.get(s);
      }
      return null;
    }

    private String label(String s){
      if(label.containsKey(s)){
        return label.get(s);
      }
      return s;
    }

    private int size(String s){
      if(size.containsKey(s)){
        return size.get(s);
      }
      return 1;
    }

    private String child(String s){
      if(child.containsKey(s)){
        return child.get(s);
      }
      return vertex(0);
    }

    public void link(String v, String w){
      ancestor.put(w, v);
    }


    private void compress(String v){

      ArrayDeque<String> toCompress = new ArrayDeque<>();
      toCompress.add(v);
      ArrayDeque<String> process = new ArrayDeque<>();

      while(!toCompress.isEmpty()){
        String act = toCompress.pop();
        if(ancestor(ancestor(act)) != null){
          toCompress.add(ancestor(act));
          process.add(act);
        }
      }

      while(!process.isEmpty()){
        String act = process.pop();
        if(semi(label(ancestor(act))) < semi(label(act))){
          label.put(act, label(ancestor(act)));
        }
        ancestor.put(act, ancestor(ancestor(act)));
      }

    }

    public String eval(String v){
      if(ancestor(v)==null){
        return v;
      }
      compress(v);
      return label(v);
    }

  }

}

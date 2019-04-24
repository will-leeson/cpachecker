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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.graph.navigator.IGraphNavigator;

public class SimpleDominator implements IDominator {

  private IGraphNavigator navigator;
  private String start;

  private Multimap<String, String> dom;
  private Map<String, String> idom;

  public SimpleDominator(IGraphNavigator pGraphNavigator, String pStart) {
    navigator = pGraphNavigator;
    start = pStart;
  }

  private void calcDom(){

    dom = HashMultimap.create();

    dom.put(start, start);

    Set<String> controlNodes = navigator.nodes();

    for(String n: controlNodes){
      if(!n.equals(start)){
        dom.putAll(n, controlNodes);
      }
    }

    boolean change;
    do {
      change = false;

      for(String n: controlNodes){
        if(!n.equals(start)){

          Set<String> inter = new HashSet<>();

          for(String p: navigator.predecessor(n)){
            if(inter.isEmpty()){
              inter.addAll(dom.get(p));
            }else{
              inter.retainAll(dom.get(p));
            }
          }
          inter.add(n);

          if(!change){
            Set<String> old = new HashSet<>(dom.get(n));
            change = !old.containsAll(inter) || !inter.containsAll(old);
          }
          dom.removeAll(n);
          dom.putAll(n, inter);

        }
      }

    }while(change);

  }

  @Override
  public Set<String> getDom(String v) {
    if(dom == null){
      calcDom();
    }
    if(dom.containsKey(v)){
      return new HashSet<>(dom.get(v));
    }
    return null;
  }

  @Override
  public String getIDom(String v) {

    if(idom == null)
      idom = new HashMap<>();

    if(idom.containsKey(v))
      return idom.get(v);

    Set<String> doms = getDom(v);
    doms.remove(v);

    for(String d: new HashSet<>(doms)){
      Set<String> ds = getDom(d);
      ds.remove(d);
      doms.removeAll(ds);
    }

    String iDom;

    if(doms.size() > 1) {
      System.out.println("WARNING: Dominator doesn't work correctly");
      return null;
    }

    if(doms.isEmpty()) {
      iDom = start;
    }else{
      iDom = doms.iterator().next();
    }

    idom.put(v, iDom);

    return iDom;
  }
}

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
package org.sosy_lab.cpachecker.intelligence.learn.binary.impl.jaccard;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.learn.sample.EmptySample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.RealProgramSample;


public class JaccardKernelCall extends RecursiveTask<Double> {

  private IProgramSample first;
  private IProgramSample loaded;
  private ShutdownNotifier notifier;

  public JaccardKernelCall(
      IProgramSample pFirst,
      IProgramSample pSecond,
      ShutdownNotifier pShutdownNotifier) {
    first = pFirst;
    loaded = pSecond;
    notifier = pShutdownNotifier;
  }

  @Override
  protected Double compute() {
    if(loaded instanceof EmptySample || first instanceof EmptySample)
      return -1.0;

    int m = Math.min(first.getMaxIteration(), loaded.getMaxIteration()) + 1;
    double norm = 1.0/m;

    double d = 0;

    for(int i = 0; i < m; i++){
      Map<IFeature, Double> bag1;

      if(first instanceof RealProgramSample){
        try {
          bag1 = ((RealProgramSample) first).getFeatureBag(i, notifier);
        } catch (InterruptedException pE) {
          return -1.0;
        }
      }else{
        bag1 = first.getFeatureBag(i);
      }

      Map<IFeature, Double> bag2 = loaded.getFeatureBag(i);

      double min = 0.0;
      double max = 0.0;

      Set<IFeature> rest = new HashSet<>(bag2.keySet());

      for(Entry<IFeature, Double> e: bag1.entrySet()){
        if(bag2.containsKey(e.getKey())){
          min += Math.min(e.getValue(), bag2.get(e.getKey()));
          max += Math.max(e.getValue(), bag2.get(e.getKey()));
          rest.remove(e.getKey());
        }else{
          max += e.getValue();
        }
      }

      for(IFeature f: rest){
        max += bag2.get(f);
      }

      d += norm * (min/max);

    }

    first = null;
    loaded = null;

    return d;
  }
}



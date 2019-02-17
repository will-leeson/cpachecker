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
package org.sosy_lab.cpachecker.intelligence.learn.binary.impl.jaccard.planning;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.learn.sample.EmptySample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.RealProgramSample;

/**
 * This call is specially designed for the backend implementation
 *
 * Assumption: Bag is precomputed for currentSample before this execution
 */
public class PlannedJaccardKernelCall implements Callable<Double> {

  private RealProgramSample currentSample;
  private IProgramSample compareTo;
  private ShutdownNotifier notifier;

  public PlannedJaccardKernelCall(
      RealProgramSample pCurrentSample,
      IProgramSample pCompareTo,
      ShutdownNotifier pShutdownNotifier) {
    currentSample = pCurrentSample;
    compareTo = pCompareTo;
    notifier = pShutdownNotifier;
  }

  @Override
  public Double call() throws Exception {
    if(compareTo instanceof EmptySample)
      return 0.0;

    int m = Math.min(currentSample.getMaxIteration(), compareTo.getMaxIteration()) + 1;
    double norm = 1.0/m;

    double d = 0;

    for(int i = 0; i < m; i++){
      Map<IFeature, Double> bag1;

      if(notifier != null){
        try {
          notifier.shutdownIfNecessary();
        } catch (InterruptedException pE) {
          return -1.0;
        }
      }

      bag1 = currentSample.getFeatureBag(i, notifier);
      Map<IFeature, Double> bag2 = compareTo.getFeatureBag(i);

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

    return  d;
  }
}

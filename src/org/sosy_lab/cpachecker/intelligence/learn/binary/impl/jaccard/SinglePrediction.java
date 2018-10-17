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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.KernelCoef;

public class SinglePrediction extends RecursiveTask<Integer> {

  private KernelComputationProcess process;
  private KernelCoef coef;

  public SinglePrediction(
      KernelComputationProcess pProcess,
      KernelCoef pCoef) {
    process = pProcess;
    coef = pCoef;
  }


  @Override
  protected Integer compute() {
    double pred = 0.0;

    Map<String, ForkJoinTask<Double>> kernels = new HashMap<>();

    for(Entry<String, Double> c : coef.getCoef().entrySet()){
      kernels.put(c.getKey(), process.kernel(c.getKey()));
    }

    for(Entry<String, Double> c: coef.getCoef().entrySet()){
      double kernel = kernels.get(c.getKey()).join();

      if(kernel == -1){
        System.out.println("Skip instance (prediction may be incorrect): "+c.getKey()+" (alpha: "+c.getValue()+").");
        continue;
      }

      double y = coef.getY().get(c.getKey());
      double alpha = c.getValue();

      pred += alpha * y * kernel;
    }

    pred += coef.getIntercept();


    return pred>=0?1:0;
  }
}

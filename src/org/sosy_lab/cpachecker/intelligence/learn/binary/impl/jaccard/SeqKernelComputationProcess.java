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

import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.KernelCoef;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.DenseVector;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.Vector;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.util.Pair;

public class SeqKernelComputationProcess {

  private SampleRegistry registry;
  private ShutdownNotifier notifier;
  private Table<String, String, KernelCoef> config;
  private IProgramSample sample;
  private List<Pair<String, String>> pos;

  private Map<String, Double> kernelCache = new HashMap<>();

  public SeqKernelComputationProcess(
      SampleRegistry pRegistry,
      ShutdownNotifier pShutdownNotifier,
      Table<String, String, KernelCoef> pConfig,
      IProgramSample pSample,
      List<Pair<String, String>> pPos) {
    registry = pRegistry;
    notifier = pShutdownNotifier;
    config = pConfig;
    sample = pSample;
    pos = pPos;
  }

  private double kernel(String id){

    if(!kernelCache.containsKey(id)) {
      JaccardKernelCall call = new JaccardKernelCall(sample, registry.getSample(id), notifier);
      kernelCache.put(id, call.compute());
    }
    return kernelCache.get(id);

  }

  private double single(KernelCoef coef) throws InterruptedException {
    double pred = 0.0;

    for(Entry<String, Double> c: coef.getCoef().entrySet()){

      if(notifier != null){
        notifier.shutdownIfNecessary();
      }

      // double y = coef.getY().get(c.getKey());
      double alpha = c.getValue();
      double coefficient = alpha;

      if(coefficient != 0) {
        double kernel = kernel(c.getKey());

        if (kernel == -1) {
          System.out.println(
              "Skip instance (prediction may be incorrect): " + c.getKey() + " (alpha: " + c
                  .getValue() + ").");
          continue;
        }

        pred += coefficient * kernel;
      }
    }

    pred += coef.getIntercept();


    return pred>=0?1:0;
  }



  public Vector compute() {
    Vector vector = new DenseVector(pos.size());

    try {
      for (int i = 0; i < pos.size(); i++) {
        Pair<String, String> p = pos.get(i);
        KernelCoef coef = config.get(p.getFirst(), p.getSecond());
        vector.set(i, single(
            coef
        ));
      }
    }catch(InterruptedException e){
      return new DenseVector(pos.size());
    }

    return vector;
  }







}

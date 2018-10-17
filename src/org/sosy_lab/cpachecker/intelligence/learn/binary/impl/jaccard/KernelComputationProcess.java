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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.KernelCoef;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.DenseVector;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.Vector;
import org.sosy_lab.cpachecker.intelligence.learn.sample.EmptySample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.util.Pair;
import scala.Int;

public class KernelComputationProcess extends RecursiveTask<Vector> {

  private SampleRegistry registry;
  private Table<String, String, KernelCoef> config;
  private IProgramSample sample;
  private List<Pair<String, String>> pos;

  private ReadWriteLock lock = new ReentrantReadWriteLock();
  private Map<String, ForkJoinTask<Double>> kernelCache = new HashMap<>();

  public KernelComputationProcess(
      SampleRegistry pRegistry,
      Table<String, String, KernelCoef> pConfig,
      IProgramSample pSample,
      List<Pair<String, String>> pPos) {
    registry = pRegistry;
    config = pConfig;
    sample = pSample;
    pos = pPos;
  }

  ForkJoinTask<Double> kernel(String id){

    if(kernelCache.containsKey(id)){
      return kernelCache.get(id);
    }

    boolean contains = false;


    lock.readLock().lock();
    try{
      contains = kernelCache.containsKey(id);
    }finally{
      if(!contains) {
        lock.readLock().unlock();
        lock.writeLock().lock();
      }
    }

    if(!contains){
      try {
        if(!kernelCache.containsKey(id)) {
          JaccardKernelCall call = new JaccardKernelCall(sample, registry.getSample(id));
          kernelCache.put(id, call.fork());
          return kernelCache.get(id);
        }else{
          return kernelCache.get(id);
        }
      }finally {
          lock.writeLock().unlock();
      }
    }

    try {
      return kernelCache.get(id);
    }finally {
      lock.readLock().unlock();
    }
  }


  @Override
  protected Vector compute() {
    List<Integer> futures = new ArrayList<>();

    for(int i = 0; i < pos.size(); i++){
      futures.add(new SinglePrediction(this,
          config.get(pos.get(i).getFirst(), pos.get(i).getSecond())
      ).invoke());
    }

    Vector vector = new DenseVector(pos.size());

    for(int i = 0; i < pos.size(); i++){
      vector.set(i, futures.get(i));
    }

    return vector;
  }






}

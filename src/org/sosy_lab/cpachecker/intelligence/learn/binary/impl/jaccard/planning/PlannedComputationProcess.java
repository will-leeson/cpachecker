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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.matheclipse.core.reflection.system.Function;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.KernelCoef;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.DenseVector;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.Vector;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.RealProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.util.Pair;

public class PlannedComputationProcess {

  private SampleRegistry registry;
  private ShutdownNotifier notifier;
  private Table<String, String, KernelCoef> config;
  private RealProgramSample sample;
  private List<Pair<String, String>> pos;

  public PlannedComputationProcess(
      SampleRegistry pRegistry,
      ShutdownNotifier pNotifier,
      Table<String, String, KernelCoef> pConfig,
      RealProgramSample pSample,
      List<Pair<String, String>> pPos) {
    registry = pRegistry;
    notifier = pNotifier;
    config = pConfig;
    sample = pSample;
    pos = pPos;
  }

  public Vector compute() throws InterruptedException, ExecutionException {

    List<PlannedKernelComputation> computations = new ArrayList<>();

    for(Pair<String, String> pair: pos){
      computations.add(new PlannedKernelComputation(
          config.get(pair.getFirst(), pair.getSecond())
      ));
    }


    //Init

    //Init lazy loading
    sample.getFeatureBag(sample.getMaxIteration(), notifier);

    List<Map<String, Double>> lookups = new ArrayList<>();
    List<Set<String>> todos = new ArrayList<>();
    Multimap<String, Integer> positionMap = HashMultimap.create();

    int i = 0;
    for(PlannedKernelComputation computation: computations){

      notifier.shutdownIfNecessary();

      Set<String> todo = computation.init();
      todos.add(todo);
      lookups.add(new HashMap<>());

      for(String kernel: todo)
        positionMap.put(kernel, i);

      i++;
    }

    Vector vector = new DenseVector(pos.size());

    //Cycle
    ExecutorService service = Executors.newCachedThreadPool();
    try{

      Map<String, Double> lookup = new HashMap<>();
      Queue<String> queue = new ArrayDeque<>(positionMap.keys());

      while (!queue.isEmpty()){

        Map<String, Future<Double>> futureMap = new HashMap<>();

        for(String s: queue){
          futureMap.put(s, service.submit(
              new PlannedJaccardKernelCall(sample, registry.getSample(s), notifier)
          ));
        }
        queue.clear();

        for(Entry<String, Future<Double>> futureEntry: futureMap.entrySet()){

          String key = futureEntry.getKey();
          double value = futureEntry.getValue().get();

          lookup.put(key, value);

          for(int position: positionMap.removeAll(key)){

            Set<String> leftOver = todos.get(position);
            leftOver.remove(key);
            Map<String, Double> currentMap = lookups.get(position);
            currentMap.put(key, value);

            while (leftOver.isEmpty() && !currentMap.isEmpty()){

              Set<String> nextRound = computations.get(position).react(currentMap);
              currentMap.clear();

              if(nextRound.isEmpty()){
                vector.set(position, computations.get(position).getCurrentPrediction());
                break;
              }

              for(String request: nextRound){
                if(lookup.containsKey(request)){
                  currentMap.put(request, lookup.get(request));
                }else{
                  leftOver.add(request);
                  positionMap.put(request, position);

                  if(!futureMap.containsKey(request)){
                    queue.add(request);
                  }

                }
              }

            }

          }

        }

      }

      return vector;
    }finally {
       service.shutdown();
    }
  }

}

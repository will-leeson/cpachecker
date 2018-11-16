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
package org.sosy_lab.cpachecker.intelligence.oracle.ranking;

import com.google.common.collect.PeekingIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.IRankingProvider.ProviderStatus;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristic;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristicOperation;

public class RankManager implements PeekingIterator<String> {

  private boolean initialised = false;

  private IRankingProvider coreProvider;

  private IRankingProvider currentProvider;
  private int currentPrecision = 0;

  private Map<IRankingProvider, Integer> precisionMap = new HashMap<>();

  private Set<String> seenLabel = new HashSet<>();

  private String buffer;

  private ExecutorService service;
  private Set<IRankingProvider> openProvider = new HashSet<>();


  private int time = 0;
  private List<RankHeuristic> heuristics = new ArrayList<>();
  private Set<RankHeuristicOperation> operations = new HashSet<>();
  private String bufferOverwrite;


  public RankManager(IRankingProvider pCoreProvider) {
    coreProvider = pCoreProvider;
    currentProvider = pCoreProvider;
  }

  public void registerProvider(int precision, IRankingProvider provider){
    precisionMap.put(provider, precision);
  }

  public void registerHeuristic(RankHeuristic pHeuristic){
    heuristics.add(pHeuristic);
  }

  private void init(){
    if(initialised)return;

    initialised = true;

    service = Executors.newCachedThreadPool();
    openProvider.addAll(precisionMap.keySet());

    updatePrecision(0);

    currentProvider.initRanking(this::callback);

    for(RankHeuristic h: heuristics){
      operations.addAll(
          h.apply(-1, currentPrecision, "", seenLabel)
      );
    }


  }


  private void updatePrecision(int precision){
      currentPrecision = precision;

      for(IRankingProvider provider: precisionMap.keySet()){

        int providerPrecision = precisionMap.get(provider);

        if(providerPrecision > currentPrecision){
          if(provider.getStatus() == ProviderStatus.RESULT_AVAILABLE){
            currentProvider = provider;
            currentPrecision = providerPrecision;
          }
        }
      }

      if(service.isTerminated())return;

      for(IRankingProvider provider: precisionMap.keySet()){

        int providerPrecision = precisionMap.get(provider);

        if(providerPrecision >= currentPrecision){
          if(provider.getStatus() == ProviderStatus.INIT){
            service.execute(
                new Runnable() {
                  @Override
                  public void run() {
                    provider.initRanking(RankManager.this::callback);
                  }
                }
            );
            openProvider.add(provider);
          }
        }
      }
  }


  private synchronized void callback(IRankingProvider provider){

    openProvider.remove(provider);

    if(provider == coreProvider){
      if(provider == currentProvider && provider.getStatus() == ProviderStatus.FAILED){
        currentProvider = new DefaultProvider();
        updatePrecision(-1);
      }
    }else if(provider.getStatus() == ProviderStatus.RESULT_AVAILABLE &&
        precisionMap.containsKey(provider)) {

      int precision = precisionMap.get(provider);

      if (precision > currentPrecision) {
        currentProvider = provider;
        updatePrecision(precision);
      }

    }

    if(openProvider.isEmpty()){
      service.shutdown();
    }

  }

  private String handleHeuristic(String label){

    for(RankHeuristic h: heuristics){
      operations.addAll(
          h.apply(time, currentPrecision, label, seenLabel)
      );
    }

    for(RankHeuristicOperation op: new HashSet<>(operations)){
      if(label != null && op.getHeuristicOperator().isActive(time, currentPrecision, label)) {
        label = op.apply(time, currentPrecision, label, seenLabel);
      }
      if(!op.getHeuristicOperator().eventuallyActive()){
        operations.remove(op);
      }
    }

    return label;
  }

  private void nextBuffer(){
    if(bufferOverwrite != null){
      buffer = bufferOverwrite;
      bufferOverwrite = null;
    }else {
      buffer = currentProvider.queryLabel();
    }

    if(buffer == null)return;

    if(seenLabel.contains(buffer)){
      nextBuffer();
    }else{
      seenLabel.add(buffer);
    }

    String hBuffer = handleHeuristic(buffer);

    if(hBuffer == null){
      nextBuffer();
    }

    bufferOverwrite = buffer;
    buffer = hBuffer;


  }


  public void collectStatistics(Collection<Statistics> statsCollection) {
    coreProvider.collectStatistics(statsCollection);

    for(IRankingProvider p: precisionMap.keySet())
      p.collectStatistics(statsCollection);
  }


  @Override
  public String peek() {
    if(hasNext()){
      return buffer;
    }
    throw new NoSuchElementException();
  }

  @Override
  public boolean hasNext() {
    if(!initialised){
      init();
      nextBuffer();
    }

    return buffer != null;
  }

  @Override
  public String next() {
    if(hasNext()) {
      String tmp = buffer;
      nextBuffer();
      time++;
      return tmp;
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}

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
import java.util.PriorityQueue;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.KernelCoef;

public class PlannedKernelComputation {

  private KernelCoef coef;

  private double volume;
  private PriorityQueue<KernelRequest> queue = new PriorityQueue<>();

  private double currentComputation;

  public PlannedKernelComputation(KernelCoef pCoef) {
    coef = pCoef;
  }

  private Set<String> estimateImpactingSet(){

    double toChange = Math.abs(currentComputation);

    if(volume < toChange){
      return new HashSet<>();
    }

    Set<String> out = new HashSet<>();

    while (!queue.isEmpty() && toChange > 0){
      KernelRequest request = queue.poll();
      out.add(request.request);
      toChange -= request.impact;
      volume -= request.impact;
    }

    return out;
  }


  public Set<String> init(){

    volume = 0.0;
    for(Entry<String, Double> e: coef.getCoef().entrySet()){
      double impact = Math.abs(e.getValue());
      queue.add(new KernelRequest(e.getKey(), impact));
      volume += impact;
    }

    currentComputation = coef.getIntercept();

    return estimateImpactingSet();
  }


  public Set<String> react(Map<String, Double> kernel){

    for(Entry<String, Double> kernelValue: kernel.entrySet()){
      double alpha = coef.getCoef().get(kernelValue.getKey());
      currentComputation += alpha * kernelValue.getValue();
    }

    return estimateImpactingSet();
  }

  public double getCurrentPrediction() {
    return currentComputation>=0?1:0;
  }

  private class KernelRequest implements Comparable<KernelRequest>{

    private String request;
    private double impact;

    public KernelRequest(String pRequest, double pImpact) {
      request = pRequest;
      impact = pImpact;
    }

    @Override
    public int compareTo(KernelRequest o) {
      return -Double.valueOf(impact).compareTo(o.impact);
    }
  }

}

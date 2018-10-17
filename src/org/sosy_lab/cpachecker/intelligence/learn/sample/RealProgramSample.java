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
package org.sosy_lab.cpachecker.intelligence.learn.sample;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RealProgramSample implements IProgramSample {

  private static Map<String, String> relabel = null;

  private static Map<String, String> relabel(){
    if(relabel == null) {
      Map<String, String> map = new HashMap<>();
      map.put("UNSIGNED_INT", "INT");
      map.put("LONG_UNSIGNED_INT", "LONG");
      map.put("LONG_INT", "LONG");
      map.put("LONGLONG_UNSIGNED_INT", "LONG");
      map.put("LONGLONG_INT", "LONG");
      map.put("LONG_UNSIGNED_LONG", "LONG");
      map.put("LONG_LONG", "LONG");
      map.put("UNSIGNED_CHAR", "CHAR");
      map.put("VOLATILE_LONG_LONG", "VOLATILE_LONG");
      map.put("VOLATILE_LONG_UNSIGNED_INT", "VOLATILE_LONG");
      map.put("VOLATILE_LONG_INT", "VOLATILE_LONG");
      map.put("VOLATILE_LONG_UNSIGNED_LONG", "VOLATILE_LONG");
      map.put("VOLATILE_UNSIGNED_INT", "VOLATILE_INT");
      map.put("CONST_UNSIGNED_INT", "CONST_INT");
      map.put("CONST_LONG_LONG", "CONST_LONG");
      map.put("CONST_LONG_UNSIGNED_LONG", "CONST_LONG");
      map.put("CONST_LONGLONG_UNSIGNED_LONGLONG", "CONST_LONG");
      map.put("CONST_LONGLONG_LONGLONG", "CONST_LONG");
      map.put("CONST_UNSIGNED_CHAR", "CONST_CHAR");
      map.put("INT_LITERAL_SMALL", "INT_LITERAL");
      map.put("INT_LITERAL_MEDIUM", "INT_LITERAL");
      map.put("INT_LITERAL_LARGE", "INT_LITERAL");

      relabel = ImmutableMap.copyOf(map);
    }
    return relabel;
  }


  private String id;
  private int maxIteration;
  private WLFeatureModel model;
  private FeatureRegistry registry;

  private Map<String, ProgramLabel> labels = new HashMap<>();
  private List<Map<IFeature, Double>> bags = new ArrayList<>();

  private Lock lock = new ReentrantLock();

  public RealProgramSample(
      String pId,
      int pMaxIteration,
      WLFeatureModel pModel,
      FeatureRegistry pRegistry) {
    id = pId;
    maxIteration = pMaxIteration;
    model = pModel;
    registry = pRegistry;
  }


  @Override
  public String getID() {
    return id;
  }

  @Override
  public int getMaxIteration() {
    return maxIteration;
  }

  @Override
  public int getMaxASTDepth() {
    return model.getAstDepth();
  }

  @Override
  public void assignLabel(String labelId, boolean correct, double time) {
      labels.put(labelId, new ProgramLabel(correct, time));
  }

  @Override
  public boolean isCorrect(String labelId) {
    if(labels.containsKey(labelId)){
      return labels.get(labelId).correctSolved;
    }
    throw new NoSuchElementException();
  }

  @Override
  public double getTime(String labelId) {
    if(labels.containsKey(labelId)){
      return labels.get(labelId).time;
    }
    throw new NoSuchElementException();
  }

  @Override
  public boolean isBetter(String labelId1, String labelId2) {
    return ((isCorrect(labelId1)&&!isCorrect(labelId2)) || (isCorrect(labelId1)==isCorrect(labelId2) && (getTime(labelId1) < getTime(labelId2))));
  }

  @Override
  public Set<String> getDefinedLabels() {
    return labels.keySet();
  }

  @Override
  public Map<IFeature, Double> getFeatureBag(int iteration) {
    if(iteration > maxIteration){
      return new HashMap<>();
    }

    if(bags.size() <= iteration) {
      lock.lock();
      try{
        while (bags.size() <= iteration) {
          Map<String, Integer> map = model.iterate();
          Map<IFeature, Double> featureMap = new HashMap<>();

          for (Entry<String, Integer> e : map.entrySet()) {
            String fName = e.getKey();
            if (relabel().containsKey(fName))
              fName = relabel().get(fName);
            featureMap.put(registry.index(fName), (double) e.getValue());
          }

          bags.add(featureMap);
        }
      }finally {
        lock.unlock();
      }
    }

    return bags.get(iteration);
  }

  private class ProgramLabel{
    boolean correctSolved;
    double time;

    public ProgramLabel(boolean pCorrectSolved, double pTime) {
      correctSolved = pCorrectSolved;
      time = pTime;
    }

  }

}

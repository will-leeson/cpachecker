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
package org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.CINDecoder.ProgramLabel;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.Svcomp18Schema.Feature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.Svcomp18Schema.Label;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.Svcomp18Schema.Sample;
import org.sosy_lab.cpachecker.util.Pair;

public class ProtoSample implements IProgramSample {

  private String id;
  private int maxIteration = 0;

  private Map<String, ProgramLabel> labels = new HashMap<>();
  private List<Map<IFeature, Double>> bags = new ArrayList<>();
  private List<Pair<FeatureRegistry, Sample>> samples = new ArrayList<>();


  public ProtoSample(){}


  public void addSerial(FeatureRegistry registry, Sample pSample){

    if(id == null){
      id = pSample.getId();
    }else if(!id.equalsIgnoreCase(pSample.getId())){
      throw new IllegalArgumentException("Samples have to be of same ID ("+id+" != "+pSample.getId()+")");
    }

    maxIteration = Math.max(pSample.getIteration(), maxIteration);

    samples.add(pSample.getIteration(), Pair.of(registry, pSample));

    //for(Label label: pSample.getLabelsList()){
      //labels.put(label.getTool(), new ProgramLabel(label.getSolve() > 0, label.getTime()));
    //}

  }

  private void parse(FeatureRegistry registry, Sample pSample){
    Map<IFeature, Double> bag = new HashMap<>();
    bags.add(pSample.getIteration(), bag);

    for(Feature feature: pSample.getFeaturesList()){
      IFeature f = registry.index(feature.getName());
      bag.put(f, (double)feature.getCount());
    }
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
    return 5;
  }

  @Override
  public void assignLabel(String labelId, boolean correct, double time) {
    labels.put(labelId, new ProgramLabel(correct, time));
  }

  @Override
  public boolean isCorrect(String labelId) {
    if(labels.containsKey(labelId)){
      return labels.get(labelId).isCorrectSolved();
    }
    throw new NoSuchElementException();
  }

  @Override
  public double getTime(String labelId) {
    if(labels.containsKey(labelId)){
      return labels.get(labelId).getTime();
    }
    throw new NoSuchElementException();
  }

  @Override
  public boolean isBetter(String labelId1, String labelId2) {
    return ((isCorrect(labelId1)&&!isCorrect(labelId2)) || (isCorrect(labelId1)==isCorrect(labelId2) && (getTime(labelId1) < getTime(labelId2))));
  }

  @Override
  public Set<String> getDefinedLabels() {
    return new HashSet<>(this.labels.keySet());
  }

  @Override
  public Map<IFeature, Double> getFeatureBag(
      int iteration) {

    if(iteration > getMaxIteration()){
      return new HashMap<>();
    }

    try{
      return bags.get(iteration);
    }catch (IndexOutOfBoundsException e){
      Pair<FeatureRegistry, Sample> p = samples.get(iteration);
      parse(p.getFirst(), p.getSecond());
      return bags.get(iteration);
    }
  }
}

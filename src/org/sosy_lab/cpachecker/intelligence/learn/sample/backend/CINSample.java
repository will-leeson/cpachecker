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
package org.sosy_lab.cpachecker.intelligence.learn.sample.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.CINDecoder.CINDecodingResult;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.CINDecoder.ProgramLabel;

public class CINSample implements IProgramSample {

  private List<List<String>> featureIndex;
  private List<String> labelIndex;
  private ZipFile source;
  private ZipEntry entry;
  private FeatureRegistry registry;

  private Map<String, ProgramLabel> labels = new HashMap<>();
  private List<Map<IFeature, Double>> bags = new ArrayList<>();

  public CINSample(
      List<List<String>> pFeatureIndex,
      List<String> pLabelIndex,
      ZipFile pSource,
      ZipEntry pEntry,
      FeatureRegistry pRegistry) {
    featureIndex = pFeatureIndex;
    labelIndex = pLabelIndex;
    source = pSource;
    entry = pEntry;
    registry = pRegistry;
  }

  private void decode() throws IOException {
    if(!bags.isEmpty()) return;
    InputStream stream = source.getInputStream(entry);

    CINDecodingResult result = CINDecoder.decode(
       stream, featureIndex, registry, labelIndex
    );

    bags = result.getBags();
    labels = result.getLabels();

    stream.close();
  }


  @Override
  public String getID() {
    String name = entry.getName();
    name = name.substring(0, name.length()-3);
    return name;
  }

  @Override
  public int getMaxIteration() {
    return featureIndex.size();
  }

  @Override
  public int getMaxASTDepth() {
    return 5;
  }

  @Override
  public void assignLabel(String labelId, boolean correct, double time) {
    try {
      decode();
    } catch (IOException pE) {}
    labels.put(labelId, new ProgramLabel(correct, time));
  }

  @Override
  public boolean isCorrect(String labelId) {
    try {
      decode();
    } catch (IOException pE) {}
    if(labels.containsKey(labelId)){
      return labels.get(labelId).isCorrectSolved();
    }
    throw new NoSuchElementException();
  }

  @Override
  public double getTime(String labelId) {
    try {
      decode();
    } catch (IOException pE) {}
    if(labels.containsKey(labelId)){
      return labels.get(labelId).getTime();
    }
    throw new NoSuchElementException();
  }

  @Override
  public boolean isBetter(String labelId1, String labelId2) {
    try {
      decode();
    } catch (IOException pE) {}
    return ((isCorrect(labelId1)&&!isCorrect(labelId2)) || (isCorrect(labelId1)==isCorrect(labelId2) && (getTime(labelId1) < getTime(labelId2))));
  }

  @Override
  public Set<String> getDefinedLabels() {
    return new HashSet<>(labelIndex);
  }

  @Override
  public Map<IFeature, Double> getFeatureBag(
      int iteration) {
    try {
      decode();
    } catch (IOException pE) {}
    if(iteration > getMaxIteration()){
      return new HashMap<>();
    }

    return bags.get(iteration);
  }



}

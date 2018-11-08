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

public class OnePassCINSample implements IProgramSample {

  private String name;
  private List<List<String>> featureIndex;
  private List<String> labelIndex;
  private FeatureRegistry registry;

  private Map<String, ProgramLabel> labels = new HashMap<>();
  private List<Map<IFeature, Double>> bags = new ArrayList<>();

  public OnePassCINSample(
      List<List<String>> pFeatureIndex,
      List<String> pLabelIndex,
      String pName,
      InputStream pStream,
      FeatureRegistry pRegistry) throws IOException {
    featureIndex = pFeatureIndex;
    labelIndex = pLabelIndex;
    registry = pRegistry;
    name = pName;
    decode(pStream);
  }



  private void decode(InputStream pStream) throws IOException {
    if(!bags.isEmpty()) return;
    InputStream stream = pStream;
    CINDecodingResult result = CINDecoder.decode(
        stream, featureIndex, registry, labelIndex
    );

    bags = result.getBags();
    labels = result.getLabels();
  }


  @Override
  public String getID() {
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
    return new HashSet<>(labelIndex);
  }

  @Override
  public Map<IFeature, Double> getFeatureBag(
      int iteration) {
    return bags.get(iteration);
  }



}

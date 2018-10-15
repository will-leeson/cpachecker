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

import java.util.Map;
import java.util.Set;

public interface IProgramSample {

  public String getID();

  public int getMaxIteration();

  public int getMaxASTDepth();

  public void assignLabel(String labelId, boolean correct, double time);

  public boolean isCorrect(String labelId);

  public double getTime(String labelId);

  public boolean isBetter(String labelId1, String labelId2);

  public Set<String> getDefinedLabels();

  public Map<IFeature, Double> getFeatureBag(int iteration);

}

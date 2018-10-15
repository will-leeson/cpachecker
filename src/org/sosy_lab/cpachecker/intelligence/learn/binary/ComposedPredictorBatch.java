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
package org.sosy_lab.cpachecker.intelligence.learn.binary;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.util.Pair;

public class ComposedPredictorBatch implements IPredictorBatch {

  private List<IBinaryPredictor> predictors;
  private List<Pair<String, String>> labels;

  public ComposedPredictorBatch(
      List<IBinaryPredictor> pPredictors,
      List<Pair<String, String>> pLabels) {
    predictors = pPredictors;
    labels = pLabels;
  }

  @Override
  public Future<double[][]> predict(Iterable<IProgramSample> entities) {
    return null;
  }

  @Override
  public Pair<String, String> getLabelsByPosition(int i) {
    if(i >= labels.size() || i < 0)throw new NoSuchElementException();
    return labels.get(i);
  }
}

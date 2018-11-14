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

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.learn.binary.exception.IncompleteConfigurationException;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.PretrainedJaccPredictorBatch;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.PretrainedSVMPredictorBatch;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.util.Pair;

public class PredictorBatchBuilder {

  private SampleRegistry registry;
  private ShutdownNotifier notifier;
  private IBinaryPredictorType type;
  private List<Pair<String, String>> pairs;

  public PredictorBatchBuilder(
      IBinaryPredictorType pType,
      List<Pair<String, String>> pPairs) {
    type = pType;
    pairs = pPairs;
  }

  public PredictorBatchBuilder shutdownOn(ShutdownNotifier pShutdownNotifier){
    this.notifier = pShutdownNotifier;
    return this;
  }

  public PredictorBatchBuilder registry(SampleRegistry pSampleRegistry){
    this.registry = pSampleRegistry;
    return this;
  }


  public IPredictorBatch build() throws IncompleteConfigurationException {
    if(type instanceof LinearPretrainedType){
      LinearPretrainedType linear = (LinearPretrainedType)type;
      return new PretrainedSVMPredictorBatch(linear);
    }

    if(type instanceof JaccardPretrainedType){
      JaccardPretrainedType jaccard = (JaccardPretrainedType)type;

      if(registry == null)
        throw new IncompleteConfigurationException("A kernelized SVM needs the sample registry");


      return new PretrainedJaccPredictorBatch(registry, jaccard.getConfig(), notifier);
    }

    List<IBinaryPredictor> predictors = new ArrayList<>();
    for(Pair<String, String> p: pairs){
      predictors.add(type.instantiate(p.getFirst(), p.getSecond()));
    }

    return new ComposedPredictorBatch(predictors, pairs);

  }

}

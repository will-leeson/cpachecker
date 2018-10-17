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
import org.sosy_lab.cpachecker.intelligence.learn.binary.HyperSVMType.OptimizeFunctionType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.SVMType.KernelType;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;

public class SVMPredictorBuilder {

  private FeatureRegistry registry;
  private String optimizeC = null;
  private List<Integer> params = null;
  private double C = 1.0;
  private String kernel;
  private String pretrainedModel = null;
  private List<IProgramSample> trainSamples = null;
  private boolean pythonBackend = true;


  SVMPredictorBuilder(){}

  public SVMPredictorBuilder regularize(double pC){
    this.C = pC;
    return this;
  }

  public SVMPredictorBuilder tuneHyperparameter(String goalFunction, List<Integer> pParams){
    this.optimizeC = goalFunction;
    this.params = pParams;
    return this;
  }

  public SVMPredictorBuilder tuneHyperparameter(OptimizeFunctionType type, List<Integer> pParams){
    switch(type){
      case FSCORE:
        return tuneHyperparameter("f1", pParams);
      case PRECISION:
        return tuneHyperparameter("precision", pParams);
      case RECALL:
        return tuneHyperparameter("recall", pParams);
        default:
          return tuneHyperparameter("accuracy", pParams);
    }
  }

  public SVMPredictorBuilder kernel(String pKernel){
    this.kernel = pKernel;
    return this;
  }

  public SVMPredictorBuilder kernel(KernelType type){
    switch (type){
      case JACCARD:
        return kernel("jaccard");
      case RBF:
        return kernel("rbf");
      default:
        return kernel("linear");
    }
  }

  public SVMPredictorBuilder trainOn(List<IProgramSample> samples){
    this.trainSamples = samples;
    return this;
  }

  public SVMPredictorBuilder preload(String path){
    this.pretrainedModel = path;
    return this;
  }

  public SVMPredictorBuilder disablePythonBackend(){
    this.pythonBackend = false;
    return this;
  }

  public IBinaryPredictorType createType(){
    if(this.pretrainedModel == null || this.trainSamples == null){
      throw new IllegalArgumentException("Model is not configured");
    }
    if(this.pretrainedModel != null){
      if(this.kernel.equalsIgnoreCase("linear")){
        return new LinearPretrainedType(
            this.pretrainedModel
        );
      }else if(this.kernel.equalsIgnoreCase("jaccard")){
        return new JaccardPretrainedType(
            this.pretrainedModel
        );
      }
    }
    if(optimizeC != null){
      return new HyperSVMType(
          this.pythonBackend, this.kernel, this.optimizeC, this.params,
          this.pretrainedModel, this.trainSamples
      );
    }else{
      return new SVMType(
          this.pythonBackend, this.kernel, this.C, this.pretrainedModel, this.trainSamples
      );
    }
  }

}

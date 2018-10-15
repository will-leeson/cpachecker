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
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.JLinearSVM;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;

public class SVMType implements IBinaryPredictorType {

  public enum KernelType{
    LINEAR, RBF, JACCARD
  }


  private boolean pythonBackend = false;
  private String kernel = "linear";
  private double C = 1.0;

  private String pretrainedModel = null;
  private List<IProgramSample> trainingSamples;

  SVMType(
      boolean pPythonBackend,
      String pKernel,
      double pC,
      String pPretrainedModel,
      List<IProgramSample> pTrainingSamples) {
    pythonBackend = pPythonBackend;
    kernel = pKernel;
    C = pC;
    pretrainedModel = pPretrainedModel;
    trainingSamples = pTrainingSamples;
  }

  private boolean bindPython(){
    return false;
  }

  private IBinaryPredictor instantiateLinear(String label1, String label2){

    JLinearSVM svm = new JLinearSVM();

    if(pretrainedModel != null){
      svm.loadModel(pretrainedModel);
      return svm;
    }

    svm.trainModel(label1, label2, trainingSamples, C, false);

    return svm;
  }

  @Override
  public IBinaryPredictor instantiate(String label1, String label2) {

    if(!pythonBackend || !bindPython()){
      if(kernel.equals("linear")){
        return instantiateLinear(label1, label2);
      }
    }


    return null;
  }

}

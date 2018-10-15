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
package org.sosy_lab.cpachecker.intelligence.learn.binary.impl;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.ParameterSearchResult;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.sosy_lab.cpachecker.intelligence.learn.binary.IBinaryPredictor;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;

public class JLinearSVM implements IBinaryPredictor {


  private Model model;

  public boolean trainModel(String label1, String label2,
                            Iterable<IProgramSample> trainData,
                            double C, boolean optimizeC){

    int n = 0;
    int l = 0;

    List<List<Feature>> X = new ArrayList<>();
    List<Double> Y = new ArrayList<>();

    for(IProgramSample sample: trainData){

      Set<String> labels = sample.getDefinedLabels();

      if(!labels.contains(label1) || !labels.contains(label2))
        continue;

      Y.add(sample.isBetter(label1, label2)?1.0:0.0);

      l++;
      List<Feature> features = new ArrayList<>();
      X.add(features);
      for(int i = 0; i < sample.getMaxIteration(); i++){
        for(Entry<IFeature, Double> e: sample.getFeatureBag(i).entrySet()){
          n = Math.max(e.getKey().getFeatureId(), n);
          features.add(new FeatureNode(e.getKey().getFeatureId(), e.getValue()));
        }
      }
    }

    Feature[][] x = new Feature[l][];

    for(int i = 0; i < X.size(); i++){
      x[i] = new Feature[X.get(i).size()];
      for(int j = 0; j < X.get(i).size(); j++){
        x[i][j] = X.get(i).get(j);
      }
    }

    double[] y = new double[Y.size()];

    for(int i = 0; i < Y.size(); i++){
      y[i] = Y.get(i);
    }


    Problem problem = new Problem();
    problem.l = l;
    problem.n = n;
    problem.x = x;
    problem.y = y;

    SolverType type = SolverType.L2R_L2LOSS_SVC_DUAL;

    Parameter param = new Parameter(type, C, 0.01);

    if(optimizeC){
      ParameterSearchResult result = Linear.findParameterC(
          problem, param, 10, C, 1000
      );
      C = result.getBestC();
    }

    this.model = Linear.train(problem, param);
    return true;
  }


  public boolean loadModel(String filePath){
    try {
      model = Model.load(new File(filePath));
      return true;
    } catch (IOException pE) {
      pE.printStackTrace();
    }
    return false;
  }


  @Override
  public Future<double[]> predict(Iterable<IProgramSample> entities) {

    List<Double> prediction = new ArrayList<>();

    for(IProgramSample sample : entities){
      List<Feature> features = new ArrayList<>();
      for(int i = 0; i < sample.getMaxIteration(); i++){
        for(Entry<IFeature, Double> e: sample.getFeatureBag(i).entrySet()){
          if(e.getKey().getFeatureId() >= model.getNrFeature())continue;
          features.add(new FeatureNode(e.getKey().getFeatureId(), e.getValue()));
        }
      }
      Feature[] F = new Feature[features.size()];
      F = features.toArray(F);
      prediction.add(Linear.predict(this.model, F));
    }

    double[] P = new double[prediction.size()];
    for(int i = 0; i < P.length; i++)
      P[i] = prediction.get(i);

    return new Future<double[]>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public double[] get() throws InterruptedException, ExecutionException {
        return P;
      }

      @Override
      public double[] get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return P;
      }
    };
  }

  @Override
  public boolean save(String path) {
    try {
      model.save(new File(path));
      return true;
    } catch (IOException pE) {
      pE.printStackTrace();
    }
    return false;
  }
}

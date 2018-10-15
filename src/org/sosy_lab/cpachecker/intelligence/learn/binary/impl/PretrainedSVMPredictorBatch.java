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

import com.google.common.collect.Table.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.sosy_lab.cpachecker.intelligence.learn.binary.IPredictorBatch;
import org.sosy_lab.cpachecker.intelligence.learn.binary.LinearPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.Matrix;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.MathEngine;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.SparseVector;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.util.Pair;


public class PretrainedSVMPredictorBatch implements IPredictorBatch {

  private LinearPretrainedType type;
  private List<Pair<String, String>> labels;

  public PretrainedSVMPredictorBatch(
      LinearPretrainedType pType) {
    type = pType;

    labels = new ArrayList<>(type.getMapCombination().size());

    for(Cell<String, String, Integer> cell: type.getMapCombination().cellSet()){
      labels.add(cell.getValue(), Pair.of(cell.getRowKey(), cell.getColumnKey()));
    }

  }

  @Override
  public Future<double[][]> predict(Iterable<IProgramSample> entities) {
    FutureTask<double[][]> task = new FutureTask<>(new BatchPrediction(entities));
    task.run();
    return task;
  }

  @Override
  public Pair<String, String> getLabelsByPosition(int i) {
    if(i >= labels.size() || i < 0)throw new NoSuchElementException();
    return labels.get(i);
  }

  private class BatchPrediction implements Callable<double[][]> {

    private Iterable<IProgramSample> entities;

    public BatchPrediction(Iterable<IProgramSample> pEntities) {
      entities = pEntities;
    }


    @Override
    public double[][] call() throws Exception {
      List<double[]> predictions = new ArrayList<>();

      Matrix alphaMatrix = type.getAlpha();

      for(IProgramSample sample: entities){

        SparseVector vector = new SparseVector(alphaMatrix.getCols());
        for(Entry<IFeature, Double> e: sample.getFeatureBag(0).entrySet()){
          String fName = e.getKey().getFeatureName();

          if(!type.getMapFeatures().containsKey(fName))
            continue;

          vector.set(type.getMapFeatures().get(fName), e.getValue());
        }

        vector.set(alphaMatrix.getCols() - 1, 1.0);

        try {
          predictions.add(
              type.getEngine().sign(type.getEngine().dot(alphaMatrix, vector)).toArray());
        } catch (Exception pE) {
          pE.printStackTrace();
        }
      }

      double[][] P = new double[predictions.size()][];
      return predictions.toArray(P);
    }
  }

}

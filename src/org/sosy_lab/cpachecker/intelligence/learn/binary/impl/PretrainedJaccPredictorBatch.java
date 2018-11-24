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

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.learn.binary.IPredictorBatch;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.jaccard.SeqKernelComputationProcess;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.jaccard.planning.PlannedComputationProcess;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.Vector;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.RealProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.util.Pair;

public class PretrainedJaccPredictorBatch implements IPredictorBatch {

  private SampleRegistry registry;
  private ShutdownNotifier notifier;
  private Table<String, String, KernelCoef> config;
  private List<Pair<String, String>> pos = new ArrayList<>();

  public PretrainedJaccPredictorBatch(
      SampleRegistry pRegistry,
      Table<String, String, KernelCoef> pConfig,
      ShutdownNotifier pShutdownNotifier) {
    registry = pRegistry;
    config = pConfig;
    notifier = pShutdownNotifier;
    buildPos();
  }

  private void buildPos(){
    for(Cell<String, String, KernelCoef> cell: config.cellSet())
      pos.add(Pair.of(cell.getRowKey(), cell.getColumnKey()));
  }


  @Override
  public Future<double[][]> predict(Iterable<IProgramSample> entities) {
    return new PredictionProcess(entities).fork();
  }

  @Override
  public Pair<String, String> getLabelsByPosition(int i) {
    if(i >= pos.size() || i < 0)throw new NoSuchElementException();
    return pos.get(i);
  }

  private class PredictionProcess extends RecursiveTask<double[][]> {

    private Iterable<IProgramSample> entities;

    public PredictionProcess(Iterable<IProgramSample> pEntities) {
      entities = pEntities;
    }

    @Override
    public double[][] compute(){
      List<Vector> predictions = new ArrayList<>();

      for(IProgramSample sample: entities){

        predictions.add(
            new SeqKernelComputationProcess(registry, notifier, config, (RealProgramSample)sample, pos).compute()
        );


        if(notifier != null) {
          try {
            notifier.shutdownIfNecessary();
          } catch (InterruptedException pE) {
            return new double[0][];
          }
        }
      }

      double[][] p = new double[predictions.size()][];

      for(int i = 0;  i < predictions.size(); i++){
          p[i] = predictions.get(i).toArray();
      }

      return p;
    }
  }

}

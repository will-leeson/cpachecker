// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.learn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class HeuristicPerformanceModel implements IPerformanceModel {

  private int[] numSeenRuns = new int[2];

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void nextRound() {
    numSeenRuns = new int[2];
  }

  @Override
  public void learnPath(int algorithmId, List<String> pathTokens) {
      numSeenRuns[algorithmId] += 1;
      return;
  }

  @Override
  public List<Double> predictPerformance(List<String> pathTokens) {

    List<Double> outputList = new ArrayList<>(2);

    double[] output = new double[2];
    Set<String> tokens = new HashSet<>(pathTokens);

    for(String token : tokens){

      if(token.contains("UNUSED_VAR")){

        output[0] += 1;

        if(token.contains("LOOP")){
          continue;
        }
      } else if(token.contains("LOOP")){
        output[1] += 4;
      }

    }

    double[] prior = new double[2];
    int norm = IntStream.of(numSeenRuns).sum() + 2;

    for(int i = 0; i < numSeenRuns.length; i++){
        prior[i] = (numSeenRuns[i] + 1) / (double) norm;
    }

    double sum = DoubleStream.of(output).sum();

    for(int i = 0; i < output.length; i++){
      output[i] = prior[i] * (output[i] / sum);
    }

    double finalNorm = DoubleStream.of(output).sum();

    for(int i = 0; i < output.length; i++){
      outputList.add(output[i] / finalNorm);
    }

    return outputList;
  }
}

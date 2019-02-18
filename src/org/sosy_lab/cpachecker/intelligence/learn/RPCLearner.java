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
package org.sosy_lab.cpachecker.intelligence.learn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.intelligence.learn.binary.IPredictorBatch;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.util.Pair;

public class RPCLearner implements IRankLearner {

  private IPredictorBatch batch;

  public RPCLearner(IPredictorBatch pBatch) {
    batch = pBatch;
  }

  @Override
  public List<List<String>> predict(Iterable<IProgramSample> pSample) {

    try {
      double[][] prediction = batch.predict(pSample).get();

      if(prediction.length < 1)
        return new ArrayList<>();

      List<List<String>> rankings = new ArrayList<>();

      for(int i = 0; i < prediction.length; i++){
        Map<String, Double> voting = new HashMap<>();

        for(int j = 0; j < prediction[i].length; j++) {
          Pair<String, String> labels = batch.getLabelsByPosition(j);
          String l1 = labels.getFirst();
          String l2 = labels.getSecond();

          double pred = Math.max(0, prediction[i][j]);

          if(!voting.containsKey(l1)){
            voting.put(l1, 0.0);
          }
          voting.put(l1, voting.get(l1) + pred);

          if(!voting.containsKey(l2)){
            voting.put(l2, 0.0);
          }
          voting.put(l2, voting.get(l2) + (1.0 - pred));
        }

        rankings.add(voting.entrySet().stream()
                            .sorted(Collections.reverseOrder(
                                Map.Entry.comparingByValue()
                            )).map(x -> x.getKey())
                            .collect(Collectors.toList()));

      }

      return rankings;

    } catch (InterruptedException pE) {
      System.out.println(pE.getMessage());
    } catch (ExecutionException pE) {
      pE.printStackTrace();
    }

    return new ArrayList<>();
  }
}

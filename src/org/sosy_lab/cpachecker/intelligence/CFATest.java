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
package org.sosy_lab.cpachecker.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.learn.IRankLearner;
import org.sosy_lab.cpachecker.intelligence.learn.RPCLearner;
import org.sosy_lab.cpachecker.intelligence.learn.binary.IPredictorBatch;
import org.sosy_lab.cpachecker.intelligence.learn.binary.LinearPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.PredictorBatchBuilder;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.WLFeatureModel;

public class CFATest {

  public static void test(CFA cfa) {

    SampleRegistry registry = new SampleRegistry(
        new FeatureRegistry(), 1, 5
    );

    IProgramSample sample = registry.registerSample("defaultId", cfa);

    PredictorBatchBuilder batchBuilder = new PredictorBatchBuilder(
        new LinearPretrainedType(null), null
    );
    IPredictorBatch batch = batchBuilder.build();
    IRankLearner learner = new RPCLearner(batch);

    List<IProgramSample> sampleList = new ArrayList<>();
    sampleList.add(sample);

    System.out.println(learner.predict(sampleList));


  }

}

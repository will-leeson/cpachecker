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
package org.sosy_lab.cpachecker.intelligence.oracle.predictor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.learn.IRankLearner;
import org.sosy_lab.cpachecker.intelligence.learn.RPCLearner;
import org.sosy_lab.cpachecker.intelligence.learn.binary.LinearPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.PredictorBatchBuilder;
import org.sosy_lab.cpachecker.intelligence.learn.binary.exception.IncompleteConfigurationException;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleStatistics;

@Options(prefix="linearPredictor")
public class LinearLabelPredictor implements IOracleLabelPredictor {

  @Option(secure = true,
      description = "pretrained parameter of jaccard SVM")
  private String pretrained = null;

  private LogManager logger;
  private OracleStatistics statistics = new OracleStatistics(getName()+" Oracle");
  private IProgramSample currentSample;


  public LinearLabelPredictor(
      LogManager pLogger,
      Configuration pConfiguration,
      IProgramSample pCurrentSample) throws InvalidConfigurationException {
    pConfiguration.inject(this);
    logger = pLogger;
    currentSample = pCurrentSample;
  }

  @Override
  public String getName() {
    return "Linear";
  }

  @Override
  public List<String> ranking() {
    statistics.reset();
    PredictorBatchBuilder batchBuilder = new PredictorBatchBuilder(
        new LinearPretrainedType(pretrained), null
    );

    List<IProgramSample> samples = Arrays.asList(currentSample);

    IRankLearner learner = null;
    try {
      learner = new RPCLearner(batchBuilder.build());
    } catch (IncompleteConfigurationException pE) {
      logger.log(Level.WARNING, pE, "Use random sequence");
      return new ArrayList<>();
    }

    List<String> out = learner.predict(samples).get(0);

    logger.log(Level.INFO, "Predicted ranking: "+out.toString());

    statistics.setOrder(out);
    statistics.stopTime();
    return out;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}

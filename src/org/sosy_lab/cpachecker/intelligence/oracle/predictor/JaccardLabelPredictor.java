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
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.learn.IRankLearner;
import org.sosy_lab.cpachecker.intelligence.learn.RPCLearner;
import org.sosy_lab.cpachecker.intelligence.learn.binary.JaccardPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.PredictorBatchBuilder;
import org.sosy_lab.cpachecker.intelligence.learn.binary.exception.IncompleteConfigurationException;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleStatistics;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

@Options(prefix="jaccardPredictor")
public class JaccardLabelPredictor implements IOracleLabelPredictor {

  @Option(secure = true,
      description = "pretrained parameter of jaccard SVM")
  private String pretrained = null;

  @Option(secure = true,
          description = "timeout for prediction")
  private int timeout = -1;

  private LogManager logger;
  private SampleRegistry registry;
  private ShutdownNotifier shutdownNotifier;
  private ShutdownManager ownManager;
  private IProgramSample currentSample;
  private ShutdownRequestListener logShutdownListener;

  private OracleStatistics statistics = new OracleStatistics(getName()+ " Oracle");

  public JaccardLabelPredictor(
      Configuration pConfiguration,
      LogManager pLogger,
      SampleRegistry pRegistry,
      ShutdownNotifier pShutdownNotifier,
      IProgramSample pCurrentSample) throws InvalidConfigurationException {
    pConfiguration.inject(this);
    logger = pLogger;
    registry = pRegistry;
    ownManager = ShutdownManager.createWithParent(pShutdownNotifier);
    shutdownNotifier = ownManager.getNotifier();
    currentSample = pCurrentSample;
  }

  private void enableTimeout(){
    if(timeout < 0)return;

    logShutdownListener =
        reason ->
            logger.logf(
                Level.WARNING,
                "Shutdown of jaccard predictor requested (%s).",
                reason);
    shutdownNotifier.register(logShutdownListener);

    try {
      Configuration configuration = Configuration.builder()
                                    .setOption("limits.time.cpu", timeout+"s")
                                    .setOption("limits.time.cpu::required", timeout+"s")
                                    .build();
      ResourceLimitChecker checker = ResourceLimitChecker.fromConfiguration(configuration, logger, ownManager);
      checker.start();
    } catch (InvalidConfigurationException pE) {
    }

  }

  @Override
  public String getName() {
    return "Jaccard";
  }

  @Override
  public List<String> ranking() {
    enableTimeout();
    statistics.reset();
    logger.log(Level.INFO, "Start precise ranking... This can take some time.");

    PredictorBatchBuilder batchBuilder = new PredictorBatchBuilder(
        new JaccardPretrainedType(pretrained), null
    );

    List<IProgramSample> samples = Arrays.asList(currentSample);

    IRankLearner learner = null;
    try {
      learner = new RPCLearner(batchBuilder
          .registry(registry)
          .shutdownOn(shutdownNotifier)
          .build());
    } catch (IncompleteConfigurationException pE) {
      logger.log(Level.WARNING, pE, "Use random sequence");
      return new ArrayList<>();
    }
    List<List<String>> predictions = learner.predict(samples);

    if(logShutdownListener != null)
      shutdownNotifier.unregister(logShutdownListener);

    if(predictions.size() < 1){
      logger.log(Level.WARNING, "Oracle stopped as prediction is empty");
      return null;
    }

    List<String> out = predictions.get(0);

    if(out.get(out.size() - 1 ).equalsIgnoreCase("Unknown")){
      out.remove(out.size() - 1);
    }

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

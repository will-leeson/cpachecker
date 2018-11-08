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
package org.sosy_lab.cpachecker.intelligence.oracle.ranking;

import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleFactory;
import org.sosy_lab.cpachecker.intelligence.oracle.predictor.IOracleLabelPredictor;
import org.sosy_lab.cpachecker.intelligence.oracle.predictor.PredictorFactory;

public class PackedPredictorFactory {

  private LogManager logger;
  private Configuration config;
  private ShutdownNotifier pShutdownNotifier;
  private List<AnnotatedValue<Path>> configPaths;
  private SampleRegistry pSampleRegistry;
  private CFA pCFA;
  private PredictorFactory factory;

  public PackedPredictorFactory(
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pPShutdownNotifier,
      List<AnnotatedValue<Path>> pConfigPaths,
      SampleRegistry pPSampleRegistry,
      CFA pPCFA,
      PredictorFactory pFactory) {
    logger = pLogger;
    config = pConfig;
    pShutdownNotifier = pPShutdownNotifier;
    configPaths = pConfigPaths;
    pSampleRegistry = pPSampleRegistry;
    pCFA = pPCFA;
    factory = pFactory;
  }

  public IOracleLabelPredictor create(String oracle) throws InvalidConfigurationException {
    return factory.create(oracle, logger, config, pShutdownNotifier, configPaths, pSampleRegistry, pCFA);
  }

  public LogManager getLogger() {
    return logger;
  }

  public Configuration getConfig() {
    return config;
  }

  public ShutdownNotifier getpShutdownNotifier() {
    return pShutdownNotifier;
  }

  public List<AnnotatedValue<Path>> getConfigPaths() {
    return configPaths;
  }

  public SampleRegistry getpSampleRegistry() {
    return pSampleRegistry;
  }

  public CFA getpCFA() {
    return pCFA;
  }

  public PredictorFactory getFactory() {
    return factory;
  }

}

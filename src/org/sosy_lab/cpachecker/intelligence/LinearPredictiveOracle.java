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

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.learn.IRankLearner;
import org.sosy_lab.cpachecker.intelligence.learn.RPCLearner;
import org.sosy_lab.cpachecker.intelligence.learn.binary.LinearPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.PredictorBatchBuilder;
import org.sosy_lab.cpachecker.intelligence.learn.binary.exception.IncompleteConfigurationException;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;

@Options(prefix="linearOracle")
public class LinearPredictiveOracle implements IConfigOracle {

  @Option(secure=true,
          description = "file path to label-path mapping")
  private String labelPath = null;

  @Option(secure = true,
          description = "pretrained parameter of linear SVM")
  private String pretrained = null;


  private Map<String, AnnotatedValue<Path>> labelToPath;
  private IProgramSample currentSample;
  private LogManager logger;
  private ShutdownNotifier shutdownNotifier;


  private OracleStatistics stats;
  private List<String> labelRanking;
  private List<AnnotatedValue<Path>> unknown = new ArrayList<>();
  private int pos = 0;


  public LinearPredictiveOracle(LogManager pLogger, Configuration config, ShutdownNotifier pShutdownNotifier, List<AnnotatedValue<Path>> configPaths, CFA pCFA)
      throws InvalidConfigurationException {

    SampleRegistry registry = new SampleRegistry(
        new FeatureRegistry(), 0, 5
    );

    init(pLogger, config, pShutdownNotifier, configPaths, registry.registerSample("randId", pCFA));
  }


  LinearPredictiveOracle(LogManager pLogger, Configuration config, ShutdownNotifier pShutdownNotifier, List<AnnotatedValue<Path>> configPaths, IProgramSample pSample)
      throws InvalidConfigurationException {

    init(pLogger, config, pShutdownNotifier, configPaths, pSample);
  }

  private void init(LogManager pLogger, Configuration pConfiguration, ShutdownNotifier pShutdownNotifier, List<AnnotatedValue<Path>> configPaths, IProgramSample pSample)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);

    this.shutdownNotifier = pShutdownNotifier;

    stats = new OracleStatistics("Linear Oracle");

    this.logger = pLogger;

    initLabelToPath(configPaths);

    currentSample = pSample;
  }

  private void initLabelToPath(List<AnnotatedValue<Path>> list){
    //TODO: Support loading

    if(labelPath == null) {
      Map<String, String> revLabel = new HashMap<>();
      revLabel.put("svcomp18--01-valueAnalysis.properties", "VA-NoCegar");
      revLabel.put("svcomp18--02-valueAnalysis-itp.properties", "VA-Cegar");
      revLabel.put("svcomp18--03-predicateAnalysis.properties", "PA");
      revLabel.put("svcomp18--04-kInduction.properties", "KI");
      revLabel.put("svcomp18--recursion.properties", "BAM");
      revLabel.put("svcomp18--bmc.properties", "BMC");

      labelToPath = new HashMap<>();

      for(AnnotatedValue<Path> p: list){

        String n = p.value().getFileName().toString();

        if(!revLabel.containsKey(n)){
          unknown.add(p);
          continue;
        }

        labelToPath.put(revLabel.get(n), p);
      }
    }

  }

  private void initRanking() throws InterruptedException {
    if(labelRanking != null)return;

    long time = System.currentTimeMillis();

    labelRanking = new ArrayList<>();
    stats.setOrder(labelRanking);

    PredictorBatchBuilder batchBuilder = new PredictorBatchBuilder(
        new LinearPretrainedType(pretrained), null
    );

    List<IProgramSample> samples = Arrays.asList(currentSample);

    IRankLearner learner = null;
    try {
      learner = new RPCLearner(batchBuilder.build());
    } catch (IncompleteConfigurationException pE) {
      logger.log(Level.WARNING, pE, "Use random sequence");
      labelRanking = new ArrayList<>(labelToPath.keySet());
    }

    this.shutdownNotifier.shutdownIfNecessary();

    labelRanking = learner.predict(samples).get(0);
    stats.setOrder(labelRanking);

    logger.log(Level.INFO, "Finished ranking after "+ (System.currentTimeMillis() - time)+ "ms");
    logger.log(Level.INFO, "Predicted ranking: "+labelRanking.toString());
    stats.stopTime();
  }


  private AnnotatedValue<Path> get(int i) throws InterruptedException {
    initRanking();
    if(i >= labelRanking.size()){
      i = i - labelRanking.size();
      if(i < unknown.size()){
        return unknown.get(i);
      }
    }else{
      String n = labelRanking.get(i);
      if(!labelToPath.containsKey(n)){
        logger.log(Level.INFO, "Unknown label "+n+". Skip.");
        return get(++i);
      }

      return labelToPath.get(n);
    }
    throw new NoSuchElementException();
  }

  @Override
  public AnnotatedValue<Path> peek() {
    try {
      return get(pos);
    } catch (InterruptedException pE) {
      throw new NoSuchElementException(pE.getMessage());
    }
  }


  @Override
  public boolean hasNext() {
    try {
      initRanking();
    } catch (InterruptedException pE) {
      return false;
    }
    return pos < labelRanking.size() + unknown.size();
  }

  @Override
  public AnnotatedValue<Path> next() {
    try {
      return get(pos++);
    } catch (InterruptedException pE) {
      throw new NoSuchElementException(pE.getMessage());
    }
  }

  @Override
  public void remove() {
    throw new IllegalStateException();
  }

  @Override
  public void precomputeOracle(Consumer<IConfigOracle> callback) {
    try {
      initRanking();
      callback.accept(this);
    } catch (InterruptedException pE) {
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

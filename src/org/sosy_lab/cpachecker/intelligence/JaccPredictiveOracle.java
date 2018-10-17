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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.intelligence.learn.binary.JaccardPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.LinearPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.PredictorBatchBuilder;
import org.sosy_lab.cpachecker.intelligence.learn.binary.exception.IncompleteConfigurationException;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;

@Options(prefix="jaccOracle")
public class JaccPredictiveOracle implements IConfigOracle {
  @Option(secure=true,
      description = "file path to label-path mapping")
  private String labelPath = null;

  @Option(secure = true,
      description = "pretrained parameter of linear SVM")
  private String pretrained = null;


  private SampleRegistry registry;
  private Map<String, AnnotatedValue<Path>> labelToPath;
  private IProgramSample currentSample;
  private LogManager logger;


  private OracleStatistics stats;
  private List<String> labelRanking;
  private List<AnnotatedValue<Path>> unknown = new ArrayList<>();
  private int pos = 0;



  public JaccPredictiveOracle(LogManager pLogger, Configuration config, List<AnnotatedValue<Path>> configPaths, CFA pCFA)
      throws InvalidConfigurationException {

    registry = new SampleRegistry(
        new FeatureRegistry(), 0, 5
    );

    init(pLogger, config, configPaths, registry.registerSample("randId", pCFA));
  }


  JaccPredictiveOracle(LogManager pLogger, Configuration config, List<AnnotatedValue<Path>> configPaths, IProgramSample pSample,
                       SampleRegistry pSampleRegistry)
      throws InvalidConfigurationException {

    registry = pSampleRegistry;
    init(pLogger, config, configPaths, pSample);
  }

  private void init(LogManager pLogger, Configuration pConfiguration, List<AnnotatedValue<Path>> configPaths, IProgramSample pSample)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);

    stats = new OracleStatistics("Jaccard Oracle");

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

  private void initRanking(){
    if(labelRanking != null)return;

    logger.log(Level.INFO, "Start precise ranking... This can take some time.");

    long time = System.currentTimeMillis();

    PredictorBatchBuilder batchBuilder = new PredictorBatchBuilder(
        new JaccardPretrainedType(pretrained), null
    );

    List<IProgramSample> samples = Arrays.asList(currentSample);

    IRankLearner learner = null;
    try {
      learner = new RPCLearner(batchBuilder
          .registry(registry)
          .build());
    } catch (IncompleteConfigurationException pE) {
      logger.log(Level.WARNING, pE, "Use random sequence");
      labelRanking = new ArrayList<>(labelToPath.keySet());
    }
    labelRanking = learner.predict(samples).get(0);
    stats.setOrder(labelRanking);

    logger.log(Level.INFO, "Finished ranking after "+ (System.currentTimeMillis() - time)+ "ms");
    logger.log(Level.INFO, "Predicted ranking: "+labelRanking.toString());
    stats.stopTime();
  }


  private AnnotatedValue<Path> get(int i){
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
    return get(pos);
  }

  @Override
  public boolean hasNext() {
    initRanking();
    return pos < labelRanking.size() + unknown.size();
  }

  @Override
  public AnnotatedValue<Path> next() {
    return get(pos++);
  }

  @Override
  public void remove() {
    throw new IllegalStateException();
  }

  @Override
  public void precomputeOracle(Consumer<IConfigOracle> callback) {
    initRanking();
    callback.accept(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

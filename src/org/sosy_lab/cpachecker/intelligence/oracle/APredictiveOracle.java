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
package org.sosy_lab.cpachecker.intelligence.oracle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;

@Options(prefix = "abstractPredictiveOracle")
public abstract class APredictiveOracle implements IConfigOracle {
  @Option(secure = true,
      description = "file path to label-path mapping")
  private String labelPath = null;

  protected SampleRegistry registry;
  private Map<String, AnnotatedValue<Path>> labelToPath;
  protected IProgramSample currentSample;
  protected LogManager logger;
  protected ShutdownNotifier shutdownNotifier;


  private OracleStatistics stats;
  private List<String> labelRanking;
  private List<AnnotatedValue<Path>> unknown = new ArrayList<>();
  private int pos = 0;


  public APredictiveOracle(
      LogManager pLogger,
      Configuration config,
      ShutdownNotifier pShutdownNotifier,
      List<AnnotatedValue<Path>> configPaths,
      CFA pCFA)
      throws InvalidConfigurationException {

    registry = new SampleRegistry(
        new FeatureRegistry(), 1, 5
    );

    init(pLogger, config, pShutdownNotifier, configPaths, registry.registerSample("randId", pCFA));
  }


  APredictiveOracle(
      LogManager pLogger,
      Configuration config,
      ShutdownNotifier pShutdownNotifier,
      List<AnnotatedValue<Path>> configPaths,
      IProgramSample pSample,
      SampleRegistry pSampleRegistry)
      throws InvalidConfigurationException {

    registry = pSampleRegistry;
    init(pLogger, config, pShutdownNotifier, configPaths, pSample);
  }

  public abstract String getName();

  public Set<String> getAvailableLabels(){
    Set<String> out = new HashSet<>(labelToPath.keySet());
    out.add("UNKNOWN");
    return out;
  }

  private void init(
      LogManager pLogger,
      Configuration pConfiguration,
      ShutdownNotifier pShutdownNotifier,
      List<AnnotatedValue<Path>> configPaths,
      IProgramSample pSample)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);

    this.shutdownNotifier = pShutdownNotifier;

    stats = new OracleStatistics(getName());

    this.logger = pLogger;

    initLabelToPath(configPaths);

    currentSample = pSample;
  }

  private void initLabelToPath(List<AnnotatedValue<Path>> list) {
    if (labelPath == null) {
      labelToPath = OracleFactory.initLabelToPath(list);

      for (AnnotatedValue<Path> l : list) {
        boolean knownPath = false;
        for (AnnotatedValue<Path> labelled : labelToPath.values()) {
          knownPath |= (l.equals(labelled));
        }
        if (!knownPath) unknown.add(l);
      }
    }

  }

  private void initRanking() {
    if (labelRanking != null) return;

    logger.log(Level.INFO, "Start "+getName()+" ranking...");

    long time = System.currentTimeMillis();

    labelRanking = new ArrayList<>();
    stats.setOrder(labelRanking);

    labelRanking = initRankingImpl();
    stats.setOrder(labelRanking);

    logger.log(Level.INFO, "Finished ranking after " + (System.currentTimeMillis() - time) + "ms");
    logger.log(Level.INFO, "Predicted ranking: " + labelRanking.toString());
    stats.stopTime();
  }

  protected abstract List<String> initRankingImpl();



  private AnnotatedValue<Path> handleUnknown(int i, String label){
    if(label.equalsIgnoreCase("unknown")){
      Path p = Paths.get("SKIP");
      AnnotatedValue<Path> annotatedValue = AnnotatedValue.create(p, "shutdownAfter");
      return annotatedValue;
    }

    logger.log(Level.INFO, "Unknown label " + label + ". Skip.");
    return get(++i);
  }


  private AnnotatedValue<Path> get(int i) {
    initRanking();
    if (i >= labelRanking.size()) {
      i = i - labelRanking.size();
      if (i < unknown.size()) {
        return unknown.get(i);
      }
    } else {
      String n = labelRanking.get(i);
      if (!labelToPath.containsKey(n)) {
        return handleUnknown(i, n);
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
    if (labelRanking.size() > 0)
      callback.accept(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

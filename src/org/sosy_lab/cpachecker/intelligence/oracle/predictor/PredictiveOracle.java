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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.oracle.IConfigOracle;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleFactory;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleStatistics;

@Options(prefix = "oracle")
public class PredictiveOracle implements IConfigOracle {

  private static final String[] DEFAULT_RANKING = new String[]{
      "VA-NoCegar", "VA-Cegar", "PA", "KI", "BAM"
  };

  @Option(secure = true,
      description = "file path to label-path mapping")
  private String labelPath = null;

  protected LogManager logger;
  private Map<String, AnnotatedValue<Path>> labelToPath;
  private IOracleLabelPredictor predictor;


  private OracleStatistics stats;
  private List<String> labelRanking;
  private List<AnnotatedValue<Path>> unknown = new ArrayList<>();
  private int pos = 0;

  public PredictiveOracle(
      LogManager pLogger,
      Configuration config,
      IOracleLabelPredictor pPredictor,
      List<AnnotatedValue<Path>> configPaths)
      throws InvalidConfigurationException {

    predictor = pPredictor;
    init(pLogger, config, configPaths);
  }


  private void init(
      LogManager pLogger,
      Configuration pConfiguration,
      List<AnnotatedValue<Path>> configPaths)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);

    stats = new OracleStatistics(getName());

    this.logger = pLogger;

    initLabelToPath(configPaths);
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

  public Set<String> getAvailableLabels(){
    Set<String> out = new HashSet<>(labelToPath.keySet());
    out.add("UNKNOWN");
    return out;
  }

  public String getName(){
    return predictor.getName()+" Oracle";
  }


  private void initRanking(){
    if (labelRanking != null) return;

    logger.log(Level.INFO, "Start "+getName()+" ranking...");

    long time = System.currentTimeMillis();

    labelRanking = new ArrayList<>();
    stats.setOrder(labelRanking);

    labelRanking = predictor.ranking();

    if(labelRanking == null){
      logger.log(Level.WARNING, "Oracle stopped as prediction is empty");
      labelRanking = new ArrayList<>();
      stats.stopTime();
      return;
    }else if(labelRanking.isEmpty()){
      labelRanking = Arrays.asList(DEFAULT_RANKING);
    }

    stats.setOrder(labelRanking);

    logger.log(Level.INFO, "Finished ranking after " + (System.currentTimeMillis() - time) + "ms");
    logger.log(Level.INFO, "Predicted ranking ("+predictor.getName()+"): " + labelRanking.toString());
    stats.stopTime();
  }

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

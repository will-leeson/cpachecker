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

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristic;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.impl.HeuristicsFactory;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;

@Options(prefix = "managedOracle")
public class ManagedOracle implements IConfigOracle {

  @Option(secure = true,
      description = "file path to label-path mapping")
  private String labelPath = null;

  @Option(secure = true,
          description = "Base predictor for oracle. Predictor should be fast.")
  private String base = "linear";

  @Option(secure = true,
      description = "Staged predictor. Sort list after precision level and prediction speed.")
  private List<String> staged = new ArrayList<>();

  @Option(secure = true,
      description = "Backup predictor. Sort list after prediction speed.")
  private List<String> backup = new ArrayList<>();

  @Option(secure = true,
          description = "Heuristics to apply on output ranking.")
  private List<String> heuristics = new ArrayList<>();


  private LogManager logger;
  private RankManager manager;

  private Map<String, AnnotatedValue<Path>> labelToPath;
  private List<AnnotatedValue<Path>> unknown = new ArrayList<>();

  private PeekingIterator<AnnotatedValue<Path>> interceptIterator;

  private AnnotatedValue<Path> peekedBuffer = null;

  public ManagedOracle(Configuration config, PackedPredictorFactory pFactory, String program) throws InvalidConfigurationException {

    config.inject(this);
    logger = pFactory.getLogger();
    initLabelToPath(pFactory.getConfigPaths());
    manager = buildManager(pFactory, program);

  }


  private RankManager buildManager(PackedPredictorFactory pFactory, String program)
      throws InvalidConfigurationException {
    IRankingProvider baseProvider = new PredictorProviderWrapper(pFactory.getLogger(),
        pFactory.create(base), program
    );

    RankManager m = new RankManager(baseProvider);

    int i = 1;
    for(String stage: staged){
      IRankingProvider stagedProvider = new PredictorProviderWrapper(
          pFactory.getLogger(), pFactory.create(stage), program
      );
      m.registerProvider(i++, stagedProvider);
    }

    for(String stage: backup){
      IRankingProvider stagedProvider = new PredictorProviderWrapper(
          pFactory.getLogger(), pFactory.create(stage), program
      );
      m.registerProvider(0, stagedProvider);
    }

    HeuristicsFactory factory = new HeuristicsFactory();
    for(String heuristic: heuristics){
      RankHeuristic h = factory.create(heuristic);
      if(h != null){
        m.registerHeuristic(h);
      }
    }

    return m;
  }


  private void initLabelToPath(List<AnnotatedValue<Path>> list) {
    if (labelPath == null) {
      labelToPath = OracleFactory.initLabelToPath(list);

      for (AnnotatedValue<Path> l : list) {
        boolean knownPath = false;
        for (AnnotatedValue<Path> labelled : labelToPath.values()) {
          knownPath |= l.equals(labelled);
        }
        if (!knownPath) unknown.add(l);
      }
    }

  }

  private AnnotatedValue<Path> translate(String label){
    if(label.equalsIgnoreCase("unknown")){
      Path p = Paths.get("SKIP");
      AnnotatedValue<Path> annotatedValue = AnnotatedValue.create(p, "shutdownAfter");
      return annotatedValue;
    }
    if(label.equals("intercept-unknown")){
        label = "skip";
        interceptIterator = Iterators.peekingIterator(unknown.iterator());
        manager.next();
    }
    if(label.equalsIgnoreCase("skip")){
      Path p = Paths.get("SKIP");
      AnnotatedValue<Path> annotatedValue = AnnotatedValue.create(p);
      return annotatedValue;
    }

    if(labelToPath.containsKey(label)){
      logger.log(Level.INFO, label,".");
      return labelToPath.get(label);
    }

    Path path = PathFinder.find("%s", label);
    if(path != null && Files.exists(path)){
      Path p = Paths.get(label);
      AnnotatedValue<Path> annotatedValue = AnnotatedValue.create(p);
      return annotatedValue;
    }

    return null;
  }

  private boolean shiftUntilFound(){

    while (manager.hasNext()){
      String label = manager.peek();

      if(translate(label) != null){
        return true;
      }else{
        logger.log(Level.INFO, "Unknown label " + label + ". Skip.");
        manager.next();
      }

    }

    return false;
  }

  @Override
  public void precomputeOracle(Consumer<IConfigOracle> callback) {
    manager.peek();
    callback.accept(this);
  }

  @Override
  public AnnotatedValue<Path> peek() {

    if(peekedBuffer == null){
      peekedBuffer = this.next();
    }

    return peekedBuffer;
  }

  @Override
  public boolean hasNext() {

    if(peekedBuffer != null) return true;

    if(interceptIterator != null){
      if(interceptIterator.hasNext()){
        return true;
      } else {
        interceptIterator = null;
      }
    }

    return shiftUntilFound();
  }

  @Override
  public AnnotatedValue<Path> next() {

    if(peekedBuffer != null){
      AnnotatedValue<Path> tmp = peekedBuffer;
      peekedBuffer = null;
      return tmp;
    }

    if(hasNext()){
      if(interceptIterator != null){
        return interceptIterator.next();
      }else{
        return translate(manager.next());
      }
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {

  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    manager.collectStatistics(statsCollection);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.DoubleStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.CFAPathIterator;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.HeuristicPerformanceModel;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.IPerformanceModel;
import org.sosy_lab.cpachecker.core.algorithm.learning.CounterexampleStatistics;
import org.sosy_lab.cpachecker.core.algorithm.learning.CounterexampleStatistics.CounterexampleStatistic;
import org.sosy_lab.cpachecker.core.algorithm.learning.PathAnalyser;
import org.sosy_lab.cpachecker.core.algorithm.learning.PathEdge;
import org.sosy_lab.cpachecker.core.algorithm.learning.PathGraph;
import org.sosy_lab.cpachecker.core.algorithm.learning.analysis.RecursiveFunctionListener;
import org.sosy_lab.cpachecker.core.algorithm.learning.SVPathProcessor;
import org.sosy_lab.cpachecker.core.algorithm.learning.analysis.TypingListener;
import org.sosy_lab.cpachecker.core.algorithm.learning.analysis.UnusedVarListener;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

@Options(prefix="compositionAlgorithm.control")
public class ControlledCompositionStrategy extends AlgorithmCompositionStrategy
    implements Statistics {

  @Option(
      secure = true,
      description = "Set the maximum error path limit to be considered during learning.")
  private int maxPathLength = 512;

  @Option(
      secure = true,
      description = "Set the maximum number of samples drawn from the open tasks.")
  private int sampleSize = 10;

  @Option(
      secure = true,
      name="globalKeys",
      description = "Global keys to load statistics from.")
  private List<String> globalStatisticsKeys = new ArrayList<>();


  private int inCycleCount;
  private int noOfRounds;

  protected Iterator<AlgorithmContext> algorithmContextCycle;

  public ControlledCompositionStrategy(final Configuration pConfig, final LogManager pLogger, final CFA pCFA)
      throws InvalidConfigurationException {
    super(pLogger);
    pConfig.inject(this);
  }

  @Override
  public AlgorithmContext createContext(int pAlgorithmID, AnnotatedValue<Path> pConfigFile){
    String key = null;

    if(pAlgorithmID < globalStatisticsKeys.size()){
      key = globalStatisticsKeys.get(pAlgorithmID);
    }

    return new CEXAlgorithmContext(pConfigFile, key);
  }

  @Override
  protected void initializeAlgorithmContexts(List<AnnotatedValue<Path>> pConfigFiles) {
    super.initializeAlgorithmContexts(pConfigFiles);
    algorithmContextCycle = Iterables.cycle(algorithmContexts).iterator();
    inCycleCount = 0;
    noOfRounds = 0;
  }

  private boolean validateAnalysisState(){
    boolean update = false;
    for(AlgorithmContext context: algorithmContexts){
      update |= context.tryOrGetTestTargets().isPresent();
    }
    return update;
  }


  private List<CFAEdge> computePath(CFAEdge targetEdge){
    List<CFAEdge> edges = new ArrayList<>();

    CFAPathIterator iterator = new CFAPathIterator(targetEdge);

    int currentLength = 0;
    while(currentLength < maxPathLength && iterator.hasNext()){
      edges.add(iterator.next());
      currentLength++;
    }

    Collections.reverse(edges);

    return edges;
  }


  private List<CFAEdge> sampleList(List<CFAEdge> list, int n){

    if(list.isEmpty())return list;

    Random r = ThreadLocalRandom.current();
    List<CFAEdge> sample = new ArrayList<>(n);

    for(int i = 0; i < n; i++){
      sample.add(list.get(r.nextInt(list.size())));
    }

    return sample;
  }

  // Naive Bayesian approximation of log( P(V = 1) * P( p | V = 1) )
  private double computeLogit(CounterexampleStatistics pCounterexampleStatistics, List<CFAEdge> path, int vocabularySize){
    CounterexampleStatistic statistic = pCounterexampleStatistics.generateCounterexampleStatistic(path);

    double logit = 0.0;

    for(Entry<String, Integer> e : statistic.getCounterexampleTokens().entrySet()){

      //Laplace smoothing
      logit += Math.log((e.getValue() + 1) / (double)(statistic.getNumSeenTokens() + vocabularySize));

    }

    return logit;

  }

  private double logSumExp(double[] logits){
    double xs = DoubleStream.of(logits).max().orElse(0.0);
    return Math.log(DoubleStream.of(logits).map(x -> Math.exp(x - xs)).sum()) + xs;
  }

  private double[] computePrior(){

    double priorNorm = 0.0;
    double[] algorithmPrior = new double[algorithmContexts.size()];

    for(int id = 0; id < algorithmContexts.size(); id++){

      AlgorithmContext context = algorithmContexts.get(id);
      algorithmPrior[id] = context.getProgress() / context.getTimeLimit() ;
      priorNorm += algorithmPrior[id];
    }

    if(priorNorm == 0){
      for(int id = 0; id < algorithmPrior.length; id++){
        algorithmPrior[id] = 1.0 / algorithmPrior.length;
      }
      priorNorm = 1.0;
    }

    for(int id = 0; id < algorithmPrior.length; id++){
      algorithmPrior[id] = algorithmPrior[id] / priorNorm;
    }

    return algorithmPrior;

  }

  private List<Integer> computePriorMask(double[] prior){

    List<Integer> mask = new ArrayList<>();

    for(int id = 0; id < prior.length; id++){
      if(prior[id] > 0.0) mask.add(id);
    }

    return mask;

  }

  // We compute distriution without prior: P(V = 1 | P).
  private double[] computeTimeDistribution(double[] prior, Set<CFAEdge> openTasks, List<CounterexampleStatistics> pCounterexampleStatistics){
    double[] voting = new double[algorithmContexts.size()];
    List<Integer> priorMask = computePriorMask(prior);

    if(priorMask.size() == 1){
      int index = priorMask.iterator().next();
      voting[index] = 1.0;
      return voting;
    }

    Set<String> vocabulary = new HashSet<>();
    for(CounterexampleStatistics statistics: pCounterexampleStatistics)
      vocabulary.addAll(statistics.getVocabulary());


    for(CFAEdge edge : sampleList(new ArrayList<>(openTasks), sampleSize)){

        List<CFAEdge> randomPath = computePath(edge);
        double[] logits = new double[priorMask.size()];

        for(int id = 0; id < priorMask.size(); id++){

          int maskId = priorMask.get(id);
          CounterexampleStatistics current = pCounterexampleStatistics.get(maskId);
          logits[id] = Math.log(prior[maskId]) + computeLogit(current, randomPath, vocabulary.size());

        }

        double lnP = logSumExp(logits);

        for(int id = 0; id < priorMask.size(); id++){
          int maskId = priorMask.get(id);
          voting[maskId] += Math.exp(logits[id] - lnP);
        }

    }

    for(int i = 0; i < voting.length; i++){
      voting[i] /= sampleSize;
    }

    return voting;
  }

  private void updateModelAndSetNewTimeLimits(){

    boolean mayAdapt = true;
    long totalDistributableTimeBudget = 0;

    // We do not configure the first round
    if(noOfRounds == 0) return;

    // We are not building a model or update time limits if test targets are not available
    if(!validateAnalysisState()){
      logger.log(Level.SEVERE, "Cannot update time limits because test targets are missing.");
      return;
    }

    for(AlgorithmContext context: algorithmContexts){
      totalDistributableTimeBudget += context.getTimeLimit() - AlgorithmContext.DEFAULT_TIME_LIMIT;
      mayAdapt &= context.getProgress() >= 0;
    }

    if (totalDistributableTimeBudget <= algorithmContexts.size()) {
      mayAdapt = false;
    }

    if(!mayAdapt) return;

    //Now we are ready to adapt the timelimit
    Set<CFAEdge> openTasks = null;
    List<CounterexampleStatistics> statistics = new ArrayList<>();

    for(int id = 0; id < algorithmContexts.size(); id++){

      AlgorithmContext context = algorithmContexts.get(id);

      if(context instanceof CEXAlgorithmContext)
        statistics.add(((CEXAlgorithmContext) context).getCEXStatistics());

      Optional<Set<CFAEdge>> optionalTestTargets = algorithmContexts.get(id).tryOrGetTestTargets();
      if(optionalTestTargets.isPresent())
          openTasks = optionalTestTargets.get();
    }

    double[] algorithmPrior = computePrior();
    double[] timeDistribution = computeTimeDistribution(algorithmPrior, openTasks, statistics);

    for(int id = 0; id < algorithmContexts.size(); id++){
      AlgorithmContext context = algorithmContexts.get(id);
      double percentage = timeDistribution[id];

      context.adaptTimeLimit(
          AlgorithmContext.DEFAULT_TIME_LIMIT
          + (int) Math.round(percentage * totalDistributableTimeBudget)
      );

    }

  }

  @Override
  public boolean hasNextAlgorithm() {
    return algorithmContextCycle.hasNext();
  }

  @Override
  public AlgorithmContext getNextAlgorithm() {
    if (inCycleCount == algorithmContexts.size()) {

      inCycleCount = 0;
      noOfRounds++;
      logger.log(Level.INFO, "Controlled composition strategy starts next iteration...");

      updateModelAndSetNewTimeLimits();

      for (AlgorithmContext tempContext : algorithmContexts) {
        tempContext.resetProgress();
      }
    }
    inCycleCount++;

    return algorithmContextCycle.next();
  }

  @Override
  public void printStatistics(
      PrintStream pOut, Result result, UnmodifiableReachedSet reached) {

    pOut.println("Number of analyes per round: " + algorithmContexts.size());
    pOut.println("Number of completed rounds:  " + noOfRounds);
    pOut.println("Stopped in analysis:         " + inCycleCount);

    for (int i = 1; i < algorithmContexts.size(); i++) {
      pOut.println(
          "Time spent in analysis " + i + ":    " + algorithmContexts.get(i).getTotalTimeSpent());
    }

  }

  @Override
  public @Nullable String getName() {
    return "Controlled Composition";
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import com.google.common.base.Preconditions;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.AlgorithmControl;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ICompositionController;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

@Options(prefix = "compositionAlgorithm.dynamic")
public abstract class ADynamicCompositionStrategy extends AlgorithmCompositionStrategy
    implements Statistics {

  public enum TIMESCALE {
    UNIFORM_20,
    EXP_20
  }

  @Option(
      secure = true,
      description = "Timescales for algorithm configuration (Options: UNIFORM_20, EXP_20)."
  )
  private TIMESCALE timescale = TIMESCALE.UNIFORM_20;

  @Option(
      secure = true,
      description = "The maximum time given per algorithm. (Default: 100)"
  )
  private int maxTimePerAlgorithm = 100;

  private ICompositionController controller;
  private Map<String, AlgorithmContext> identifierToContext;
  private AlgorithmContext currentContext;

  private int noOfAlgorihms;

  protected ADynamicCompositionStrategy(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    super(pLogger);
    pConfig.recursiveInject(this);
    identifierToContext = new HashMap<>();
  }

  @Override
  protected void initializeAlgorithmContexts(List<AnnotatedValue<Path>> pConfigFiles) {
    super.initializeAlgorithmContexts(pConfigFiles);
    Collection<String> controlActions = createAlgorithmIdentifier();
    controller = this.createController(controlActions, createTimeDistribution());
    noOfAlgorihms = 1;
  }

  private Collection<String> createAlgorithmIdentifier(){
    for(AlgorithmContext context : super.algorithmContexts){
        identifierToContext.put(
            context.configToString(), context
        );
    }
    return identifierToContext.keySet();
  }

  private List<Integer> uniformDistribution(int divider){
    return IntStream.rangeClosed(1, maxTimePerAlgorithm/divider)
        .map(index -> divider * index)
        .boxed()
        .collect(Collectors.toList());
  }

  private List<Integer> expDistribution(int divider){

    int maxLog = (int) (Math.log(maxTimePerAlgorithm / (float)divider) / Math.log(2)) + 1;

    return IntStream.rangeClosed(0, maxLog)
        .map(index -> Math.min(maxTimePerAlgorithm, divider * (1 << index)))
        .boxed()
        .collect(Collectors.toList());
  }

  private Collection<Integer> createTimeDistribution(){
    switch (timescale){
      case UNIFORM_20:
        return uniformDistribution(20);
      case EXP_20:
        return expDistribution(20);
      default:
        throw new AssertionError("Unhandled timescale: " + timescale);
    }
  }

  protected abstract ICompositionController createController(Collection<String> controlActions,
                                                             Collection<Integer> timeDistribution);

  private void reportFeedbackToController(){
    if(currentContext == null)return;

    controller.reportAlgorithmProgress(
        currentContext.getProgress(), currentContext.getTimeLimit()
    );

   /* if(currentContext.getCoveredEdges().isPresent()){

      controller.reportCoverage(
          currentContext.getCoveredEdges().get()
      );

    }*/

  }

  @Override
  public boolean hasNextAlgorithm() {
    return controller.hasNextControlAction();
  }

  @Override
  public AlgorithmContext getNextAlgorithm() {

    reportFeedbackToController();

    AlgorithmControl control = controller.nextControlAction();

    Preconditions.checkState(identifierToContext.containsKey(control.getAlgorithmIdentifier()), "Illegal identifier given by controller: "+control.getAlgorithmIdentifier());

    AlgorithmContext nextContext = identifierToContext.get(control.getAlgorithmIdentifier());
    int timeLimit = Math.min(maxTimePerAlgorithm, control.getTimeLimit());
    nextContext.adaptTimeLimit(timeLimit);

    currentContext = nextContext;

    return nextContext;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    // TODO Auto-generated method stub
    pOut.println("Number of analyes: " + algorithmContexts.size());
    pOut.println("Number of executed algorithms:  " + noOfAlgorihms);

    for (int i = 1; i < algorithmContexts.size(); i++) {
      pOut.println(
          "Time spent in analysis " + i + ":    " + algorithmContexts.get(i).getTotalTimeSpent());
    }

  }

  @Override
  public @Nullable String getName() {
    return "Dynamic Composition";
  }

}

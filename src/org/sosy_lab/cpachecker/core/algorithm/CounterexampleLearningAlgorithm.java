// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.learning.CEXRegistry;
import org.sosy_lab.cpachecker.core.algorithm.learning.CounterexampleStatistics;
import org.sosy_lab.cpachecker.core.algorithm.learning.CounterexampleStatistics.CounterexampleStatistic;
import org.sosy_lab.cpachecker.core.algorithm.learning.PathAnalyser;
import org.sosy_lab.cpachecker.core.algorithm.learning.analysis.RecursiveFunctionListener;
import org.sosy_lab.cpachecker.core.algorithm.learning.analysis.TypingListener;
import org.sosy_lab.cpachecker.core.algorithm.learning.analysis.UnusedVarListener;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix="cex.learning")
public class CounterexampleLearningAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      name = "learningID",
      description = "the learning ID is used to recognize an analysis"
  )
  private String learningID = "NULL";

  @Option(
      secure = true,
      name = "global",
      description = "Whether we register the counterexample statistic globally."
  )
  private boolean registerGlobally = false;


  @Option(
      secure = true,
      name = "cexAnalysis",
      description = "Whether to infer counterexample features via static analysis"
  )
  private boolean cexAnalysis = false;


  @Option(
      secure = true,
      name = "ngramSize",
      description = "Size for a ngram statistic. If set to 1, only edges are counted"
  )
  private int ngramSize = 1;

  @Option(
      secure = true,
      name = "setBased",
      description = "Whether to count the exact number of edges or only the occurrence in counterexample."
  )
  private boolean isSetBased = true;

  @Option(
      secure = true,
      name = "initialPath",
      description = "path to load initial statistics from"
  )
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path initialStatisticPath = null;

  @Option(
      secure = true,
      name = "storeStatePath",
      description = "path to store learning results"
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path storeStatePath = null;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;


  private CounterexampleStatistics counterexampleStatistics;

  public CounterexampleLearningAlgorithm(
      final Algorithm pAlgorithm,
      final CFA pCfa,
      final Configuration pConfig,
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpec)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, TestCaseGeneratorAlgorithm.class);
    algorithm = pAlgorithm;
    cpa = pCpa;
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfig, logger, pCfa.getMachineModel());

    boolean shouldBuild = true;
    if(registerGlobally){
      Optional<CounterexampleStatistics> registeredCEXStats = CEXRegistry.loadIfPresent(learningID);
      if(registeredCEXStats.isPresent()){
        counterexampleStatistics = registeredCEXStats.get();
        shouldBuild = false;
      }
    }

    if(shouldBuild){

      buildCounterexampleStatistics();
      if(registerGlobally){
        CEXRegistry.register(learningID, counterexampleStatistics);
      }
    }

  }

  private void buildCounterexampleStatistics(){
    counterexampleStatistics = new CounterexampleStatistics(learningID, ngramSize, isSetBased);

    if(cexAnalysis){
      PathAnalyser analyser = new PathAnalyser();
      analyser.attachListener(new RecursiveFunctionListener());
      analyser.attachListener(new UnusedVarListener());
      if(cfa.getVarClassification().isPresent())
        analyser.attachListener(new TypingListener(cfa.getVarClassification().get()));
      counterexampleStatistics.setAnalyser(analyser);
    }

    if(initialStatisticPath != null){
      try{
        counterexampleStatistics.loadFromFile(initialStatisticPath);
        logger.log(Level.INFO, String.format("Loaded counterexample statistics from %s.", initialStatisticPath.toString()));
      } catch(IOException e){
        logger.log(Level.WARNING, String.format("Cannot load path %s. Does it exists?", initialStatisticPath.toString()), e);
      }
    }
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReached)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_IMPRECISE;

    try{
      status = algorithm.run(pReached);
    } catch (CPAException e) {
      status = status.withPrecise(false);
      throw e;

    } catch (InterruptedException e1) {
      // may be thrown only be counterexample check, if not will be thrown again in finally
      // block due to respective shutdown notifier call)
      status = status.withPrecise(false);
    } catch (Exception e2) {
      // precaution always set precision to false, thus last target state not handled in case of
      // exception
      status = status.withPrecise(false);
      throw e2;
    } finally {

      boolean shouldSave = false;

      assert ARGUtils.checkARG(pReached);
      assert (from(pReached).filter(AbstractStates::isTargetState).size() < 2);

      AbstractState reachedState =
          from(pReached).firstMatch(AbstractStates::isTargetState).orNull();
      if (reachedState != null) {

        ARGState argState = (ARGState) reachedState;

        Collection<ARGState> parentArgStates = argState.getParents();

        assert (parentArgStates.size() == 1);

        ARGState parentArgState = parentArgStates.iterator().next();

        CFAEdge targetEdge = parentArgState.getEdgeToChild(argState);
        if (targetEdge != null) {

          if(status.isPrecise()){

            CounterexampleInfo
                cexInfo = ARGUtils.tryGetOrCreateCounterexampleInformation(argState, cpa, assumptionToEdgeAllocator).orElseThrow();
            learnFromCounterexample(cexInfo);

            shouldSave = true;

          } else {
            logger.log(Level.FINE, "Target edge found but counterexample was imprecise.");
          }

        } else {
          logger.log(Level.FINE, "Target edge was null.");
        }
      } else {
        logger.log(Level.FINE, "There was no target state in the reached set.");
      }

      if(shouldSave && storeStatePath != null){
        try{
          counterexampleStatistics.saveToFile(storeStatePath);
        } catch(IOException e){
          logger.log(Level.WARNING, String.format("Cannot store statistics to path %s.", storeStatePath.toString()), e);
        }
      }

      shutdownNotifier.shutdownIfNecessary();

    }

    return status;
  }


  private void learnFromCounterexample(CounterexampleInfo cexInfo){

    ARGPath path = cexInfo.getTargetPath();

    counterexampleStatistics.countFromCounterexample(
        path.getFullPath()
    );
  }

  public CounterexampleStatistics getCounterexampleStatistics() {
    return counterexampleStatistics;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
  }
}

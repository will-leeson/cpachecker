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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.algorithm.pcc.PartialARGsCombiner;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.ISampleBackend;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.InMemBackend;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.ProtoBackend;
import org.sosy_lab.cpachecker.intelligence.oracle.IConfigOracle;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix="intelligentRestartAlgorithm")
public class IntelligentRestartAlgorithm implements Algorithm, StatisticsProvider, ReachedSetUpdater {

  private static class RestartAlgorithmStatistics implements Statistics {

    private final LogManager logger;
    private final int noOfAlgorithms;
    private final Collection<Statistics> subStats;
    private int noOfAlgorithmsUsed = 0;
    private Timer totalTime = new Timer();

    public RestartAlgorithmStatistics(int pNoOfAlgorithms, LogManager pLogger) {
      noOfAlgorithms = pNoOfAlgorithms;
      subStats = new ArrayList<>();
      logger = checkNotNull(pLogger);
    }

    public Collection<Statistics> getSubStatistics() {
      return subStats;
    }

    public void resetSubStatistics() {
      subStats.clear();
      totalTime = new Timer();
    }

    @Override
    public String getName() {
      return "Predictive Restart Algorithm";
    }

    private void printIntermediateStatistics(PrintStream out, Result result,
        ReachedSet reached) {

      String text = "Statistics for algorithm " + noOfAlgorithmsUsed + " of " + noOfAlgorithms;
      out.println(text);
      out.println(Strings.repeat("=", text.length()));

      printSubStatistics(out, result, reached);
      out.println();
    }

    @Override
    public void printStatistics(PrintStream out, Result result,
        UnmodifiableReachedSet reached) {

      out.println("Number of algorithms provided:    " + noOfAlgorithms);
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);

      printSubStatistics(out, result, reached);
    }

    private void printSubStatistics(
        PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Total time for algorithm " + noOfAlgorithmsUsed + ": " + totalTime);

      for (Statistics s : subStats) {
        StatisticsUtils.printStatistics(s, out, logger, result, reached);
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      for (Statistics s : subStats) {
        try {
          StatisticsUtils.writeOutputFiles(s, logger, pResult, pReached);
        }catch(Exception e){
          logger.log(Level.WARNING, "Problem while writting "+s.getName());
        }
      }
    }
  }

  @Option(
    secure = true,
    required = true,
    description =
        "List of files with configurations to use. "
            + "A filename can be suffixed with :if-interrupted, :if-failed, and :if-terminated "
            + "which means that this configuration will only be used if the previous configuration "
            + "ended with a matching condition. What also can be added is :use-reached then the "
            + "reached set of the preceding analysis is taken and provided to the next analysis."
  )
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  @Option(
    secure = true,
    name = "combineARGsAfterRestart",
    description =
        "combine (partial) ARGs obtained by restarts of the analysis after an unknown result with a different configuration"
  )
  private boolean useARGCombiningAlgorithm = false;

  @Option(
    secure = true,
    description =
        "print the statistics of each component of the restart algorithm"
            + " directly after the components computation is finished"
  )
  private boolean printIntermediateStatistics = true;

  @Option(
    secure = true,
    description =
        "let each component of the restart algorithm write output files"
            + " and not only the last one that is excuted"
  )
  private boolean writeIntermediateOutputFiles = false;

  /* The option is useful for some preanalysis,
   * for instance, the first analysis is fast and provides some hints to the next ones
   * Is used, for example, in CPALockator
   *
   * TODO It might be better to have two lists of algorithms given to the RestartAlgorithm.
   * One list for analysis with no result expected except information about the program
   * and a second list for the real analyses to be restarted if necessary.
   * This would allow to combine the pre-computation of information and
   * the normal sequential composition of algorithms in a more flexible way.
   */
  @Option(
    secure = true,
    description =
        "wether to start next algorithm independently from the previous result"
  )
  private boolean alwaysRestart = false;

  @Option(
      secure = true,
      description = "Oracle to use"
  )
  private String oracle = "default";


  @Option(
      secure = true,
      description = "path to file containing instances of the training set"
  )
  private String instancePath = null;


  @Option(
      secure = true,
      description = "DEBUG OPTION: Only predict a ranking without executing the tools"
  )
  private boolean onlyPrediction = false;

  @Option(
      secure = true,
      description = "Switch to an accelerated prediction model. Use with caution: The pretrained model might not support the accelerated model."
  )
  private boolean accelerated = false;


  @Option(
      secure = true,
      description = "Iteration bound for WL iterations"
  )
  private int iterationBound = 2;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ShutdownManager predictionShutdown;
  private final ShutdownRequestListener logShutdownListener;
  private final RestartAlgorithmStatistics stats;
  private final CFA cfa;
  private final Configuration globalConfig;
  private final Specification specification;


  private Algorithm currentAlgorithm;
  private IConfigOracle oracleImpl;

  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners =
      new CopyOnWriteArrayList<>();

  private final Collection<ReachedSetUpdateListener> reachedSetUpdateListenersAdded = Lists.newArrayList();

  private IntelligentRestartAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      String program)
      throws InvalidConfigurationException {
    config.inject(this);

    if (configFiles.isEmpty()) {
      throw new InvalidConfigurationException("Need at least one configuration for restart algorithm!");
    }

    this.stats = new RestartAlgorithmStatistics(configFiles.size(), pLogger);
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.predictionShutdown = ShutdownManager.createWithParent(pShutdownNotifier);
    this.cfa = pCfa;
    this.globalConfig = config;
    specification = checkNotNull(pSpecification);

    //TODO: Create registry based on config
    ISampleBackend backend;
    FeatureRegistry registry = new FeatureRegistry();

    try {
      backend = new ProtoBackend(registry, instancePath);
    } catch (IOException pE) {
      if(instancePath != null){
        try {
          backend = new ProtoBackend(registry);
        } catch (IOException pE1) {
          backend = new InMemBackend();
        }
      }else{
        backend = new InMemBackend();
      }
    }

    SampleRegistry sampleRegistry = new SampleRegistry(
        registry, iterationBound, 5, backend, accelerated
    );

    this.oracleImpl = OracleFactory.getInstance().create(
        this.oracle, logger, config,
        this.predictionShutdown.getNotifier(),
        this.configFiles,
        sampleRegistry,
        this.cfa,
        program
    );
    this.oracleImpl.collectStatistics(stats.subStats);

    logShutdownListener =
        reason ->
            logger.logf(
                Level.WARNING,
                "Shutdown of analysis %d requested (%s).",
                stats.noOfAlgorithmsUsed,
                reason);
  }

  public static Algorithm create(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      String program)
      throws InvalidConfigurationException {
    IntelligentRestartAlgorithm algorithm =
        new IntelligentRestartAlgorithm(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa, program);
    if (algorithm.useARGCombiningAlgorithm) {
      return new PartialARGsCombiner(algorithm, pConfig, pLogger, pShutdownNotifier);
    }
    return algorithm;
  }

  @SuppressFBWarnings(value="DM_DEFAULT_ENCODING",
      justification="Encoding is irrelevant for null output stream")
  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    try{
      return this.runImpl(pReachedSet);
    }finally {
      this.predictionShutdown.requestShutdown("Analysis finished");
    }
  }



  @SuppressFBWarnings(value="DM_DEFAULT_ENCODING",
      justification="Encoding is irrelevant for null output stream")
  public AlgorithmStatus runImpl(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(pReached instanceof ForwardingReachedSet, "RestartAlgorithm needs ForwardingReachedSet");
    checkArgument(pReached.size() <= 1, "RestartAlgorithm does not support being called several times with the same reached set");
    checkArgument(!pReached.isEmpty(), "RestartAlgorithm needs non-empty reached set");

    ForwardingReachedSet reached = (ForwardingReachedSet)pReached;

    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
    CFANode mainFunction = Iterables.getOnlyElement(initialNodes);

    PeekingIterator<AnnotatedValue<Path>> configFilesIterator =
        this.oracleImpl;

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;
    boolean provideReachedForNextAlgorithm = false;
    boolean isLastReachedSetUsable = false;
    final List<ConfigurableProgramAnalysis> cpasToClose = new ArrayList<>();

    while (configFilesIterator.hasNext()) {
      stats.totalTime.start();
      @Nullable ConfigurableProgramAnalysis currentCpa = null;
      ReachedSet currentReached;
      ShutdownManager singleShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);

      boolean lastAnalysisInterrupted = false;
      boolean lastAnalysisFailed = false;
      boolean lastAnalysisTerminated = false;
      boolean recursionFound = false;
      boolean concurrencyFound = false;
      boolean failWithUnknown = false;

      try {
        AnnotatedValue<Path> current = configFilesIterator.next();

        if(onlyPrediction) return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);

        failWithUnknown = current.annotation().isPresent()
                          && current.annotation().get().equals("shutdownAfter");

        Path singleConfigFileName = current.value();

        if(singleConfigFileName.endsWith("SKIP")){
          logger.log(Level.INFO, "Encounter special SKIP config. Skip to next config");
          continue;
        }

        logger.logf(
            Level.INFO,
            "Loading analysis %d from file %s ...",
            stats.noOfAlgorithmsUsed + 1,
            singleConfigFileName);

        try {
          Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> currentAlg =
              createNextAlgorithm(
                  singleConfigFileName,
                  mainFunction,
                  singleShutdownManager,
                  provideReachedForNextAlgorithm,
                  // we can only use the reached set if the last analysis terminated without exception
                  isLastReachedSetUsable ? reached.getDelegate() : null);
          currentAlgorithm = currentAlg.getFirst();
          currentCpa = currentAlg.getSecond();
          currentReached = currentAlg.getThird();
          provideReachedForNextAlgorithm = false; // has to be reseted
        } catch (InvalidConfigurationException e) {
          logger.logUserException(
              Level.WARNING,
              e,
              "Skipping one analysis because the configuration file "
                  + singleConfigFileName
                  + " is invalid");
          continue;
        } catch (IOException e) {
          String message =
              "Skipping one analysis because the configuration file "
                  + singleConfigFileName.toString()
                  + " could not be read";
          if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
            logger.log(Level.WARNING, message);
          } else {
            logger.logUserException(Level.WARNING, e, message);
          }
          continue;
        }

        if (reached instanceof HistoryForwardingReachedSet) {
          ((HistoryForwardingReachedSet) reached).saveCPA(currentCpa);
        }
        reached.setDelegate(currentReached);

        if (currentAlgorithm instanceof StatisticsProvider) {
          ((StatisticsProvider)currentAlgorithm).collectStatistics(stats.getSubStatistics());
        }
        shutdownNotifier.shutdownIfNecessary();

        stats.noOfAlgorithmsUsed++;

        // run algorithm
        registerReachedSetUpdateListeners();
        try {
          logger.logf(Level.INFO, "Starting analysis %d ...", stats.noOfAlgorithmsUsed);
          status = currentAlgorithm.run(currentReached);

          if (currentReached.wasTargetReached() && status.isPrecise()) {

            // If the algorithm is not _precise_, verdict "false" actually means "unknown".
            return status;
          }

          if (!status.isSound()) {
            // if the analysis is not sound and we can proceed with
            // another algorithm, continue with the next algorithm
            logger.logf(
                Level.INFO,
                "Analysis %d terminated, but result is unsound.",
                stats.noOfAlgorithmsUsed);

          } else if (currentReached.hasWaitingState()) {
            // if there are still states in the waitlist, the result is unknown
            // continue with the next algorithm
            logger.logf(
                Level.INFO,
                "Analysis %d terminated but did not finish: There are still states to be processed.",
                stats.noOfAlgorithmsUsed);

          } else if (!(from(currentReached).anyMatch(AbstractStates::isTargetState) && !status.isPrecise())) {

            if (!(alwaysRestart && configFilesIterator.hasNext())) {
              // sound analysis and completely finished, terminate
              return status;
            }

          }
          lastAnalysisTerminated = true;
          isLastReachedSetUsable = true;

        } catch (CPAException e) {
          isLastReachedSetUsable = false;
          lastAnalysisFailed = true;
          if (e instanceof CounterexampleAnalysisFailed || e instanceof RefinementFailedException) {
            status = status.withPrecise(false);
          }
          if (configFilesIterator.hasNext()) {
            logger.logUserException(
                Level.WARNING, e, "Analysis " + stats.noOfAlgorithmsUsed + " not completed.");
            if (e.getMessage().contains("recursion")) {
              recursionFound = true;
            }
            if (e.getMessage().contains("pthread_create")) {
              concurrencyFound = true;
            }
          } else {
            throw e;
          }
        } catch (InterruptedException e) {
          isLastReachedSetUsable = false;
          lastAnalysisInterrupted = true;
          if (configFilesIterator.hasNext()) {
            logger.logUserException(
                Level.WARNING, e, "Analysis " + stats.noOfAlgorithmsUsed + " stopped.");
            shutdownNotifier.shutdownIfNecessary(); // check if we should also stop
          } else {
            throw e;
          }
        }
      } finally {
        unregisterReachedSetUpdateListeners();
        singleShutdownManager.getNotifier().unregister(logShutdownListener);
        singleShutdownManager.requestShutdown("Analysis terminated"); // shutdown any remaining components
        stats.totalTime.stop();

      }

      if(failWithUnknown){
        this.predictionShutdown.requestShutdown("Analyis terminated");
        logger.log(Level.INFO, "Shutdown after shutdownAfter condition.");
        return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
      }

      shutdownNotifier.shutdownIfNecessary();


      if (configFilesIterator.hasNext()) {
        // Check if the next config file has a condition,
        // and if it has a condition, check if it matches.
        boolean foundConfig;
        do {
          foundConfig = true;
          Optional<String> condition = configFilesIterator.peek().annotation();
          if (condition.isPresent()) {
            switch (condition.get()) {
            case "if-interrupted":
              foundConfig = lastAnalysisInterrupted;
              break;
            case "if-failed":
              foundConfig = lastAnalysisFailed;
              break;
            case "if-terminated":
              foundConfig = lastAnalysisTerminated;
              break;
            case "if-recursive":
              foundConfig = recursionFound;
              break;
            case "if-concurrent":
              foundConfig = concurrencyFound;
              break;
              case "use-reached":
                provideReachedForNextAlgorithm = true;
                foundConfig = true;
                break;
              case "shutdownAfter":
                foundConfig = true;
                break;
            default:
                logger.logf(
                    Level.WARNING,
                    "Ignoring invalid restart condition '%s' for file %s.",
                    condition.get(),
                    configFilesIterator.peek().value());
              foundConfig = true;
            }
            if (!foundConfig) {
              logger.logf(
                  Level.INFO,
                  "Ignoring restart configuration '%s' because condition %s did not match.",
                  configFilesIterator.peek().value(),
                  condition.get());
              configFilesIterator.next();
              stats.noOfAlgorithmsUsed++;
            }
          }
        } while (!foundConfig && configFilesIterator.hasNext());
      }

      if (configFilesIterator.hasNext()) {
        if (printIntermediateStatistics) {
          stats.printIntermediateStatistics(System.out, Result.UNKNOWN, currentReached);
        } else {
          stats.printIntermediateStatistics(new PrintStream(ByteStreams.nullOutputStream()), Result.UNKNOWN, currentReached);
        }
        if (writeIntermediateOutputFiles) {
          stats.writeOutputFiles(Result.UNKNOWN, pReached);
        }
        stats.resetSubStatistics();

        if (currentCpa != null && !provideReachedForNextAlgorithm) {
          CPAs.closeCpaIfPossible(currentCpa, logger);
        } else {
          cpasToClose.add(currentCpa);
        }
        CPAs.closeIfPossible(currentAlgorithm, logger);

        logger.log(Level.INFO, "RestartAlgorithm switches to the next configuration...");
      }
    }

    for (ConfigurableProgramAnalysis cpa : cpasToClose) {
      CPAs.closeCpaIfPossible(cpa, logger);
    }

    // no further configuration available, and analysis has no
    this.predictionShutdown.requestShutdown("Analyis terminated");
    logger.log(Level.INFO, "No further configuration available.");
    return status;
  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createNextAlgorithm(
      Path singleConfigFileName,
      CFANode mainFunction,
      ShutdownManager singleShutdownManager,
      boolean pProvideReachedForNextAlgorithm,
      ReachedSet pCurrentReached)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {

    ReachedSet reached;
    ConfigurableProgramAnalysis cpa;
    Algorithm algorithm;

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    singleConfigBuilder.clearOption("intelligentRestartAlgorithm.configFiles");
    singleConfigBuilder.clearOption("intelligentRestartAlgorithm.oracle");
    singleConfigBuilder.clearOption("analysis.restartIntelligentAfterUnknown");
    singleConfigBuilder.loadFromFile(singleConfigFileName);

    Configuration singleConfig = singleConfigBuilder.build();
    LogManager singleLogger = logger.withComponentName("Analysis" + (stats.noOfAlgorithmsUsed + 1));

    ResourceLimitChecker singleLimits = ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);
    singleLimits.start();
    singleShutdownManager.getNotifier().register(logShutdownListener);

    AggregatedReachedSets aggregateReached;
    if (pProvideReachedForNextAlgorithm && pCurrentReached != null) {
      aggregateReached = AggregatedReachedSets.singleton(pCurrentReached);
    } else {
      aggregateReached = AggregatedReachedSets.empty();
    }

    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            singleConfig, singleLogger, singleShutdownManager.getNotifier(), aggregateReached);
    cpa = coreComponents.createCPA(cfa, specification);

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) cpa).collectStatistics(stats.getSubStatistics());
    }

    GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

    algorithm = coreComponents.createAlgorithm(cpa, cfa, specification);
    if (algorithm instanceof IntelligentRestartAlgorithm) {
      // To avoid accidental infinitely-recursive nesting.
      throw new InvalidConfigurationException(
          "Sequential analysis parts may not be sequential analyses theirselves.");
    }
    reached = createInitialReachedSetForRestart(cpa, mainFunction, coreComponents, singleLogger);

    return Triple.of(algorithm, cpa, reached);
  }

  private ReachedSet createInitialReachedSetForRestart(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      CoreComponentsFactory pFactory,
      LogManager singleLogger) throws InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState = cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pFactory.createReachedSet(cpa);
    reached.add(initialState, initialPrecision);
    return reached;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (currentAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)currentAlgorithm).collectStatistics(pStatsCollection);
    }
    this.oracleImpl.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  @Override
  public void register(ReachedSetUpdateListener pReachedSetUpdateListener) {
    reachedSetUpdateListeners.add(pReachedSetUpdateListener);
  }

  @Override
  public void unregister(ReachedSetUpdateListener pReachedSetUpdateListener) {
    reachedSetUpdateListeners.remove(pReachedSetUpdateListener);
  }

  private void registerReachedSetUpdateListeners() {
    Preconditions.checkState(reachedSetUpdateListenersAdded.isEmpty());
    if (currentAlgorithm instanceof ReachedSetUpdater) {
      ReachedSetUpdater algorithm = (ReachedSetUpdater) currentAlgorithm;
      for (ReachedSetUpdateListener listener : reachedSetUpdateListeners) {
        algorithm.register(listener);
        reachedSetUpdateListenersAdded.add(listener);
      }
    }
  }

  private void unregisterReachedSetUpdateListeners() {
    if (currentAlgorithm instanceof ReachedSetUpdater) {
      ReachedSetUpdater algorithm = (ReachedSetUpdater) currentAlgorithm;
      for (ReachedSetUpdateListener listener : reachedSetUpdateListenersAdded) {
        algorithm.unregister(listener);
      }
      reachedSetUpdateListenersAdded.clear();
    } else {
      Preconditions.checkState(reachedSetUpdateListenersAdded.isEmpty());
    }
    assert reachedSetUpdateListenersAdded.isEmpty();
  }
}

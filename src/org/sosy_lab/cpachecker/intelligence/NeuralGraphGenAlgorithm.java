/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.protobuf.Struct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.neural.GraphWriter;
import org.sosy_lab.cpachecker.intelligence.ast.neural.SVGraphProcessor;
import org.sosy_lab.cpachecker.intelligence.ast.neural.SVPEProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.BlockedGraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer.AliasAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.*;

@Options(prefix = "neuralGraphGen")
public class NeuralGraphGenAlgorithm implements Algorithm, StatisticsProvider {

  @Option(secure = true,
          description = "export node postitions")
  private String nodePosition = null;

  @Option(
      secure = true,
      name = "output",
      description = "output path for Graph"
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputPath = null;


  public static class GraphGenStatistics implements Statistics{

    public SVGraph graph;
    public Duration cfaTime;
    public Duration prepTime;
    public Duration disconnectTime;
    public Duration dataTime;
    public Duration controlTime;
    public Duration aliasTime;
    public Duration writeTime;

    @Override
    public void printStatistics(
        PrintStream out, Result result, UnmodifiableReachedSet reached) {

      if(cfaTime != null)
        out.println("CFA Processing Time: "+cfaTime.toString());

      if(aliasTime != null)
        out.println("Time to compute aliases: "+aliasTime.toString());

      if(prepTime != null)
        out.println("Preprocessing time: "+prepTime.toString());

      if(disconnectTime != null)
        out.println("Time for component disconnect: "+disconnectTime.toString());

      if(dataTime != null)
        out.println("Data dependency: "+dataTime.toString());

      if(controlTime != null)
        out.println("Control dependency: "+controlTime.toString());

      if(writeTime != null)
        out.println("Write time: "+writeTime.toString());

      // Graph statistics

      if(graph != null){

        out.println("Number of nodes: "+graph.nodes().size());
        out.println("Number of edges: "+ graph.edgeStream().mapToInt(x -> 1).sum());
        out.println("Number of data edges: "+graph.edgeStream()
                                                    .filter(x -> x.getId().equals("dd"))
                                                    .mapToInt(x -> 1).sum()
                    );
        out.println("Number of control edges: "+graph.edgeStream()
                                                    .filter(x -> x.getId().equals("cd"))
                                                    .mapToInt(x -> 1).sum()
                                                );

      }

    }

    @Override
    public @Nullable String getName() {
      return "Neural graph generation algorithm";
    }
  }



  private LogManager logger;
  private GraphGenStatistics stats = new GraphGenStatistics();
  private CFA cfa;
  private ShutdownNotifier notifier;

  public NeuralGraphGenAlgorithm(LogManager pLogger, Configuration pConfiguration, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    logger = pLogger;
    notifier = pShutdownNotifier;
    cfa = pCFA;
  }


  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    if(outputPath == null) {
      logger.log(Level.SEVERE, "Cannot write graph if no outputPath is specified");
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
    }

    Stopwatch stopwatch = Stopwatch.createStarted();

    logger.log(Level.INFO, "Start CFA processing....");
    SVGraph graph = new SVPEProcessor().process(cfa, notifier);

    stopwatch.stop();

    stats.cfaTime = stopwatch.elapsed();
    stopwatch.reset();


    Map<String, Set<String>> aliases = null;

    if(graph.getGlobalOption(OptionKeys.POINTER_GRAPH) != null){
      stopwatch.start();
      AliasAnalyser aliasAnalyser = new AliasAnalyser(graph.getGlobalOption(OptionKeys.POINTER_GRAPH), notifier);
      aliases = aliasAnalyser.getAliases();

      graph.setGlobalOption(OptionKeys.POINTER_GRAPH, null);
      aliasAnalyser = null;
      stopwatch.stop();
      stats.aliasTime = stopwatch.elapsed();
      stopwatch.reset();
    }

    stopwatch.start();
    GraphAnalyser graphAnalyser = new BlockedGraphAnalyser(graph, notifier, logger);
    graphAnalyser.simplify();
    graphAnalyser.recursionDetection();

    stopwatch.stop();
    stats.prepTime = stopwatch.elapsed();
    stopwatch.reset();

    stopwatch.start();

    logger.log(Level.INFO, "Add data dependencies");
    graphAnalyser.applyDD(aliases);

    stats.dataTime = stopwatch.elapsed();
    stopwatch.reset().start();

    graphAnalyser.disconnectFunctionsViaDependencies();
    stats.disconnectTime = stopwatch.elapsed();
    stopwatch.reset().start();

    logger.log(Level.INFO, "Add control dependencies");
    graphAnalyser.applyCD();

    stats.controlTime = stopwatch.elapsed();
    stopwatch = stopwatch.reset().start();

    graph = graphAnalyser.getGraph();

    stats.graph = graph;

    logger.log(Level.INFO, "Write graph to "+outputPath);
    try {
      exportGraph(graph);
      exportNodePostition(graph);
    } catch (IOException pE) {
      logger.log(Level.WARNING, "Problem while writing "+outputPath, pE);
    }
    stats.writeTime = stopwatch.elapsed();
    stopwatch.stop();

    return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
  }

  private void exportGraph(SVGraph pGraph) throws IOException {
    Path parent = outputPath.getParent();

    if((parent != null && !Files.exists(parent)) ){
      Files.createDirectory(parent);
    }

    GraphWriter writer = new GraphWriter(pGraph);
    writer.writeTo(outputPath);
  }

  private void exportNodePostition(SVGraph pGraph) throws IOException {
    if(nodePosition == null) return;

    Path out = Paths.get(nodePosition);
    Path parent = out.getParent();

    if((parent != null && !Files.exists(parent)) || !Files.isDirectory(parent)){
      Files.createDirectory(parent);
    }

    GraphWriter writer = new GraphWriter(pGraph);
    writer.writePositionTo(out);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

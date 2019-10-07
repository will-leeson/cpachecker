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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.intelligence.ast.neural.GraphWriter;
import org.sosy_lab.cpachecker.intelligence.ast.neural.SVGraphProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

@Options(prefix = "neuralGraphGen")
public class NeuralGraphGenAlgorithm implements Algorithm {

  @Option(secure = true,
          description = "export node postitions")
  private String nodePosition = null;

  @Option(
      secure = true,
      description = "output path for Graph"
  )
  private String output = "output/graph.dfs";

  private LogManager logger;

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


    Stopwatch stopwatch = Stopwatch.createStarted();

    logger.log(Level.INFO, "Start CFA processing....");
    SVGraph graph = new SVGraphProcessor().process(cfa, notifier);

    System.out.println("Time for CFA: "+stopwatch.elapsed());
    stopwatch.reset();

    GraphAnalyser graphAnalyser = new GraphAnalyser(graph, notifier, logger);
    graphAnalyser.simplify();
    graphAnalyser.recursionDetection();

    stopwatch.start();

    logger.log(Level.INFO, "Add data dependencies");
    graphAnalyser.applyDD();

    System.out.println("Time for DD: "+stopwatch.elapsed());
    stopwatch.reset().start();

    graphAnalyser.disconnectFunctionsViaDependencies();
    System.out.println("Time for Disconnect: "+stopwatch.elapsed());
    stopwatch.reset().start();

    logger.log(Level.INFO, "Add control dependencies");
    graphAnalyser.applyCD();

    System.out.println("Time for CD: "+stopwatch.elapsed());
    stopwatch = stopwatch.reset().start();

    logger.log(Level.INFO, "Write graph to "+output.toString());
    try {
      exportGraph(graph);
      exportNodePostition(graph);
    } catch (IOException pE) {
      logger.log(Level.WARNING, "Problem while writing "+output.toString(), pE);
    }
    System.out.println("Time for Writing: "+stopwatch.elapsed());

    return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
  }

  private void exportGraph(SVGraph pGraph) throws IOException {
    Path out = Paths.get(output);
    Path parent = out.getParent();

    if(parent != null && !Files.exists(parent) || !Files.isDirectory(parent)){
      Files.createDirectory(parent);
    }

    GraphWriter writer = new GraphWriter(pGraph);
    writer.writeTo(out);
  }

  private void exportNodePostition(SVGraph pGraph) throws IOException {
    if(nodePosition == null) return;

    Path out = Paths.get(nodePosition);
    Path parent = out.getParent();

    if(parent != null && !Files.exists(parent) || !Files.isDirectory(parent)){
      Files.createDirectory(parent);
    }

    GraphWriter writer = new GraphWriter(pGraph);
    writer.writePositionTo(out);
  }

}

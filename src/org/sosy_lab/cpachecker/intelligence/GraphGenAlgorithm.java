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

import java.io.BufferedWriter;
import java.io.File;
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
import org.sosy_lab.cpachecker.intelligence.ast.CFAProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

@Options(prefix = "graphGen")
public class GraphGenAlgorithm implements Algorithm {

  @Option(secure = true,
          description = "depth of AST tree")
  private int astDepth = 5;

  @Option(
      secure = true,
      description = "output path for Graph"
  )
  private String output = "output/graph.dfs";

  private LogManager logger;

  private CFA cfa;
  private ShutdownNotifier notifier;

  public GraphGenAlgorithm(LogManager pLogger, Configuration pConfiguration, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    logger = pLogger;
    notifier = pShutdownNotifier;
    cfa = pCFA;
  }


  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    logger.log(Level.INFO, "Start CFA processing....");
    StructureGraph graph = new CFAProcessor().process(cfa, astDepth);

    logger.log(Level.INFO, "Add Dummy edges");
    GraphAnalyser.applyDummyEdges(graph, notifier);

    logger.log(Level.INFO, "Add data dependencies");
    GraphAnalyser.applyDD(graph, notifier);

    logger.log(Level.INFO, "Add control dependencies");
    GraphAnalyser.applyCD(graph, notifier);

    logger.log(Level.INFO, "Write graph to "+output.toString());
    try {
      exportGraph(graph);
    } catch (IOException pE) {
      logger.log(Level.WARNING, "Problem while writing "+output.toString(), pE);
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
  }

  private void exportGraph(StructureGraph pGraph) throws IOException {
    Path out = Paths.get(output);
    Path parent = out.getParent();

    if(!Files.exists(parent) || !Files.isDirectory(parent)){
      Files.createDirectory(parent);
    }

    BufferedWriter writer = Files.newBufferedWriter(out);

    try {
      writer.write(pGraph.toDFSRepresentation());
    }finally {
      writer.close();
    }

  }

}

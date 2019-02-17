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

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
import org.sosy_lab.cpachecker.intelligence.graph.CDEdge;
import org.sosy_lab.cpachecker.intelligence.graph.GEdge;
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

    Stopwatch stopwatch = Stopwatch.createStarted();

    logger.log(Level.INFO, "Start CFA processing....");
    StructureGraph graph = new CFAProcessor().process(cfa, astDepth);

    System.out.println("Time for CFA: "+stopwatch.elapsed());
    stopwatch.reset();

    GraphAnalyser analyser = new GraphAnalyser(graph, notifier, logger);

    logger.log(Level.INFO, "Add Dummy edges");
    analyser.pruneBlank();
    analyser.connectComponents();
    analyser.applyDummyEdges();

    stopwatch.start();

    logger.log(Level.INFO, "Add data dependencies");
    analyser.applyDD();

    System.out.println("Time for DD: "+stopwatch.elapsed());
    stopwatch.reset().start();

    logger.log(Level.INFO, "Add control dependencies");
    analyser.applyCD();

    System.out.println("Time for CD: "+stopwatch.elapsed());
    stopwatch = stopwatch.reset();

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

    EdgeWriter edgeSer = new EdgeWriter();

    String serial = pGraph.edgeStream().map(
        edge -> edgeSer.write(edge)
    ).collect(Collectors.joining(", "));

    serial = "["+serial+"]";

    writeWithNIO(out, serial);
  }

  private void writeWithNIO(Path pPath, String text)
      throws IOException {

    RandomAccessFile file = null;
    FileChannel channel = null;

    try {
      file = new RandomAccessFile(pPath.toString(), "rw");
      channel = file.getChannel();

      byte[] bytes = text.getBytes("UTF-8");
      ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
      byteBuffer.put(bytes);
      byteBuffer.flip();
      channel.write(byteBuffer);
    }finally {
      if(file != null)file.close();
      if(channel != null)channel.close();
    }



  }

  private static class EdgeWriter{

    Map<String, Integer> index = new HashMap<>();
    int counter = 0;

    private int index(String s){
      if(!index.containsKey(s)){
        index.put(s, counter++);
      }
      return index.get(s);
    }

    public String write(GEdge pGEdge){

      int a = index(pGEdge.getSource().getId());
      String aLabel = pGEdge.getSource().getLabel();

      int b = index(pGEdge.getSink().getId());
      String bLabel = pGEdge.getSink().getLabel();

      boolean forward = a <= b;
      String edgeLabel = pGEdge.getId();

      if(pGEdge instanceof CDEdge){

        Map<String, Object> options = pGEdge.getSource().getOptions();
        if(options.containsKey("truth")){
          edgeLabel+= "_"+((boolean)options.get("truth")?"t":"f");
        }

      }



      if(forward){
        return String.format("[%d, %d, \"%s\", \"%s\", \"%s\"]", a, b, aLabel, edgeLabel+"|>", bLabel);
      }else{
        return String.format("[%d, %d, \"%s\", \"%s\", \"%s\"]", b, a, bLabel, "<|"+edgeLabel, aLabel);
      }

    }

  }

}
